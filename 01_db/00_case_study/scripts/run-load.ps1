param(
    [string]$Tag = 'java',
    [int]$VirtualUsers = 20,
    [string]$Duration = '30s'
)

$ErrorActionPreference = 'Stop'
$labRoot = Split-Path -Parent $PSScriptRoot
Set-Location $labRoot

if ($VirtualUsers -lt 1 -or $VirtualUsers -gt 500) {
    throw 'VirtualUsers must be between 1 and 500.'
}
if ($Duration -notmatch '^\d+(s|m)$') {
    throw 'Duration must use a format such as 30s or 2m.'
}

$env:LOAD_TAG = $Tag
$env:LOAD_VUS = $VirtualUsers.ToString()
$env:LOAD_DURATION = $Duration

docker compose --profile app up -d --build api
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to start the API container.'
}

docker compose exec -T postgres bash -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d stackoverflow_lab -f /observability/reset-statements.sql'
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to reset PostgreSQL statistics.'
}

$artifactDirectory = Join-Path $labRoot 'artifacts'
New-Item -ItemType Directory -Path $artifactDirectory -Force | Out-Null
$startedAt = Get-Date
$timestamp = $startedAt.ToString('yyyyMMdd-HHmmss')
$k6LogPath = Join-Path $artifactDirectory "k6-$timestamp.txt"
$reportPath = Join-Path $artifactDirectory "db-report-$timestamp.txt"

docker compose --profile app --profile load run --rm k6 |
    Tee-Object -LiteralPath $k6LogPath
$k6ExitCode = $LASTEXITCODE

docker compose exec -T postgres bash -lc 'psql -U "$POSTGRES_USER" -d stackoverflow_lab -f /observability/report.sql' |
    Out-File -LiteralPath $reportPath -Encoding utf8

Write-Host "k6 result: $k6LogPath"
Write-Host "DB report: $reportPath"
if ($k6ExitCode -ne 0) {
    throw "k6 thresholds failed. Exit code: $k6ExitCode"
}
