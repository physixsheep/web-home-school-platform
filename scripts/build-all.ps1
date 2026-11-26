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

Push-Location (Join-Path $projectRoot "backend")
try {
    mvn clean verify
    if ($LASTEXITCODE -ne 0) {
        throw "Backend build failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

Push-Location (Join-Path $projectRoot "frontend")
try {
    if (Test-Path -LiteralPath "pnpm-lock.yaml") {
        pnpm install --frozen-lockfile
    } else {
        pnpm install
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend dependency installation failed with exit code $LASTEXITCODE."
    }
    pnpm build
    if ($LASTEXITCODE -ne 0) {
        throw "Frontend build failed with exit code $LASTEXITCODE."
    }
} finally {
    Pop-Location
}

