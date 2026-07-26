$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$scriptPath = Join-Path $repoRoot 'run-k6-with-metrics.ps1'

if (-not (Test-Path -LiteralPath $scriptPath)) {
    throw "Collector script is missing: $scriptPath"
}

$syntaxErrors = $null
[void][System.Management.Automation.Language.Parser]::ParseFile(
    $scriptPath,
    [ref]$null,
    [ref]$syntaxErrors
)

if ($syntaxErrors.Count -gt 0) {
    throw ($syntaxErrors | ForEach-Object Message | Out-String)
}

$help = & $scriptPath -Help 2>&1 | Out-String
if (-not $?) {
    throw "-Help failed.`n$help"
}

foreach ($expected in @('metrics.csv', 'report.html', 'k6-summary.json')) {
    if ($help -notmatch [regex]::Escape($expected)) {
        throw "Help does not describe '$expected'."
    }
}

$source = Get-Content -Raw -LiteralPath $scriptPath
if ($source -match '[^\x00-\x7F]') {
    throw 'Collector script must remain ASCII-compatible for Windows PowerShell 5.1.'
}
foreach ($expected in @(
    'chart.js@4.5.1',
    'new Chart(',
    'data-view="overview"',
    'data-view="metrics"',
    'data-view="logs"',
    'id="log-source"',
    'Integrated logs',
    'const unifiedLogs=',
    'build-results-dashboard.ps1'
)) {
    if ($source -notmatch [regex]::Escape($expected)) {
        throw "Generated report does not use expected Chart.js pattern '$expected'."
    }
}

. $scriptPath -TestFunctionsOnly
$nativeLog = Join-Path ([IO.Path]::GetTempPath()) "k6-native-$([guid]::NewGuid().ToString('N')).log"
$dashboardHtml = Join-Path ([IO.Path]::GetTempPath()) "k6-dashboard-$([guid]::NewGuid().ToString('N')).html"
$dashboardJs = Join-Path ([IO.Path]::GetTempPath()) "k6-dashboard-$([guid]::NewGuid().ToString('N')).js"
try {
    $nativeResult = Invoke-NativeProcess `
        -FilePath (Get-Command cmd.exe).Source `
        -Arguments '/d /c "echo thresholds crossed 1>&2 & exit /b 99"' `
        -LogPath $nativeLog

    if ($nativeResult.ExitCode -ne 99) {
        throw "Expected native exit code 99, got $($nativeResult.ExitCode)."
    }
    if ((Get-Content -Raw -LiteralPath $nativeLog) -notmatch 'thresholds crossed') {
        throw 'Native stderr was not preserved in the log.'
    }

    $sampleRows = @([pscustomobject]@{
        timestamp = '2026-07-19T11:00:00.0000000Z'
        metric = 'api_p95'
        series = 'total'
        value = '0.125'
        unit = 'seconds'
    })
    $sampleMetadata = @{
        startUtc = '2026-07-19T11:00:00.0000000Z'
        endUtc = '2026-07-19T11:03:00.0000000Z'
        durationSeconds = 180
        sampleCount = 1
        k6ExitCode = 99
    }
    New-HtmlReport -Rows $sampleRows -K6Log 'thresholds crossed' -Metadata $sampleMetadata -OutputPath $dashboardHtml
    $dashboard = Get-Content -Raw -Encoding UTF8 -LiteralPath $dashboardHtml
    foreach ($expected in @('api_p95', 'thresholds crossed', 'Prometheus + k6', 'FAILED')) {
        if ($dashboard -notmatch [regex]::Escape($expected)) {
            throw "Generated dashboard is missing '$expected'."
        }
    }
    $inlineScripts = [regex]::Matches($dashboard, '<script(?:\s[^>]*)?>([\s\S]*?)</script>')
    [IO.File]::WriteAllText($dashboardJs, $inlineScripts[1].Groups[1].Value, [Text.UTF8Encoding]::new($false))
    & node --check $dashboardJs
    if ($LASTEXITCODE -ne 0) {
        throw 'Generated dashboard JavaScript has a syntax error.'
    }
}
finally {
    Remove-Item -LiteralPath $nativeLog -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $dashboardHtml -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $dashboardJs -Force -ErrorAction SilentlyContinue
}

Write-Host 'PASS: PowerShell syntax and help contract are valid.'
