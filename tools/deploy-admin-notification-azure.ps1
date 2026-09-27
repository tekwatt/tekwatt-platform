param(
    [string]$ResourceGroup = 'tekwatt-prod-eastasia-rg',
    [string]$AcrName = 'tekwattr25ddrfkacr',
    [string]$StorageAccount = 'tekwattr25ddrfkweb',
    [string]$GatewayUrl = 'https://api-gateway.lemonmushroom-1166ae48.eastasia.azurecontainerapps.io',
    [string]$PortalUrl = 'https://tekwattr25ddrfkweb.z7.web.core.windows.net'
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$tag = 'admin-notification-' + (Get-Date -Format 'yyyyMMddHHmmss')
$maven = 'C:\software\maven\apache-maven-3.9.16\bin\mvn.cmd'
$oldApi = $env:VITE_API_BASE_URL
$oldStorageKey = $env:AZURE_STORAGE_KEY
$context = $null
function Invoke-Az {
    param([string[]]$Arguments)
    $result = & az @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Azure operation failed: $($Arguments[0]) $($Arguments[1])." }
    return $result
}
try {
    if ((Invoke-Az @('account','show','--query','id','-o','tsv')).Trim() -ne '1fe690b7-4f45-454c-80ac-1501ca422669') { throw 'Wrong Azure subscription.' }
    $containers = Invoke-Az @('containerapp','show','-g',$ResourceGroup,'-n','notification-service','--query','properties.template.containers[].name','-o','tsv')
    if (@($containers) -notcontains 'notification-service') { throw 'Expected notification-service container not found.' }
    & $maven -f (Join-Path $repo 'backend\pom.xml') "-Dmaven.repo.local=$repo\.maven-repository" -pl notification-service -am package
    if ($LASTEXITCODE -ne 0) { throw 'Notification-service build or tests failed. No deployment performed.' }
    Push-Location (Join-Path $repo 'frontend\admin-portal')
    try {
        $env:VITE_API_BASE_URL = $GatewayUrl
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed. No deployment performed.' }
        & npm.cmd run audit:navigation
        if ($LASTEXITCODE -ne 0) { throw 'Navigation audit failed. No deployment performed.' }
    } finally { Pop-Location }
    $jar = Join-Path $repo 'backend\notification-service\target\notification-service-0.1.0-SNAPSHOT.jar'
    if (-not (Test-Path -LiteralPath $jar)) { throw "Missing build output: $jar" }
    $context = Join-Path $env:TEMP ('tekwatt-notification-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $context | Out-Null
    Copy-Item -LiteralPath $jar -Destination (Join-Path $context 'app.jar')
    Copy-Item -LiteralPath (Join-Path $repo 'infrastructure\azure\backend.Dockerfile') -Destination (Join-Path $context 'Dockerfile')
    Invoke-Az @('acr','build','--registry',$AcrName,'--image',"tekwatt/notification-service:$tag",'--file',(Join-Path $context 'Dockerfile'),$context,'--output','none')
    Invoke-Az @('containerapp','update','-g',$ResourceGroup,'-n','notification-service','--container-name','notification-service','--image',"$AcrName.azurecr.io/tekwatt/notification-service:$tag",'--set-env-vars','ADMIN_SERVICE_URL=http://admin-service','--output','none')
    $revision = (Invoke-Az @('containerapp','show','-g',$ResourceGroup,'-n','notification-service','--query','properties.latestRevisionName','-o','tsv')).Trim()
    $deadline = (Get-Date).AddMinutes(5)
    do {
        $health = (Invoke-Az @('containerapp','revision','show','-g',$ResourceGroup,'-n','notification-service','--revision',$revision,'--query','properties.healthState','-o','tsv')).Trim()
        if ($health -eq 'Healthy') { break }
        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $deadline)
    if ($health -ne 'Healthy') { throw "Notification-service revision $revision is not healthy. Web app was not uploaded." }
    $env:AZURE_STORAGE_KEY = (Invoke-Az @('storage','account','keys','list','-g',$ResourceGroup,'--account-name',$StorageAccount,'--query','[0].value','-o','tsv')).Trim()
    if ([string]::IsNullOrWhiteSpace($env:AZURE_STORAGE_KEY)) { throw 'Storage upload credential unavailable.' }
    Invoke-Az @('storage','blob','upload-batch','--account-name',$StorageAccount,'--auth-mode','key','--destination','$web','--source',(Join-Path $repo 'frontend\admin-portal\dist'),'--overwrite','true','--output','none')
    Invoke-Az @('storage','blob','update','--account-name',$StorageAccount,'--auth-mode','key','--container-name','$web','--name','index.html','--content-cache-control','no-cache','--output','none')
    if ((Invoke-RestMethod "$GatewayUrl/actuator/health" -TimeoutSec 60).status -ne 'UP') { throw 'Gateway health check failed.' }
    $null = Invoke-WebRequest "$PortalUrl/?deployment=$tag" -UseBasicParsing -TimeoutSec 60
    Write-Host "Deployment complete: $tag"
} finally {
    $env:VITE_API_BASE_URL = $oldApi
    $env:AZURE_STORAGE_KEY = $oldStorageKey
    if ($context -and (Test-Path -LiteralPath $context)) {
        $resolved = (Resolve-Path -LiteralPath $context).Path
        $tempRoot = (Resolve-Path -LiteralPath $env:TEMP).Path.TrimEnd('\') + '\'
        if ($resolved.StartsWith($tempRoot,[System.StringComparison]::OrdinalIgnoreCase) -and (Split-Path -Leaf $resolved) -like 'tekwatt-notification-*') {
            Remove-Item -LiteralPath $resolved -Recurse -Force
        }
    }
}
