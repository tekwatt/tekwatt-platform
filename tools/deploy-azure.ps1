[CmdletBinding()]
param(
    [string]$SubscriptionId,
    [string]$ResourceGroupName = "tekwatt-prod-rg",
    [string]$Location = "centralindia",
    [string]$Prefix = "tekwatt",
    [string]$MySqlAdminUsername = "tekwattadmin",
    [SecureString]$MySqlAdminPassword,
    [SecureString]$JwtSecret,
    [SecureString]$OcppSharedKey,
    [string]$ImageTag = (Get-Date -Format "yyyyMMddHHmmss"),
    [int]$BackendMinReplicas = 0,
    [int]$AlwaysOnMinReplicas = 1,
    [int]$MaxReplicas = 2,
    [switch]$SkipBackendBuild,
    [switch]$SkipFrontendBuild,
    [switch]$Yes
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$azureDirectory = Join-Path $repoRoot "infrastructure\azure"
$frontendDirectory = Join-Path $repoRoot "frontend\admin-portal"

function Get-PlainText([SecureString]$Value) {
    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Value)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer)
    }
}

function Find-CommandPath([string]$Name, [string[]]$Fallbacks) {
    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    foreach ($candidate in $Fallbacks) {
        if (Test-Path -LiteralPath $candidate) { return $candidate }
    }
    throw "$Name is not installed or is not available in PATH."
}

function Invoke-AzureCli([Parameter(ValueFromRemainingArguments = $true)][string[]]$Arguments) {
    & $script:az @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Azure CLI command failed: az $($Arguments -join ' ')"
    }
}

function New-DeploymentParameters([hashtable]$Values) {
    $parameters = @{}
    foreach ($key in $Values.Keys) {
        $parameters[$key] = @{ value = $Values[$key] }
    }
    $document = @{
        '$schema' = 'https://schema.management.azure.com/schemas/2019-04-01/deploymentParameters.json#'
        contentVersion = '1.0.0.0'
        parameters = $parameters
    }
    $path = Join-Path $script:temporaryDirectory ("parameters-{0}.json" -f [guid]::NewGuid())
    $document | ConvertTo-Json -Depth 20 | Set-Content -LiteralPath $path -Encoding UTF8
    return $path
}

$script:az = Find-CommandPath "az" @(
    "C:\Program Files\Microsoft SDKs\Azure\CLI2\wbin\az.cmd",
    "C:\Program Files (x86)\Microsoft SDKs\Azure\CLI2\wbin\az.cmd"
)
$maven = Find-CommandPath "mvn" @(
    "C:\software\maven\apache-maven-3.9.16\bin\mvn.cmd"
)
$npm = Find-CommandPath "npm" @(
    "C:\Program Files\nodejs\npm.cmd"
)
$npx = Find-CommandPath "npx" @(
    "C:\Program Files\nodejs\npx.cmd"
)

if (-not $MySqlAdminPassword) {
    $MySqlAdminPassword = Read-Host "Enter a NEW production MySQL administrator password" -AsSecureString
}
if (-not $JwtSecret) {
    $JwtSecret = Read-Host "Enter a NEW JWT secret (at least 32 characters)" -AsSecureString
}
if (-not $OcppSharedKey) {
    $OcppSharedKey = Read-Host "Enter a NEW OCPP shared key" -AsSecureString
}

$mysqlPasswordText = Get-PlainText $MySqlAdminPassword
$jwtSecretText = Get-PlainText $JwtSecret
$ocppSharedKeyText = Get-PlainText $OcppSharedKey

if ($mysqlPasswordText.Length -lt 12) { throw "The production MySQL password must contain at least 12 characters." }
if ($jwtSecretText.Length -lt 32) { throw "The JWT secret must contain at least 32 characters." }
if ($ocppSharedKeyText.Length -lt 16) { throw "The OCPP shared key must contain at least 16 characters." }

if (-not $Yes) {
    Write-Host "This will create billable Azure resources in ${Location}:" -ForegroundColor Yellow
    Write-Host "  Resource group: $ResourceGroupName"
    Write-Host "  22 Container Apps, MySQL Flexible Server, Container Registry, Log Analytics, and Static Web App"
    $confirmation = Read-Host "Type DEPLOY to continue"
    if ($confirmation -cne "DEPLOY") { throw "Deployment cancelled." }
}

