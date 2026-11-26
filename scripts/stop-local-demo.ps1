$ErrorActionPreference = "Stop"

$tempBase = [IO.Path]::GetTempPath().TrimEnd("\")
$runtimeRoot = Join-Path $tempBase "home-school-platform-demo"
$stateFile = Join-Path $runtimeRoot "processes.json"
$mysqladmin = "D:\soft\mysql\bin\mysqladmin.exe"

if (-not (Test-Path -LiteralPath $stateFile)) {
    Write-Host "No local demo state file was found."
    exit 0
}

$state = Get-Content -LiteralPath $stateFile -Raw | ConvertFrom-Json

foreach ($processId in @($state.frontendPid, $state.backendPid)) {
    if ($processId) {
        Stop-Process -Id $processId -Force -ErrorAction SilentlyContinue
    }
}

if (Test-Path -LiteralPath $mysqladmin) {
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        & $mysqladmin `
            --protocol=TCP `
            --host=127.0.0.1 `
            "--port=$($state.mysqlPort)" `
            --user=root `
            shutdown 2>&1 | Out-Null
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

if ($state.mysqlPid) {
    Stop-Process -Id $state.mysqlPid -Force -ErrorAction SilentlyContinue
}

Remove-Item -LiteralPath $stateFile -Force -ErrorAction SilentlyContinue
Write-Host "Home-school platform local demo services stopped."

