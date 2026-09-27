# Deploy the current notification-service and admin-portal builds, including the
# MSG91 per-event Flow template registry. Run from a PowerShell window where
# `az account show` succeeds. No payment service or SMS is started by this script.
[CmdletBinding()]
param(
    [string]$ResourceGroup = 'tekwatt-prod-eastasia-rg',
    [string]$AcrName = 'tekwattr25ddrfkacr',
    [string]$StorageAccount = 'tekwattr25ddrfkweb',
    [string]$GatewayUrl = 'https://api-gateway.lemonmushroom-1166ae48.eastasia.azurecontainerapps.io',
    [string]$PortalUrl = 'https://tekwattr25ddrfkweb.z7.web.core.windows.net',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$migration = Join-Path $repo 'backend\notification-service\src\main\resources\db\migration\V4__sms_flow_templates.sql'
$deploy = Join-Path $PSScriptRoot 'deploy-razorpay-test-azure.ps1'
if (-not (Test-Path -LiteralPath $migration)) { throw 'MSG91 template migration V4 is missing from this checkout.' }
if (-not (Test-Path -LiteralPath $deploy)) { throw 'Notification deployment script is missing from this checkout.' }

Write-Host 'This publishes the CURRENT local notification-service and admin-portal code, including other uncommitted changes in those projects.'
Write-Host 'It does not enable automatic charging SMS or send a test message.'
if (-not $Force) {
    $answer = Read-Host 'Type YES to continue'
    if ($answer -cne 'YES') { Write-Host 'Deployment cancelled.'; return }
}

& powershell.exe -NoProfile -ExecutionPolicy Bypass -File $deploy `
    -NotificationOnly `
    -ResourceGroup $ResourceGroup `
    -AcrName $AcrName `
    -StorageAccount $StorageAccount `
    -GatewayUrl $GatewayUrl
if ($LASTEXITCODE -ne 0) { throw 'Notification-service or frontend deployment failed; check the output above.' }

# The underlying script confirms the new backend revision is Healthy. Check
# that the public static site serves the newly built MSG91 form as well.
$index = (Invoke-WebRequest -Uri "$PortalUrl/?sms-template-check=$(Get-Date -Format yyyyMMddHHmmss)" -UseBasicParsing -TimeoutSec 60).Content
$match = [regex]::Match($index, '/assets/[^"\s]+\.js')
if (-not $match.Success) { throw 'Deployment finished, but the hosted page did not reference a JavaScript bundle. Check the frontend upload.' }
$bundle = (Invoke-WebRequest -Uri ($PortalUrl.TrimEnd('/') + $match.Value) -UseBasicParsing -TimeoutSec 60).Content
if (-not $bundle.Contains('MSG91 Flow templates')) {
    throw 'Deployment finished, but the hosted JavaScript does not contain the MSG91 template list. Refresh the site and check storage upload.'
}
Write-Host 'Verified: Azure notification-service revision is Healthy and the hosted MSG91 template list is available.'
