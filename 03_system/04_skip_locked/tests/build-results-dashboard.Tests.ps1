$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$builder = Join-Path $repoRoot 'build-results-dashboard.ps1'
if (-not (Test-Path -LiteralPath $builder)) {
    throw "Dashboard builder is missing: $builder"
}

$syntaxErrors = $null
[void][Management.Automation.Language.Parser]::ParseFile($builder, [ref]$null, [ref]$syntaxErrors)
if ($syntaxErrors.Count -gt 0) {
    throw ($syntaxErrors | ForEach-Object Message | Out-String)
}

$tempRoot = Join-Path ([IO.Path]::GetTempPath()) "results-dashboard-$([guid]::NewGuid().ToString('N'))"
$completeRun = Join-Path $tempRoot '20260719-120000'
$partialRun = Join-Path $tempRoot '20260719-130000'
$dashboard = Join-Path $tempRoot 'dashboard.html'
$inlineJs = Join-Path $tempRoot 'dashboard.js'

try {
    [void](New-Item -ItemType Directory -Force $completeRun, $partialRun)
    '{"runId":"20260719-120000","startUtc":"2026-07-19T03:00:00Z","endUtc":"2026-07-19T03:03:00Z","durationSeconds":180,"sampleCount":2,"k6ExitCode":0}' | Set-Content -Encoding UTF8 (Join-Path $completeRun 'run.json')
    '{"metrics":{"http_reqs":{"count":100,"rate":10},"http_req_duration":{"avg":100,"p(95)":200,"max":300},"http_req_failed":{"value":0},"successful_purchases":{"count":100}}}' | Set-Content -Encoding UTF8 (Join-Path $completeRun 'k6-summary.json')
    @'
"timestamp","metric","series","value","unit"
"2026-07-19T03:00:00Z","api_rps","total","10","req/s"
"2026-07-19T03:00:05Z","api_rps","total","20","req/s"
'@ | Set-Content -Encoding UTF8 (Join-Path $completeRun 'metrics.csv')
    "test started`ntest passed" | Set-Content -Encoding UTF8 (Join-Path $completeRun 'k6-console.log')

    '{"metrics":{"http_reqs":{"count":50,"rate":5},"http_req_duration":{"avg":400,"p(95)":900,"max":1200,"thresholds":{"p(95)<500":true}}}}' | Set-Content -Encoding UTF8 (Join-Path $partialRun 'k6-summary.json')
    'thresholds crossed' | Set-Content -Encoding UTF8 (Join-Path $partialRun 'k6-console.log')

    & $builder -ResultsRoot $tempRoot -OutputPath $dashboard
    if (-not $?) { throw 'Dashboard builder failed.' }

    $html = Get-Content -Raw -Encoding UTF8 $dashboard
    foreach ($expected in @(
        '20260719-120000',
        '20260719-130000',
        'Results dashboard',
        'api_rps',
        'thresholds crossed',
        'chart.js@4.5.1',
        'metricSeries',
        'id="detail-metric-select"',
        'id="detail-sample-rows"',
        'Selected run metrics',
        '2026-07-19T03:00:05Z',
        "title:{display:true,text:unit,color:'#91a2bd'}",
        "unit=runs.map(run=>run.metricSummaries.find(m=>m.name===name)?.unit).find(Boolean)||'value'",
        'main{max-width:none;margin:auto;padding:12px}',
        '.metric-detail-grid{display:grid;grid-template-columns:minmax(0,3fr) minmax(520px,2fr);gap:12px}',
        '.metric-detail-grid table{table-layout:fixed;font-size:12px}',
        '.metric-detail-grid .table-wrap{max-height:420px;overflow-y:auto;overflow-x:hidden}'
    )) {
        if ($html -notmatch [regex]::Escape($expected)) {
            throw "Dashboard is missing '$expected'."
        }
    }

    $scripts = [regex]::Matches($html, '<script(?:\s[^>]*)?>([\s\S]*?)</script>')
    [IO.File]::WriteAllText($inlineJs, $scripts[1].Groups[1].Value, [Text.UTF8Encoding]::new($false))
    & node --check $inlineJs
    if ($LASTEXITCODE -ne 0) { throw 'Dashboard JavaScript has a syntax error.' }
}
finally {
    Remove-Item -LiteralPath $tempRoot -Recurse -Force -ErrorAction SilentlyContinue
}

Write-Host 'PASS: results dashboard aggregates complete and partial runs.'
