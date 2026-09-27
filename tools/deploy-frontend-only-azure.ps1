param(
    [string]$ResourceGroup = 'tekwatt-prod-eastasia-rg',
    [string]$StorageAccount = 'tekwattr25ddrfkweb',
    [string]$GatewayUrl = 'https://api-gateway.lemonmushroom-1166ae48.eastasia.azurecontainerapps.io'
)

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$frontend = Join-Path $repo 'frontend\admin-portal'
$previousApiUrl = $env:VITE_API_BASE_URL
$previousStorageKey = $env:AZURE_STORAGE_KEY

try {
    $subscription = & az account show --query id --output tsv
    if ($LASTEXITCODE -ne 0 -or ([string]$subscription).Trim() -ne '1fe690b7-4f45-454c-80ac-1501ca422669') {
        throw 'Azure CLI is not signed in to the expected TekWatt subscription.'
    }
    $env:VITE_API_BASE_URL = $GatewayUrl
    Push-Location $frontend
    try {
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) { throw 'Frontend build failed. Nothing was uploaded.' }
        & npm.cmd run audit:navigation
        if ($LASTEXITCODE -ne 0) { throw 'Navigation audit failed. Nothing was uploaded.' }
    } finally { Pop-Location }

    $key = & az storage account keys list --resource-group $ResourceGroup --account-name $StorageAccount --query '[0].value' --output tsv
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($key)) { throw 'Azure storage key is unavailable. Nothing was uploaded.' }
    $env:AZURE_STORAGE_KEY = ([string]$key).Trim()
    $key = $null
    & az storage blob upload-batch --account-name $StorageAccount --auth-mode key --destination '$web' --source (Join-Path $frontend 'dist') --overwrite true --output none
    if ($LASTEXITCODE -ne 0) { throw 'Frontend upload failed.' }
    & az storage blob update --account-name $StorageAccount --auth-mode key --container-name '$web' --name index.html --content-cache-control no-cache --output none
    if ($LASTEXITCODE -ne 0) { throw 'Frontend uploaded, but index.html cache policy could not be updated.' }
    Write-Host 'Frontend published. Refresh the TekWatt page to use the new SMS-provider selector.'
} finally {
    $env:VITE_API_BASE_URL = $previousApiUrl
    $env:AZURE_STORAGE_KEY = $previousStorageKey
}
