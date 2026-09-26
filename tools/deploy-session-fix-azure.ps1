param(
    [string]$ResourceGroup = "tekwatt-prod-eastasia-rg",
    [string]$AcrName = "tekwattr25ddrfkacr",
    [string]$ContainerAppName = "charging-session-service",
    [string]$StorageAccountName = "tekwattr25ddrfkweb",
    [string]$ApiGatewayUrl = "https://api-gateway.lemonmushroom-1166ae48.eastasia.azurecontainerapps.io",
    [string]$ImageTag = "codex-20260926-session-fix",
    [string]$Maven = "C:\software\maven\apache-maven-3.9.16\bin\mvn.cmd"
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$frontendDirectory = Join-Path $repoRoot "frontend\admin-portal"
$jar = Join-Path $repoRoot "backend\charging-session-service\target\charging-session-service-0.1.0-SNAPSHOT-exec.jar"
$dockerfile = Join-Path $repoRoot "infrastructure\azure\backend.Dockerfile"
$buildContext = Join-Path $env:TEMP "tekwatt-charging-session-$ImageTag"

function Invoke-AzureCli {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    & az @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Azure CLI command failed: az $($Arguments -join ' ')"
    }
}

if (-not (Get-Command az -ErrorAction SilentlyContinue)) { throw "Azure CLI is not installed or is not on PATH." }
if (-not (Test-Path -LiteralPath $Maven)) { throw "Maven was not found at $Maven." }

Write-Host "Checking the active Azure account..." -ForegroundColor Cyan
Invoke-AzureCli account show --output none

Write-Host "Building and testing the charging-session service..." -ForegroundColor Cyan
& $Maven -f (Join-Path $repoRoot "pom.xml") "-Dmaven.repo.local=$repoRoot\.maven-repository" -pl backend/charging-session-service -am clean package
if ($LASTEXITCODE -ne 0) { throw "Charging-session build or tests failed." }

New-Item -ItemType Directory -Force -Path $buildContext | Out-Null
Copy-Item -LiteralPath $jar -Destination (Join-Path $buildContext "app.jar") -Force
Copy-Item -LiteralPath $dockerfile -Destination (Join-Path $buildContext "Dockerfile") -Force

$image = "$AcrName.azurecr.io/tekwatt/charging-session-service:$ImageTag"
Write-Host "Publishing $image..." -ForegroundColor Cyan
Invoke-AzureCli acr build --registry $AcrName --image "tekwatt/charging-session-service:$ImageTag" --file (Join-Path $buildContext "Dockerfile") $buildContext --output none

Write-Host "Updating the Azure Container App..." -ForegroundColor Cyan
Invoke-AzureCli containerapp update --resource-group $ResourceGroup --name $ContainerAppName --image $image --output none

Write-Host "Building the admin portal..." -ForegroundColor Cyan
Push-Location $frontendDirectory
$previousApiBaseUrl = $env:VITE_API_BASE_URL
$previousTimeout = $env:VITE_API_TIMEOUT_SECONDS
try {
    $env:VITE_API_BASE_URL = $ApiGatewayUrl
    $env:VITE_API_TIMEOUT_SECONDS = "12"
    & npm.cmd run build
    if ($LASTEXITCODE -ne 0) { throw "Frontend build failed." }
    & npm.cmd run audit:navigation
    if ($LASTEXITCODE -ne 0) { throw "Frontend navigation audit failed." }
}
finally {
    $env:VITE_API_BASE_URL = $previousApiBaseUrl
    $env:VITE_API_TIMEOUT_SECONDS = $previousTimeout
    Pop-Location
}

Write-Host "Publishing the admin portal..." -ForegroundColor Cyan
$storageKey = & az storage account keys list --resource-group $ResourceGroup --account-name $StorageAccountName --query "[0].value" --output tsv
if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($storageKey)) { throw "Could not obtain the frontend storage key." }
Invoke-AzureCli storage blob upload-batch --account-name $StorageAccountName --account-key $storageKey --destination '$web' --source (Join-Path $frontendDirectory "dist") --overwrite true --output none

Write-Host "Verifying the public endpoints..." -ForegroundColor Cyan
$health = Invoke-RestMethod "$ApiGatewayUrl/actuator/health" -TimeoutSec 60
if ($health.status -ne "UP") { throw "API Gateway did not report UP." }
$null = Invoke-RestMethod "$ApiGatewayUrl/openapi/charging-session/v3/api-docs" -TimeoutSec 60

Write-Host "Azure deployment completed." -ForegroundColor Green
Write-Host "Frontend: https://$StorageAccountName.z7.web.core.windows.net/"
Write-Host "Swagger: $ApiGatewayUrl/swagger-ui.html"
