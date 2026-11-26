param(
    [string]$JavaHome = ""
)

$ErrorActionPreference = "Stop"
$projectRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot "..")).Path

if (-not $JavaHome) {
    $candidate = Join-Path $env:USERPROFILE ".jdks\ms-17.0.17"
    if (Test-Path -LiteralPath $candidate) {
        $JavaHome = $candidate
    }
}

if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome "bin\java.exe"))) {
    throw "JDK 17 was not found. Pass its directory with the -JavaHome parameter."
}

$env:JAVA_HOME = (Resolve-Path -LiteralPath $JavaHome).Path

Push-Location $projectRoot
try {
    & (Join-Path $PSScriptRoot "build-all.ps1") -JavaHome $env:JAVA_HOME
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed with exit code $LASTEXITCODE."
    }

    $stamp = Get-Date -Format "yyyyMMdd-HHmmss"
    $releaseRoot = Join-Path $projectRoot "release"
    $packageRoot = Join-Path $releaseRoot "home-school-platform-$stamp"
    New-Item -ItemType Directory -Path $packageRoot -Force | Out-Null

    New-Item -ItemType Directory -Path (Join-Path $packageRoot "backend") -Force | Out-Null
    New-Item -ItemType Directory -Path (Join-Path $packageRoot "frontend") -Force | Out-Null
    Copy-Item `
        -LiteralPath (Join-Path $projectRoot "backend\target\home-school-platform-0.1.0-SNAPSHOT.jar") `
        -Destination (Join-Path $packageRoot "backend\home-school-platform.jar")
    Copy-Item `
        -LiteralPath (Join-Path $projectRoot "frontend\dist") `
        -Destination (Join-Path $packageRoot "frontend\dist") `
        -Recurse
    Copy-Item -LiteralPath (Join-Path $projectRoot "database") -Destination $packageRoot -Recurse
    Copy-Item -LiteralPath (Join-Path $projectRoot "docs") -Destination $packageRoot -Recurse
    Copy-Item -LiteralPath (Join-Path $projectRoot "deploy") -Destination $packageRoot -Recurse
    Copy-Item -LiteralPath (Join-Path $projectRoot "README.md") -Destination $packageRoot

    $archive = Join-Path $releaseRoot "home-school-platform-$stamp.zip"
    Compress-Archive -LiteralPath $packageRoot -DestinationPath $archive -CompressionLevel Optimal

    [pscustomobject]@{
        PackageDirectory = $packageRoot
        Archive = $archive
    } | Format-List
} finally {
    Pop-Location
}

