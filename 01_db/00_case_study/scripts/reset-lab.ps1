$ErrorActionPreference = 'Stop'
$labRoot = Split-Path -Parent $PSScriptRoot
Set-Location $labRoot

docker compose exec -T postgres bash -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -f /migration/prepare-lab.sql'
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to reset the lab database.'
}

Write-Host 'Reset stackoverflow_lab from stackoverflow_base.'
