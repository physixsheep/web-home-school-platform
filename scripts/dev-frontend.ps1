$ErrorActionPreference = "Stop"

Push-Location (Join-Path $PSScriptRoot "..\frontend")
try {
    if (-not (Test-Path -LiteralPath "node_modules")) {
        pnpm install
    }
    pnpm dev
} finally {
    Pop-Location
}

