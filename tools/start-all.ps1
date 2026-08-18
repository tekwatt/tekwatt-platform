param(
    [string]$DatabaseUsername = "tekwatt",
    [Parameter(Mandatory = $true)]
    [string]$DatabasePassword,
    [int]$StartupTimeoutSeconds = 300,
    [string]$JavaPath
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

if ([string]::IsNullOrWhiteSpace($JavaPath)) {
    if ($env:JAVA_HOME -and (Test-Path (Join-Path $env:JAVA_HOME "bin\java.exe"))) {
        $JavaPath = Join-Path $env:JAVA_HOME "bin\java.exe"
    } else {
        $JavaPath = Get-ChildItem "$env:ProgramFiles\Java\jdk-*\bin\java.exe" -ErrorAction SilentlyContinue |
            Sort-Object FullName -Descending | Select-Object -First 1 -ExpandProperty FullName
    }
}
if ([string]::IsNullOrWhiteSpace($JavaPath) -or -not (Test-Path $JavaPath)) {
    $JavaPath = (Get-Command java -ErrorAction SilentlyContinue).Source
}
if ([string]::IsNullOrWhiteSpace($JavaPath)) {
    throw "Java was not found. Install JDK 21 or pass -JavaPath."
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
        Sort-Object @{ Expression = { if ($_.Name -like "*-exec.jar") { 0 } else { 1 } } }, Name |
        Select-Object -First 1

    if ($null -eq $jar) {
        throw "Executable JAR missing for $service. Run Maven clean package -DskipTests first."
    }

    $stdout = Join-Path $logsDirectory "$service.log"
    $stderr = Join-Path $logsDirectory "$service-error.log"
    $process = Start-Process -FilePath $JavaPath `
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

$healthPorts = @{}
for ($index = 0; $index -lt $services.Count - 1; $index++) {
    $healthPorts[$services[$index]] = 8081 + $index
}
$healthPorts["api-gateway"] = 8080

$deadline = (Get-Date).AddSeconds($StartupTimeoutSeconds)
Write-Host "Waiting for all TekWatt services..." -ForegroundColor Cyan
do {
    $unhealthy = @()
    foreach ($service in $services) {
        try {
            $port = $healthPorts[$service]
            $health = Invoke-RestMethod -Uri "http://localhost:$port/actuator/health" -TimeoutSec 2
            if ($health.status -ne "UP") {
                $unhealthy += $service
            }
        } catch {
            $unhealthy += $service
        }
    }
    if ($unhealthy.Count -eq 0) {
        Write-Host "TekWatt Platform is UP. All $($services.Count) services are healthy." -ForegroundColor Green
        Write-Host "Swagger: http://localhost:8080/swagger-ui.html" -ForegroundColor Cyan
        exit 0
    }
    Start-Sleep -Seconds 3
} while ((Get-Date) -lt $deadline)

Write-Host "TekWatt Platform did not become fully healthy within $StartupTimeoutSeconds seconds." -ForegroundColor Red
Write-Host "Unhealthy services: $($unhealthy -join ', ')" -ForegroundColor Yellow
Write-Host "Review the matching service logs in: $logsDirectory" -ForegroundColor Yellow
exit 1
