$ErrorActionPreference = 'Stop'
$labRoot = Split-Path -Parent $PSScriptRoot
Set-Location $labRoot

$sourceOutput = docker compose exec -T sqlserver bash -lc '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -b -h -1 -W -s "," -i /migration/row-counts-mssql.sql'
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to read SQL Server row counts.'
}

$targetOutput = docker compose exec -T postgres bash -lc 'psql -q -U "$POSTGRES_USER" -d stackoverflow_base -f /migration/row-counts-postgres.sql'
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to read PostgreSQL row counts.'
}

function ConvertTo-CountMap([string[]]$Lines) {
    $map = @{}
    foreach ($line in $Lines) {
        if ($line.Trim() -match '^([^,]+),(\d+)$') {
            $map[$matches[1].Trim().ToLowerInvariant()] = [long]$matches[2]
        }
    }
    return $map
}

$sourceCounts = ConvertTo-CountMap $sourceOutput
$targetCounts = ConvertTo-CountMap $targetOutput
$failed = $false

foreach ($tableName in ($sourceCounts.Keys | Sort-Object)) {
    $sourceCount = $sourceCounts[$tableName]
    $targetCount = $targetCounts[$tableName]
    $matches = $null -ne $targetCount -and $sourceCount -eq $targetCount
    [pscustomobject]@{
        Table = $tableName
        SqlServer = $sourceCount
        PostgreSQL = $targetCount
        Matches = $matches
    }
    if (-not $matches) {
        $failed = $true
    }
}

if ($sourceCounts.Count -eq 0 -or $targetCounts.Count -eq 0) {
    throw 'No table row counts were available for comparison.'
}
if ($failed) {
    throw 'SQL Server and PostgreSQL table row counts do not match.'
}
