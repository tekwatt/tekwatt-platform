param(
    [string]$ResourceGroup = 'tekwatt-prod-eastasia-rg',
    [string]$AcrName = 'tekwattr25ddrfkacr',
    [string]$StorageAccount = 'tekwattr25ddrfkweb',
    [string]$GatewayUrl = 'https://api-gateway.lemonmushroom-1166ae48.eastasia.azurecontainerapps.io',
    [string]$ProviderAdminEmails = 'admin@tekwatt.in',
    [switch]$NotificationOnly
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$tag = 'payment-sms-config-' + (Get-Date -Format 'yyyyMMddHHmmss')
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
    $services = if ($NotificationOnly) { @('notification-service') } else { @('payment-service','notification-service') }
    foreach ($service in $services) {
        $containers = Invoke-Az @('containerapp','show','-g',$ResourceGroup,'-n',$service,'--query','properties.template.containers[].name','-o','tsv')
        if (@($containers) -notcontains $service) { throw "Expected $service container not found." }
    }
    & $maven -f (Join-Path $repo 'backend\pom.xml') "-Dmaven.repo.local=$repo\.maven-repository" -pl ($services -join ',') -am package
    if ($LASTEXITCODE -ne 0) { throw 'Payment or notification build or tests failed. No deployment performed.' }
    Push-Location (Join-Path $repo 'frontend\admin-portal')
    try {
        $env:VITE_API_BASE_URL = $GatewayUrl
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed. No deployment performed.' }
        & npm.cmd run audit:navigation
        if ($LASTEXITCODE -ne 0) { throw 'Navigation audit failed. No deployment performed.' }
    } finally { Pop-Location }
    $context = Join-Path $env:TEMP ('tekwatt-payment-' + [guid]::NewGuid().ToString('N'))
    New-Item -ItemType Directory -Path $context | Out-Null
    foreach ($service in $services) {
        $jar = Join-Path $repo "backend\$service\target\$service-0.1.0-SNAPSHOT.jar"
        if (-not (Test-Path -LiteralPath $jar)) { throw "Missing build output: $jar" }
        $serviceContext = Join-Path $context $service
        New-Item -ItemType Directory -Path $serviceContext | Out-Null
        Copy-Item -LiteralPath $jar -Destination (Join-Path $serviceContext 'app.jar')
        Copy-Item -LiteralPath (Join-Path $repo 'infrastructure\azure\backend.Dockerfile') -Destination (Join-Path $serviceContext 'Dockerfile')
        Invoke-Az @('acr','build','--registry',$AcrName,'--image',"tekwatt/$service`:$tag",'--file',(Join-Path $serviceContext 'Dockerfile'),$serviceContext,'--output','none')
    }
    $secretName = 'sms-provider-encryption-key'
    $existingOutput = Invoke-Az @('containerapp','secret','list','-g',$ResourceGroup,'-n','notification-service','--query',"[?name=='$secretName'].name | [0]",'-o','tsv')
    $existing = if ([string]::IsNullOrWhiteSpace($existingOutput)) { '' } else { ([string]$existingOutput).Trim() }
    if ($existing -ne $secretName) {
        $raw = New-Object byte[] 32
        $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
        try {
            $rng.GetBytes($raw)
            $generated = [Convert]::ToBase64String($raw)
            Invoke-Az @('containerapp','secret','set','-g',$ResourceGroup,'-n','notification-service','--secrets',"$secretName=$generated",'--output','none')
        } finally {
            $rng.Dispose()
            [Array]::Clear($raw,0,$raw.Length)
            $generated = $null
        }
    }
    Invoke-Az @('containerapp','update','-g',$ResourceGroup,'-n','notification-service','--container-name','notification-service','--image',"$AcrName.azurecr.io/tekwatt/notification-service:$tag",'--set-env-vars',"SMS_PROVIDER_ENCRYPTION_KEY=secretref:$secretName",'AUTH_SERVICE_URL=http://auth-service','ADMIN_SERVICE_URL=http://admin-service',"SMS_PROVIDER_ADMIN_EMAILS=$ProviderAdminEmails",'--output','none')
    if (-not $NotificationOnly) {
        Invoke-Az @('containerapp','update','-g',$ResourceGroup,'-n','payment-service','--container-name','payment-service','--image',"$AcrName.azurecr.io/tekwatt/payment-service:$tag",'--output','none')
    }
    foreach ($service in $services) {
        $revisionOutput = Invoke-Az @('containerapp','show','-g',$ResourceGroup,'-n',$service,'--query','properties.latestRevisionName','-o','tsv')
        if ([string]::IsNullOrWhiteSpace($revisionOutput)) { throw "$service has no latest revision after update." }
        $revision = ([string]$revisionOutput).Trim()
        $deadline = (Get-Date).AddMinutes(5)
        do {
            $healthOutput = Invoke-Az @('containerapp','revision','show','-g',$ResourceGroup,'-n',$service,'--revision',$revision,'--query','properties.healthState','-o','tsv')
            $health = if ([string]::IsNullOrWhiteSpace($healthOutput)) { 'Unknown' } else { ([string]$healthOutput).Trim() }
            if ($health -eq 'Healthy') { break }
            Start-Sleep -Seconds 5
        } while ((Get-Date) -lt $deadline)
        if ($health -ne 'Healthy') { throw "$service revision $revision is not healthy. Web app was not uploaded." }
        Write-Host "$service revision $revision is Healthy."
    }
    $storageKeyOutput = Invoke-Az @('storage','account','keys','list','-g',$ResourceGroup,'--account-name',$StorageAccount,'--query','[0].value','-o','tsv')
    $env:AZURE_STORAGE_KEY = if ([string]::IsNullOrWhiteSpace($storageKeyOutput)) { '' } else { ([string]$storageKeyOutput).Trim() }
    if ([string]::IsNullOrWhiteSpace($env:AZURE_STORAGE_KEY)) { throw 'Storage upload credential unavailable.' }
    Invoke-Az @('storage','blob','upload-batch','--account-name',$StorageAccount,'--auth-mode','key','--destination','$web','--source',(Join-Path $repo 'frontend\admin-portal\dist'),'--overwrite','true','--output','none')
    Invoke-Az @('storage','blob','update','--account-name',$StorageAccount,'--auth-mode','key','--container-name','$web','--name','index.html','--content-cache-control','no-cache','--output','none')
    Write-Host "Deployment complete: $tag; updated backend revisions are Healthy."
} finally {
    $env:VITE_API_BASE_URL = $oldApi
    $env:AZURE_STORAGE_KEY = $oldStorageKey
    if ($context -and (Test-Path -LiteralPath $context)) {
        $resolved = (Resolve-Path -LiteralPath $context).Path
        $tempRoot = (Resolve-Path -LiteralPath $env:TEMP).Path.TrimEnd('\') + '\'
        if ($resolved.StartsWith($tempRoot,[System.StringComparison]::OrdinalIgnoreCase) -and (Split-Path -Leaf $resolved) -like 'tekwatt-payment-*') {
            Remove-Item -LiteralPath $resolved -Recurse -Force
        }
    }
}
