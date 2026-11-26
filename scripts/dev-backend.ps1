param(
    [string]$JavaHome = ""
)

$ErrorActionPreference = "Stop"

if (-not $JavaHome) {
    $candidate = Join-Path $env:USERPROFILE ".jdks\ms-17.0.17"
    if (Test-Path -LiteralPath $candidate) {
        $JavaHome = $candidate
    } elseif ($env:JAVA_HOME) {
        $JavaHome = $env:JAVA_HOME
    }
}

if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome "bin\java.exe"))) {
    throw "JDK 17 was not found. Pass its directory with the -JavaHome parameter."
}

$resolvedJavaHome = (Resolve-Path -LiteralPath $JavaHome).Path
$versionOutput = & (Join-Path $resolvedJavaHome "bin\java.exe") -version 2>&1
if (($versionOutput -join "`n") -notmatch 'version "17\.') {
    throw "The selected JavaHome is not JDK 17: $resolvedJavaHome"
}

$env:JAVA_HOME = $resolvedJavaHome
$env:PATH = "$resolvedJavaHome\bin;$env:PATH"

Push-Location (Join-Path $PSScriptRoot "..")
try {
    mvn -f backend/pom.xml spring-boot:run
} finally {
    Pop-Location
}

