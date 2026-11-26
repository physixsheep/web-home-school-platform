param(
    [switch]$OpenBrowser
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path
$tempBase = [IO.Path]::GetTempPath().TrimEnd("\")
$runtimeRoot = Join-Path $tempBase "home-school-platform-demo"
$dataDir = Join-Path $runtimeRoot "mysql-data"
$stateFile = Join-Path $runtimeRoot "processes.json"
$mysqlBase = "D:\soft\mysql"
$mysqld = Join-Path $mysqlBase "bin\mysqld.exe"
$mysql = Join-Path $mysqlBase "bin\mysql.exe"
$mysqladmin = Join-Path $mysqlBase "bin\mysqladmin.exe"
$mysqlPort = 33306
$apiPort = 8080
$frontendPort = 5173

function Wait-Http {
    param(
        [string]$Uri,
        [int]$TimeoutSeconds = 60
    )
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Uri -TimeoutSec 2
            if ($response.StatusCode -eq 200) {
                return $true
            }
        } catch {
            Start-Sleep -Milliseconds 500
        }
    }
    return $false
}

function Invoke-Native {
    param(
        [string]$FilePath,
        [string[]]$Arguments
    )
    $previousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = "Continue"
        & $FilePath @Arguments 2>&1 | Out-Null
        return $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $previousPreference
    }
}

function Test-Port {
    param([int]$Port)
    return $null -ne (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue)
}

if (-not (Test-Path -LiteralPath $mysqld)) {
    throw "MySQL server binary not found: $mysqld"
}

$javaHome = Join-Path $env:USERPROFILE ".jdks\ms-17.0.17"
if (-not (Test-Path -LiteralPath (Join-Path $javaHome "bin\java.exe"))) {
    throw "JDK 17 not found: $javaHome"
}

$pnpmCommand = Get-Command pnpm.cmd -ErrorAction Stop
$jar = Join-Path $projectRoot "backend\target\home-school-platform-0.1.0-SNAPSHOT.jar"
if (-not (Test-Path -LiteralPath $jar) -or -not (Test-Path -LiteralPath (Join-Path $projectRoot "frontend\node_modules"))) {
    & (Join-Path $PSScriptRoot "build-all.ps1") -JavaHome $javaHome
}

New-Item -ItemType Directory -Path $runtimeRoot -Force | Out-Null

