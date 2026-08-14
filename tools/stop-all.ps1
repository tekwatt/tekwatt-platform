$platformRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$pidFile = Join-Path $platformRoot "logs\service-pids.json"

if (-not (Test-Path $pidFile)) {
    Write-Host "No TekWatt process file was found."
    exit 0
}

$processes = Get-Content $pidFile -Raw | ConvertFrom-Json
foreach ($entry in $processes) {
    $process = Get-Process -Id $entry.processId -ErrorAction SilentlyContinue
    if ($null -ne $process) {
        Stop-Process -Id $entry.processId
        Write-Host "Stopped $($entry.service) (PID $($entry.processId))"
    }
}

Write-Host "TekWatt services stopped."
