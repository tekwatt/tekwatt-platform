param(
    [switch]$ForceAllJava
)

$ErrorActionPreference = "Continue"
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
        & taskkill.exe /PID $entry.processId /T /F 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Stopped $($entry.service) process tree (PID $($entry.processId))"
        }
    }
}

if ($ForceAllJava) {
    $javaProcesses = Get-Process java, javaw -ErrorAction SilentlyContinue
    foreach ($process in $javaProcesses) {
        & taskkill.exe /PID $process.Id /T /F 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) {
            Write-Host "Stopped stale Java process (PID $($process.Id))"
        }
    }
}

Write-Host "TekWatt services stopped."
