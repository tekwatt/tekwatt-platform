param(
    [string]$ResourceGroup = 'tekwatt-prod-eastasia-rg',
    [string]$SubscriptionId = '1fe690b7-4f45-454c-80ac-1501ca422669',
    [string]$AcrName = 'tekwattr25ddrfkacr',
    [string]$StorageAccount = 'tekwattr25ddrfkweb',
    [string]$PortalUrl = 'https://tekwattr25ddrfkweb.z7.web.core.windows.net',
    [string]$GatewayUrl = 'https://api-gateway.lemonmushroom-1166ae48.eastasia.azurecontainerapps.io',
    [string]$Maven = 'C:\software\maven\apache-maven-3.9.16\bin\mvn.cmd',
    [string]$JavaHome = 'C:\Program Files\Java\jdk-21.0.12'
)
$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$tag = 'payment-notifications-' + (Get-Date -Format 'yyyyMMddHHmmss')
function Invoke-Az {
    param([string[]]$Arguments)
    $result = & az @Arguments
    if ($LASTEXITCODE -ne 0) { throw "Azure operation failed: $($Arguments[0]) $($Arguments[1]). Deployment stopped." }
    return $result
}
function Wait-Revision {
    param([string]$Name)
    $latest = Invoke-Az @('containerapp','show','-g',$ResourceGroup,'-n',$Name,'--query','properties.latestRevisionName','-o','tsv')
    $deadline = (Get-Date).AddMinutes(5)
    do {
        $health = Invoke-Az @('containerapp','revision','show','-g',$ResourceGroup,'-n',$Name,'--revision',$latest,'--query','properties.healthState','-o','tsv')
        if ($health -eq 'Healthy') { Write-Host "$Name revision $latest is healthy."; return }
        Start-Sleep -Seconds 5
    } while ((Get-Date) -lt $deadline)
    throw "$Name revision $latest did not become healthy. Inspect Container App logs before continuing."
}
if (-not (Get-Command az -ErrorAction SilentlyContinue)) { throw 'Azure CLI is not on PATH.' }
if (-not (Test-Path -LiteralPath $Maven)) { throw "Maven not found: $Maven" }
if (-not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin\java.exe'))) { throw "JDK not found: $JavaHome" }
$account = (Invoke-Az @('account','show','--query','id','-o','tsv')).Trim()
if ($account -ne $SubscriptionId) { throw "Wrong active subscription. Select $SubscriptionId in Azure CLI and rerun." }
foreach ($name in @('charging-session-service','notification-service')) {
    $containers = Invoke-Az @('containerapp','show','-g',$ResourceGroup,'-n',$name,'--query','properties.template.containers[].name','-o','tsv')
    if (@($containers) -notcontains $name) { throw "Expected container '$name' was not found. No resources updated." }
}
$oldJava = $env:JAVA_HOME
$oldApi = $env:VITE_API_BASE_URL
$oldStorageKey = $env:AZURE_STORAGE_KEY
try {
    $env:JAVA_HOME = $JavaHome
    Write-Host 'Building and testing both services (without cleaning running JARs)...'
    & $Maven -f (Join-Path $repo 'backend\pom.xml') "-Dmaven.repo.local=$repo\.maven-repository" -pl 'charging-session-service,notification-service' -am package
    if ($LASTEXITCODE -ne 0) { throw 'Backend build/tests failed. No deployment performed.' }
    Push-Location (Join-Path $repo 'frontend\admin-portal')
    try {
        $env:VITE_API_BASE_URL = $GatewayUrl
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed.' }
        & npm.cmd run audit:navigation
        if ($LASTEXITCODE -ne 0) { throw 'Navigation checks failed.' }
    } finally { Pop-Location }

    # Upload all images before changing either running service. Temporary folders contain no secrets.
    foreach ($name in @('notification-service','charging-session-service')) {
        $suffix = if ($name -eq 'charging-session-service') { '-exec' } else { '' }
        $jar = Join-Path $repo "backend\$name\target\$name-0.1.0-SNAPSHOT$suffix.jar"
        if (-not (Test-Path -LiteralPath $jar)) { throw "Missing build output: $jar" }
        $context = Join-Path $env:TEMP ("tekwatt-payment-" + [guid]::NewGuid().ToString('N'))
        New-Item -ItemType Directory -Path $context | Out-Null
        Copy-Item -LiteralPath $jar -Destination (Join-Path $context 'app.jar')
        Copy-Item -LiteralPath (Join-Path $repo 'infrastructure\azure\backend.Dockerfile') -Destination (Join-Path $context 'Dockerfile')
        Invoke-Az @('acr','build','--registry',$AcrName,'--image',"tekwatt/${name}:$tag",'--file',(Join-Path $context 'Dockerfile'),$context,'--output','none')
    }
    Invoke-Az @('containerapp','update','-g',$ResourceGroup,'-n','notification-service','--container-name','notification-service','--image',"$AcrName.azurecr.io/tekwatt/notification-service:$tag",'--output','none')
    Wait-Revision 'notification-service'
    # SMS deliberately stays disabled until sender configuration and authorization checks pass.
    # One minimum replica is required for the durable billing worker (ongoing Azure cost).
    Invoke-Az @('containerapp','update','-g',$ResourceGroup,'-n','charging-session-service','--container-name','charging-session-service','--image',"$AcrName.azurecr.io/tekwatt/charging-session-service:$tag",'--min-replicas','1','--set-env-vars','BILLING_SERVICE_URL=http://billing-service','INVOICE_SERVICE_URL=http://invoice-service','USER_SERVICE_URL=http://user-service','NOTIFICATION_SERVICE_URL=http://notification-service',"CUSTOMER_PORTAL_URL=$PortalUrl",'AUTO_SESSION_BILLING_ENABLED=true','PAYMENT_SMS_ENABLED=false','--output','none')
    Wait-Revision 'charging-session-service'

    # Keep the storage key out of console output and command-line arguments.
    $env:AZURE_STORAGE_KEY = (Invoke-Az @('storage','account','keys','list','-g',$ResourceGroup,'--account-name',$StorageAccount,'--query','[0].value','-o','tsv')).Trim()
    if ([string]::IsNullOrWhiteSpace($env:AZURE_STORAGE_KEY)) { throw 'Could not obtain storage upload credentials.' }
    Invoke-Az @('storage','blob','upload-batch','--account-name',$StorageAccount,'--auth-mode','key','--destination','$web','--source',(Join-Path $repo 'frontend\admin-portal\dist'),'--overwrite','true','--output','none')
    Invoke-Az @('storage','blob','update','--account-name',$StorageAccount,'--auth-mode','key','--container-name','$web','--name','index.html','--content-cache-control','no-cache','--output','none')
    $health = Invoke-RestMethod "$GatewayUrl/actuator/health" -TimeoutSec 60
    if ($health.status -ne 'UP') { throw 'Public gateway is not healthy after deployment.' }
    $null = Invoke-WebRequest "$PortalUrl/?deployment=$tag" -UseBasicParsing -TimeoutSec 60
    Write-Host "Deployment completed: $tag" -ForegroundColor Green
    Write-Host "Web: $PortalUrl"
    Write-Host 'SMS remains disabled. Test a new completed session and the customer invoice page.'
    Write-Host 'Android source changes require a new mobile build; this script only deploys Azure services and web.'
} finally {
    $env:JAVA_HOME = $oldJava
    $env:VITE_API_BASE_URL = $oldApi
    $env:AZURE_STORAGE_KEY = $oldStorageKey
}
