param(
    [switch]$ForceAllJava
)

$ErrorActionPreference = "Continue"
$platformRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$pidFile = Join-Path $platformRoot "logs\service-pids.json"

if (Test-Path $pidFile) {
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
} else {
    Write-Host "No TekWatt process file was found; checking for stale TekWatt JVMs."
}

# The PID file can be replaced by a later start attempt while older JVMs are still
# booting. jps lets us remove only TekWatt processes without closing Android Studio
# or unrelated Java applications.
$jpsPath = (Get-Command jps -ErrorAction SilentlyContinue).Source
if (-not $jpsPath -and $env:JAVA_HOME) {
    $candidate = Join-Path $env:JAVA_HOME "bin\jps.exe"
    if (Test-Path $candidate) { $jpsPath = $candidate }
}
if ($jpsPath) {
    $backendMarker = [regex]::Escape((Join-Path $platformRoot "backend") + "\")
    $staleJvms = & $jpsPath -lv 2>$null | Where-Object { $_ -match "^\d+\s+$backendMarker" }
    foreach ($line in $staleJvms) {
        $processId = [int](($line -split "\s+", 2)[0])
        if (Get-Process -Id $processId -ErrorAction SilentlyContinue) {
            & taskkill.exe /PID $processId /T /F 2>$null | Out-Null
            if ($LASTEXITCODE -eq 0) {
                Write-Host "Stopped stale TekWatt JVM (PID $processId)"
            }
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
