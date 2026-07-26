param(
    [ValidateSet('Full', 'PostMigration', 'LabOnly')]
    [string]$Stage = 'Full',
    [switch]$SkipExtraction,
    [switch]$KeepSqlServer
)

$ErrorActionPreference = 'Stop'
$labRoot = Split-Path -Parent $PSScriptRoot
$startedAt = Get-Date
Set-Location $labRoot

function Write-Step([string]$Message) {
    $elapsed = (Get-Date) - $startedAt
    Write-Host "[$($elapsed.ToString('hh\:mm\:ss'))] $Message"
}

function Wait-Healthy([string]$ServiceName, [int]$TimeoutSeconds) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        $containerId = docker compose ps -q $ServiceName
        if ($containerId) {
            $status = docker inspect --format '{{.State.Health.Status}}' $containerId
            if ($status -eq 'healthy') {
                return
            }
            if ($status -eq 'unhealthy') {
                throw "$ServiceName container is unhealthy."
            }
        }
        Start-Sleep -Seconds 2
    }

    $recentLogs = docker compose logs --tail=40 $ServiceName 2>&1 | Out-String
    throw "Timed out after $TimeoutSeconds seconds waiting for the $ServiceName container.`n$recentLogs"
}

function Remove-MigrationSource {
    Write-Step 'Removing the migration-only SQL Server container and data volume.'
    docker compose rm -s -f sqlserver
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to remove the SQL Server container.'
    }

    $volumeNames = @(
        docker volume ls `
            --filter 'label=com.docker.compose.project=stackoverflow-db-lab' `
            --filter 'label=com.docker.compose.volume=sqlserver-data' `
            --format '{{.Name}}'
    ) | Where-Object { $_ }

    if ($volumeNames.Count -gt 1) {
        throw "Expected at most one SQL Server data volume, found: $($volumeNames -join ', ')"
    }
    if ($volumeNames.Count -eq 1) {
        docker volume rm $volumeNames[0]
        if ($LASTEXITCODE -ne 0) {
            throw "Failed to remove SQL Server data volume: $($volumeNames[0])"
        }
    }
}

if (-not (Test-Path -LiteralPath '.env')) {
    Copy-Item -LiteralPath '.env.example' -Destination '.env'
    Write-Host 'Created .env from .env.example for this local lab.'
}

$envValues = @{}
Get-Content -Encoding UTF8 -LiteralPath '.env' | ForEach-Object {
    if ($_ -match '^([^#=]+)=(.*)$') {
        $envValues[$matches[1].Trim()] = $matches[2].Trim()
    }
}

foreach ($passwordName in 'MSSQL_SA_PASSWORD', 'POSTGRES_PASSWORD') {
    if ($envValues[$passwordName] -notmatch '^[A-Za-z0-9!._-]{8,64}$') {
        throw "$passwordName must contain 8-64 URI-safe characters: A-Z, a-z, 0-9, !._-"
    }
}

docker info | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw 'Docker engine is unavailable. Start Docker Desktop.'
}

if ($Stage -eq 'Full') {
    $dockerMemoryBytes = docker info --format '{{.MemTotal}}'
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to read Docker memory capacity.'
    }
    if ([long]$dockerMemoryBytes -lt 6GB) {
        Write-Warning 'Docker has less than 6 GiB of memory. The pgloader migration may be terminated by OOM.'
    }
}

if ($Stage -ne 'LabOnly') {
    $archivePath = Join-Path $labRoot 'StackOverflow2010.7z'
    $extractPath = Join-Path $labRoot 'data\sqlserver'
    $mdfPath = Join-Path $extractPath 'StackOverflow2010.mdf'
    $ldfPath = Join-Path $extractPath 'StackOverflow2010_log.ldf'

    if (-not $SkipExtraction -and -not ((Test-Path -LiteralPath $mdfPath) -and (Test-Path -LiteralPath $ldfPath))) {
        if (-not (Test-Path -LiteralPath $archivePath)) {
            throw "Archive not found: $archivePath"
        }

        Write-Step 'Extracting StackOverflow2010.7z.'
        New-Item -ItemType Directory -Path $extractPath -Force | Out-Null
        $sevenZipCommand = Get-Command 7z -ErrorAction SilentlyContinue
        if ($sevenZipCommand) {
            & $sevenZipCommand.Source x $archivePath "-o$extractPath" -y
            if ($LASTEXITCODE -ne 0) {
                throw "7-Zip extraction failed. Exit code: $LASTEXITCODE"
            }
        } else {
            docker compose --profile tools run --rm --build extractor
            if ($LASTEXITCODE -ne 0) {
                throw "Docker-based 7-Zip extraction failed. Exit code: $LASTEXITCODE"
            }
        }
    }

    if (-not ((Test-Path -LiteralPath $mdfPath) -and (Test-Path -LiteralPath $ldfPath))) {
        throw 'StackOverflow2010.mdf or StackOverflow2010_log.ldf is missing.'
    }
}

if ($Stage -eq 'LabOnly') {
    Write-Step 'Starting PostgreSQL.'
    docker compose up -d postgres
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to start the PostgreSQL container.'
    }
    Wait-Healthy 'postgres' 180
} else {
    Write-Step 'Starting PostgreSQL and SQL Server.'
    docker compose up -d postgres sqlserver
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to start database containers.'
    }
    Wait-Healthy 'postgres' 180
    Wait-Healthy 'sqlserver' 600

    Write-Step 'Preparing SQL Server source files and attaching StackOverflow2010.'
    docker compose exec -T -u root sqlserver bash /migration/copy-source-files.sh
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to copy the extracted database files into the SQL Server data volume.'
    }

    docker compose exec -T sqlserver bash -lc '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -b -i /migration/attach.sql'
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to attach the StackOverflow2010 database.'
    }
}

if ($Stage -eq 'Full') {
    Write-Step 'Checking the SQL Server source database.'
    docker compose exec -T sqlserver bash -lc '/opt/mssql-tools18/bin/sqlcmd -S localhost -U sa -P "$MSSQL_SA_PASSWORD" -C -b -i /migration/check-source.sql'
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to validate the StackOverflow2010 database.'
    }

    Write-Step 'Recreating stackoverflow_base and running pgloader.'
    docker compose exec -T postgres bash -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -f /migration/prepare-base.sql'
    if ($LASTEXITCODE -ne 0) {
        throw 'Failed to initialize the PostgreSQL base database.'
    }

    docker compose --profile tools run --rm migrator
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to migrate SQL Server data to PostgreSQL. Resume with: .\scripts\setup.ps1 -Stage Full -SkipExtraction"
    }
}

if ($Stage -ne 'LabOnly') {
    Write-Step 'Normalizing the PostgreSQL schema and validating row counts.'
    docker compose exec -T postgres bash -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d stackoverflow_base -f /migration/post-migrate.sql'
    if ($LASTEXITCODE -ne 0) {
        throw "Failed to normalize the PostgreSQL schema. Resume with: .\scripts\setup.ps1 -Stage PostMigration -SkipExtraction"
    }

    & (Join-Path $PSScriptRoot 'compare-row-counts.ps1')

    if (-not $KeepSqlServer) {
        Remove-MigrationSource
    }
}

Write-Step 'Creating and validating stackoverflow_lab.'
docker compose exec -T postgres bash -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d postgres -f /migration/prepare-lab.sql'
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to create the lab database.'
}

docker compose exec -T postgres bash -lc 'psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d stackoverflow_lab -f /migration/validate.sql'
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to validate the lab database.'
}

Write-Step 'Stack Overflow PostgreSQL lab is ready.'
