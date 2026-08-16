param(
    [string]$DatabaseUsername = "tekwatt",
    [Parameter(Mandatory = $true)]
    [string]$DatabasePassword,
    [int]$StartupTimeoutSeconds = 300
)

$ErrorActionPreference = "Stop"
$platformRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$logsDirectory = Join-Path $platformRoot "logs"
$services = @(
    "auth-service",
    "user-service",
    "charger-service",
    "charging-session-service",
    "tenant-service",
    "organization-service",
    "connector-service",
    "tariff-service",
    "reservation-service",
    "billing-service",
    "invoice-service",
    "payment-service",
    "notification-service",
    "ocpp-gateway",
    "telemetry-service",
    "firmware-service",
    "audit-service",
    "analytics-service",
    "reporting-service",
    "admin-service",
    "support-service",
    "api-gateway"
)

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    throw "Java was not found. Add JDK 21 bin to PATH before running this script."
}

New-Item -ItemType Directory -Force $logsDirectory | Out-Null
$env:DATABASE_USERNAME = $DatabaseUsername
$env:DATABASE_PASSWORD = $DatabasePassword
$env:SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE = "3"
$env:SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE = "1"
$started = @()

foreach ($service in $services) {
    $target = Join-Path $platformRoot "backend\$service\target"
    $jar = Get-ChildItem -Path $target -Filter "$service-*.jar" -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike "*.original" } |
        Select-Object -First 1

    if ($null -eq $jar) {
        throw "Executable JAR missing for $service. Run Maven clean package -DskipTests first."
    }

    $stdout = Join-Path $logsDirectory "$service.log"
    $stderr = Join-Path $logsDirectory "$service-error.log"
    $process = Start-Process -FilePath "java" `
        -ArgumentList @("-Xms64m", "-Xmx384m", "-jar", "`"$($jar.FullName)`"") `
        -RedirectStandardOutput $stdout `
        -RedirectStandardError $stderr `
        -WindowStyle Hidden `
        -PassThru

    $started += [PSCustomObject]@{ service = $service; processId = $process.Id }
    Write-Host "Started $service (PID $($process.Id))" -ForegroundColor Green
}

$pidFile = Join-Path $logsDirectory "service-pids.json"
$started | ConvertTo-Json | Set-Content -Path $pidFile -Encoding UTF8

$deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
Write-Host "Waiting for API Gateway health..." -ForegroundColor Cyan
do {
    try {
        $health = Invoke-RestMethod -Uri "http://localhost:8080/actuator/health" -TimeoutSec 3
        if ($health.status -eq "UP") {
            Write-Host "TekWatt Platform is UP." -ForegroundColor Green
            Write-Host "Swagger: http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
            exit 0
        }
    } catch {
        Start-Sleep -Seconds 3
    }
} while ((Get-Date) -lt $deadline)

Write-Host "API Gateway did not become healthy within $StartupTimeoutSeconds seconds." -ForegroundColor Red
Write-Host "Review: $logsDirectory\api-gateway-error.log" -ForegroundColor Yellow
exit 1