$account = & $script:az account show --output json 2>$null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Azure sign-in is required. A browser window will open." -ForegroundColor Cyan
    Invoke-AzureCli login
}
if ($SubscriptionId) {
    Invoke-AzureCli account set --subscription $SubscriptionId
}

$activeAccount = & $script:az account show --output json | ConvertFrom-Json
Write-Host "Deploying to subscription: $($activeAccount.name) ($($activeAccount.id))" -ForegroundColor Cyan

$script:temporaryDirectory = Join-Path ([IO.Path]::GetTempPath()) ("tekwatt-azure-{0}" -f [guid]::NewGuid())
New-Item -ItemType Directory -Path $script:temporaryDirectory | Out-Null

try {
    Invoke-AzureCli provider register --namespace Microsoft.App
    Invoke-AzureCli provider register --namespace Microsoft.ContainerRegistry
    Invoke-AzureCli provider register --namespace Microsoft.DBforMySQL
    Invoke-AzureCli provider register --namespace Microsoft.OperationalInsights
    Invoke-AzureCli provider register --namespace Microsoft.Web
    Invoke-AzureCli group create --name $ResourceGroupName --location $Location --output none

    $foundationParameters = New-DeploymentParameters @{
        prefix = $Prefix
        location = $Location
        staticWebLocation = 'eastasia'
        mysqlAdminUsername = $MySqlAdminUsername
        mysqlAdminPassword = $mysqlPasswordText
    }

    Write-Host "Creating Azure network, registry, database, Container Apps environment, and frontend host..." -ForegroundColor Cyan
    $foundation = & $script:az deployment group create `
        --resource-group $ResourceGroupName `
        --name "tekwatt-foundation-$ImageTag" `
        --template-file (Join-Path $azureDirectory "foundation.bicep") `
        --parameters "@$foundationParameters" `
        --output json | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0) { throw "Azure foundation deployment failed." }

    $outputs = $foundation.properties.outputs
    $acrName = $outputs.acrName.value
    $acrLoginServer = $outputs.acrLoginServer.value
    $environmentName = $outputs.containerEnvironmentName.value
    $environmentDefaultDomain = $outputs.containerEnvironmentDefaultDomain.value
    $pullIdentityResourceId = $outputs.pullIdentityResourceId.value
    $mysqlHost = $outputs.mysqlHost.value
    $staticWebAppName = $outputs.staticWebAppName.value
    $staticWebAppUrl = $outputs.staticWebAppUrl.value

    if (-not $SkipBackendBuild) {
        Write-Host "Building the Java services..." -ForegroundColor Cyan
        & $maven "-Dmaven.repo.local=$repoRoot\.maven-repository" package -DskipTests
        if ($LASTEXITCODE -ne 0) { throw "Maven build failed." }
    }

    $services = @(
        'api-gateway', 'auth-service', 'user-service', 'charger-service',
        'charging-session-service', 'tenant-service', 'organization-service',
        'connector-service', 'tariff-service', 'reservation-service',
        'billing-service', 'invoice-service', 'payment-service',
        'notification-service', 'ocpp-gateway', 'telemetry-service',
        'firmware-service', 'audit-service', 'analytics-service',
        'reporting-service', 'admin-service', 'support-service'
    )

    foreach ($service in $services) {
        $targetDirectory = Join-Path $repoRoot "backend\$service\target"
        $jars = Get-ChildItem -LiteralPath $targetDirectory -File -Filter '*.jar' |
            Where-Object { $_.Name -notmatch '(^original-|sources|javadoc)' }
        $jar = $jars | Where-Object { $_.Name -like '*-exec.jar' } | Select-Object -First 1
        if (-not $jar) {
            $jar = $jars | Sort-Object Length -Descending | Select-Object -First 1
        }
        if (-not $jar) { throw "No runnable JAR was found for $service. Run the build without -SkipBackendBuild." }

        $contextDirectory = Join-Path $script:temporaryDirectory $service
        New-Item -ItemType Directory -Path $contextDirectory | Out-Null
        Copy-Item -LiteralPath $jar.FullName -Destination (Join-Path $contextDirectory 'app.jar')
        Copy-Item -LiteralPath (Join-Path $azureDirectory 'backend.Dockerfile') -Destination (Join-Path $contextDirectory 'Dockerfile')

        Write-Host "Publishing $service..." -ForegroundColor Cyan
        Invoke-AzureCli acr build `
            --registry $acrName `
            --image "tekwatt/$service`:$ImageTag" `
            --file (Join-Path $contextDirectory 'Dockerfile') `
            $contextDirectory `
            --output none
    }

    $servicesParameters = New-DeploymentParameters @{
        containerEnvironmentName = $environmentName
        containerEnvironmentDefaultDomain = $environmentDefaultDomain
        acrLoginServer = $acrLoginServer
        pullIdentityResourceId = $pullIdentityResourceId
        mysqlHost = $mysqlHost
        mysqlAdminUsername = $MySqlAdminUsername
        mysqlAdminPassword = $mysqlPasswordText
        jwtSecret = $jwtSecretText
        ocppSharedKey = $ocppSharedKeyText
        frontendUrl = $staticWebAppUrl
        imageTag = $ImageTag
        backendMinReplicas = $BackendMinReplicas
        alwaysOnMinReplicas = $AlwaysOnMinReplicas
        maxReplicas = $MaxReplicas
    }

    Write-Host "Starting the TekWatt backend services..." -ForegroundColor Cyan
    $backend = & $script:az deployment group create `
        --resource-group $ResourceGroupName `
        --name "tekwatt-services-$ImageTag" `
        --template-file (Join-Path $azureDirectory "services.bicep") `
        --parameters "@$servicesParameters" `
        --output json | ConvertFrom-Json
    if ($LASTEXITCODE -ne 0) { throw "Azure backend deployment failed." }

    $apiGatewayUrl = $backend.properties.outputs.apiGatewayUrl.value
    $ocppWebSocketUrl = $backend.properties.outputs.ocppWebSocketUrl.value

    if (-not $SkipFrontendBuild) {
        Write-Host "Building the frontend for $apiGatewayUrl..." -ForegroundColor Cyan
        Push-Location $frontendDirectory
        try {
            & $npm install
            if ($LASTEXITCODE -ne 0) { throw "Frontend dependency installation failed." }
            $previousApiBaseUrl = $env:VITE_API_BASE_URL
            $env:VITE_API_BASE_URL = $apiGatewayUrl
            & $npm run build
            if ($LASTEXITCODE -ne 0) { throw "Frontend build failed." }
        }
        finally {
            $env:VITE_API_BASE_URL = $previousApiBaseUrl
            Pop-Location
        }
    }

    Write-Host "Publishing the frontend..." -ForegroundColor Cyan
    Invoke-AzureCli extension add --name staticwebapp --upgrade --yes
    $deploymentToken = & $script:az staticwebapp secrets list `
        --name $staticWebAppName `
        --resource-group $ResourceGroupName `
        --query properties.apiKey `
        --output tsv
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($deploymentToken)) {
        throw "Could not obtain the Static Web App deployment token."
    }
    Push-Location $frontendDirectory
    try {
        & $npx -y '@azure/static-web-apps-cli' deploy '.\dist' `
            --deployment-token $deploymentToken `
            --env production
        if ($LASTEXITCODE -ne 0) { throw "Frontend deployment failed." }
    }
    finally {
        Pop-Location
    }

    Write-Host "Waiting for the public API health endpoint..." -ForegroundColor Cyan
    $healthy = $false
    for ($attempt = 1; $attempt -le 30; $attempt++) {
        try {
            $health = Invoke-RestMethod "$apiGatewayUrl/actuator/health" -TimeoutSec 10
            if ($health.status -eq 'UP') { $healthy = $true; break }
        }
        catch { Start-Sleep -Seconds 10 }
    }

    Write-Host ""
    Write-Host "TekWatt Azure deployment completed." -ForegroundColor Green
    Write-Host "Frontend: $staticWebAppUrl"
    Write-Host "API Gateway: $apiGatewayUrl"
    Write-Host "Swagger: $apiGatewayUrl/swagger-ui.html"
    Write-Host "OCPP 2.0.1: $ocppWebSocketUrl"
    Write-Host "API health: $(if ($healthy) { 'UP' } else { 'still starting - review Container Apps logs' })"
}
finally {
    $mysqlPasswordText = $null
    $jwtSecretText = $null
    $ocppSharedKeyText = $null
    if (Test-Path -LiteralPath $script:temporaryDirectory) {
        Remove-Item -LiteralPath $script:temporaryDirectory -Recurse -Force
    }
}