$mysqlProcess = $null
if (-not (Test-Port $mysqlPort)) {
    $initialized = Test-Path -LiteralPath (Join-Path $dataDir "auto.cnf")
    if (-not $initialized) {
        New-Item -ItemType Directory -Path $dataDir -Force | Out-Null
        $initializeExit = Invoke-Native `
            -FilePath $mysqld `
            -Arguments @(
                "--no-defaults",
                "--initialize-insecure",
                "--basedir=$mysqlBase",
                "--datadir=$dataDir"
            )
        if ($initializeExit -ne 0) {
            throw "Failed to initialize the local demo MySQL data directory."
        }
    }

    $mysqlProcess = Start-Process `
        -FilePath $mysqld `
        -ArgumentList @(
            "--no-defaults",
            "--console",
            "--basedir=$mysqlBase",
            "--datadir=$dataDir",
            "--port=$mysqlPort",
            "--bind-address=127.0.0.1",
            "--mysqlx=0",
            "--pid-file=$dataDir\mysqld.pid",
            "--log-error=$runtimeRoot\mysql-error.log"
        ) `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runtimeRoot "mysql.stdout.log") `
        -RedirectStandardError (Join-Path $runtimeRoot "mysql.stderr.log") `
        -PassThru

    $mysqlReady = $false
    $deadline = (Get-Date).AddSeconds(45)
    while ((Get-Date) -lt $deadline -and -not $mysqlReady) {
        $pingExit = Invoke-Native `
            -FilePath $mysqladmin `
            -Arguments @(
                "--protocol=TCP",
                "--host=127.0.0.1",
                "--port=$mysqlPort",
                "--user=root",
                "ping"
            )
        $mysqlReady = $pingExit -eq 0
        if (-not $mysqlReady) {
            Start-Sleep -Milliseconds 500
        }
    }
    if (-not $mysqlReady) {
        throw "Local demo MySQL did not become ready. Check $runtimeRoot\mysql-error.log"
    }
}

Push-Location $projectRoot
try {
    $initExit = Invoke-Native `
        -FilePath $mysql `
        -Arguments @(
            "--protocol=TCP",
            "--host=127.0.0.1",
            "--port=$mysqlPort",
            "--user=root",
            "--default-character-set=utf8mb4",
            "--execute=source database/init_all.sql"
        )
    if ($initExit -ne 0) {
        throw "Failed to initialize the platform database."
    }
} finally {
    Pop-Location
}

$backendProcess = $null
$backendReady = Wait-Http -Uri "http://127.0.0.1:$apiPort/v3/api-docs" -TimeoutSeconds 2
if (-not $backendReady) {
    $env:JAVA_HOME = $javaHome
    $env:DB_URL = "jdbc:mysql://127.0.0.1:$mysqlPort/home_school_platform?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false"
    $env:DB_USERNAME = "root"
    $env:DB_PASSWORD = ""
    $env:SERVER_PORT = "$apiPort"
    $env:UPLOAD_ROOT = Join-Path $projectRoot "storage"

    $backendProcess = Start-Process `
        -FilePath (Join-Path $javaHome "bin\java.exe") `
        -ArgumentList @("-jar", "`"$jar`"") `
        -WorkingDirectory $projectRoot `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runtimeRoot "backend.stdout.log") `
        -RedirectStandardError (Join-Path $runtimeRoot "backend.stderr.log") `
        -PassThru

    if (-not (Wait-Http -Uri "http://127.0.0.1:$apiPort/v3/api-docs" -TimeoutSeconds 60)) {
        throw "Backend did not become ready. Check $runtimeRoot\backend.stderr.log"
    }
}

$frontendProcess = $null
$frontendReady = Wait-Http -Uri "http://127.0.0.1:$frontendPort" -TimeoutSeconds 2
if (-not $frontendReady) {
    $frontendProcess = Start-Process `
        -FilePath $pnpmCommand.Source `
        -ArgumentList @("dev", "--host", "127.0.0.1") `
        -WorkingDirectory (Join-Path $projectRoot "frontend") `
        -WindowStyle Hidden `
        -RedirectStandardOutput (Join-Path $runtimeRoot "frontend.stdout.log") `
        -RedirectStandardError (Join-Path $runtimeRoot "frontend.stderr.log") `
        -PassThru

    if (-not (Wait-Http -Uri "http://127.0.0.1:$frontendPort" -TimeoutSeconds 60)) {
        throw "Frontend did not become ready. Check $runtimeRoot\frontend.stderr.log"
    }
}

$state = @{
    runtimeRoot = $runtimeRoot
    mysqlPort = $mysqlPort
    mysqlPid = if ($mysqlProcess) { $mysqlProcess.Id } else { $null }
    backendPid = if ($backendProcess) { $backendProcess.Id } else { $null }
    frontendPid = if ($frontendProcess) { $frontendProcess.Id } else { $null }
}
$state | ConvertTo-Json | Set-Content -LiteralPath $stateFile -Encoding UTF8

$url = "http://127.0.0.1:$frontendPort"
if ($OpenBrowser) {
    Start-Process $url
}

Write-Host ""
Write-Host "Home-school platform is running."
Write-Host "Frontend: $url"
Write-Host "Swagger:  http://127.0.0.1:$apiPort/swagger-ui.html"
Write-Host "Logs:     $runtimeRoot"
Write-Host ""
Write-Host "Test accounts:"
Write-Host "admin / Admin@123"
Write-Host "teacher01 / Teacher@123"
Write-Host "student01 / Student@123"
Write-Host "parent01 / Parent@123"

