[CmdletBinding()]
param(
    [string]$ResourceGroup = "tekwatt-prod-eastasia-rg",
    [string]$AcrName = "tekwattr25ddrfkacr",
    [string]$ContainerAppName = "api-gateway",
    [string]$ApiGatewayUrl = "https://api-gateway.lemonmushroom-1166ae48.eastasia.azurecontainerapps.io",
    [string]$Maven = "C:\software\maven\apache-maven-3.9.16\bin\mvn.cmd",
    [string]$ImageTag = ("ocpp-cors-{0}" -f (Get-Date -Format "yyyyMMddHHmmss"))
)

$ErrorActionPreference = "Stop"
$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$backendPom = Join-Path $repoRoot "backend\pom.xml"
$targetDirectory = Join-Path $repoRoot "backend\api-gateway\target"
$dockerfile = Join-Path $repoRoot "infrastructure\azure\backend.Dockerfile"
$buildContext = Join-Path $env:TEMP ("tekwatt-api-gateway-{0}" -f $ImageTag)

function Invoke-AzureCli {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments)
    & az @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Azure CLI command failed: az $($Arguments -join ' ')"
    }
}

if (-not (Get-Command az -ErrorAction SilentlyContinue)) {
    throw "Azure CLI is not installed or is not on PATH."
}
if (-not (Test-Path -LiteralPath $Maven)) {
    throw "Maven was not found at $Maven."
}

Write-Host "Checking the active Azure account..." -ForegroundColor Cyan
Invoke-AzureCli account show --output none

Write-Host "Building and testing the API Gateway..." -ForegroundColor Cyan
& $Maven -f $backendPom -pl api-gateway -am package
if ($LASTEXITCODE -ne 0) {
    throw "API Gateway build or tests failed."
}

$jars = Get-ChildItem -LiteralPath $targetDirectory -File -Filter "*.jar" |
    Where-Object { $_.Name -notmatch "(^original-|sources|javadoc)" }
$jar = $jars | Sort-Object Length -Descending | Select-Object -First 1
if (-not $jar) {
    throw "No runnable API Gateway JAR was found in $targetDirectory."
}

New-Item -ItemType Directory -Force -Path $buildContext | Out-Null
try {
    Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $buildContext "app.jar") -Force
    Copy-Item -LiteralPath $dockerfile -Destination (Join-Path $buildContext "Dockerfile") -Force

    $image = "$AcrName.azurecr.io/tekwatt/api-gateway:$ImageTag"
    Write-Host "Publishing $image..." -ForegroundColor Cyan
    Invoke-AzureCli acr build --registry $AcrName --image "tekwatt/api-gateway:$ImageTag" `
        --file (Join-Path $buildContext "Dockerfile") $buildContext --output none

    Write-Host "Updating the API Gateway Container App..." -ForegroundColor Cyan
    Invoke-AzureCli containerapp update --resource-group $ResourceGroup `
        --name $ContainerAppName --container-name $ContainerAppName `
        --image $image --set-env-vars "OCPP_GATEWAY_WS_URL=ws://ocpp-gateway" --output none

    Write-Host "Waiting for the public gateway..." -ForegroundColor Cyan
    $healthy = $false
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        try {
            $health = Invoke-RestMethod "$ApiGatewayUrl/actuator/health" -TimeoutSec 15
            if ($health.status -eq "UP") {
                $healthy = $true
                break
            }
        }
        catch {
            Start-Sleep -Seconds 10
        }
    }

    if (-not $healthy) {
        throw "The API Gateway did not report healthy after deployment."
    }

    Write-Host "API Gateway OCPP simulator fix deployed successfully." -ForegroundColor Green
    Write-Host "Return to evcharger-simulator.com and click Test connection."
}
finally {
    if (Test-Path -LiteralPath $buildContext) {
        Remove-Item -LiteralPath $buildContext -Recurse -Force
    }
}
