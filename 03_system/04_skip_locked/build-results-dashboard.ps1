param(
    [string]$ResultsRoot = '.\results',
    [string]$OutputPath = '',
    [switch]$Help
)

$ErrorActionPreference = 'Stop'

if ($Help) {
    @'
Builds one read-only HTML dashboard from all run folders under results.

Usage:
  powershell -ExecutionPolicy Bypass -File .\build-results-dashboard.ps1

Options:
  -ResultsRoot  Directory containing yyyyMMdd-HHmmss run folders
  -OutputPath   HTML output path (default: <ResultsRoot>\dashboard.html)

The builder only reads run.json, metrics.csv, k6-summary.json, and
k6-console.log. It does not run k6 or query Prometheus.
'@
    return
}

function Get-JsonFile {
    param([string]$Path)
    if (-not (Test-Path -LiteralPath $Path)) { return $null }
    try { return Get-Content -Raw -Encoding UTF8 -LiteralPath $Path | ConvertFrom-Json }
    catch { return $null }
}

function Get-Number {
    param($Value)
    if ($null -eq $Value) { return $null }
    try { return [Convert]::ToDouble([string]$Value, [Globalization.CultureInfo]::InvariantCulture) }
    catch { return $null }
}

function Get-ThresholdFailure {
    param($Summary)
    if ($null -eq $Summary -or $null -eq $Summary.metrics) { return $false }
    foreach ($metric in $Summary.metrics.PSObject.Properties) {
        $thresholds = $metric.Value.PSObject.Properties['thresholds']
        if ($null -eq $thresholds) { continue }
        foreach ($threshold in $thresholds.Value.PSObject.Properties) {
            if ($threshold.Value -eq $true) { return $true }
        }
    }
    return $false
}

function ConvertTo-PlainObject {
    param($InputObject)
    if ($null -eq $InputObject) { return $null }
    if ($InputObject -is [string] -or $InputObject.GetType().IsValueType) { return $InputObject }
    if ($InputObject -is [Collections.IEnumerable]) {
        $items = [Collections.Generic.List[object]]::new()
        foreach ($item in $InputObject) { $items.Add((ConvertTo-PlainObject $item)) }
        return ,($items.ToArray())
    }
    $plain = [ordered]@{}
    foreach ($property in $InputObject.PSObject.Properties) {
        if ($property.MemberType -eq 'NoteProperty' -or $property.MemberType -eq 'Property') {
            $plain[$property.Name] = ConvertTo-PlainObject $property.Value
        }
    }
    return $plain
}

$root = (New-Item -ItemType Directory -Force -Path $ResultsRoot).FullName
if (-not $OutputPath) { $OutputPath = Join-Path $root 'dashboard.html' }
$outputFullPath = [IO.Path]::GetFullPath($OutputPath)

$runs = [Collections.Generic.List[object]]::new()
$directories = Get-ChildItem -Directory -LiteralPath $root | Where-Object { $_.Name -match '^\d{8}-\d{6}$' } | Sort-Object Name -Descending
foreach ($directory in $directories) {
    Write-Host "Reading:   $($directory.Name)"
    $runId = $directory.Name
    $metaPath = Join-Path $directory.FullName 'run.json'
    $metricsPath = Join-Path $directory.FullName 'metrics.csv'
    $summaryPath = Join-Path $directory.FullName 'k6-summary.json'
    $logPath = Join-Path $directory.FullName 'k6-console.log'
    $reportPath = Join-Path $directory.FullName 'report.html'

    $meta = Get-JsonFile $metaPath
    $summary = Get-JsonFile $summaryPath
    $metricRows = @()
    if (Test-Path -LiteralPath $metricsPath) {
        try { $metricRows = @(Import-Csv -LiteralPath $metricsPath -Encoding UTF8) }
        catch { $metricRows = @() }
    }

    $metricSeries = @($metricRows | ForEach-Object {
        $number = Get-Number $_.value
        if ($null -ne $number -and -not [double]::IsNaN($number) -and -not [double]::IsInfinity($number)) {
            [pscustomobject]@{
                timestamp = $_.timestamp
                metric = $_.metric
                series = $_.series
                value = $number
                unit = $_.unit
            }
        }
    })
    $metricSummaries = @()
    foreach ($group in ($metricSeries | Group-Object metric)) {
        $numericRows = @($group.Group)
        if ($numericRows.Count -eq 0) { continue }
        $measure = $numericRows.value | Measure-Object -Average -Maximum -Minimum
        $last = $numericRows | Sort-Object timestamp | Select-Object -Last 1
        $metricSummaries += [pscustomobject]@{
            name = $group.Name
            unit = $last.unit
            average = [Math]::Round($measure.Average, 6)
            maximum = [Math]::Round($measure.Maximum, 6)
            minimum = [Math]::Round($measure.Minimum, 6)
            last = [Math]::Round($last.value, 6)
            samples = $numericRows.Count
        }
    }

    $logLines = @()
    if (Test-Path -LiteralPath $logPath) {
        $logLines = @(Get-Content -Encoding UTF8 -LiteralPath $logPath | Where-Object { $_.Trim().Length -gt 0 } | Select-Object -Last 5000)
    }

    $durationMetric = if ($null -ne $summary) { $summary.metrics.http_req_duration } else { $null }
    $requestsMetric = if ($null -ne $summary) { $summary.metrics.http_reqs } else { $null }
    $failedMetric = if ($null -ne $summary) { $summary.metrics.http_req_failed } else { $null }
    $purchaseMetric = if ($null -ne $summary) { $summary.metrics.successful_purchases } else { $null }

    $exitCode = $null
    if ($null -ne $meta -and $null -ne $meta.PSObject.Properties['k6ExitCode']) {
        $exitCode = [int]$meta.k6ExitCode
    }
    elseif (Get-ThresholdFailure $summary) {
        $exitCode = 99
    }

    $startUtc = if ($null -ne $meta) { $meta.startUtc } else { $null }
    $endUtc = if ($null -ne $meta) { $meta.endUtc } else { $null }
    $durationSeconds = if ($null -ne $meta) { Get-Number $meta.durationSeconds } else { $null }
    $complete = (Test-Path -LiteralPath $metaPath) -and (Test-Path -LiteralPath $metricsPath)

    $runs.Add([pscustomobject]@{
        id = $runId
        complete = $complete
        startUtc = $startUtc
        endUtc = $endUtc
        durationSeconds = $durationSeconds
        exitCode = $exitCode
        status = if (-not $complete) { 'partial' } elseif ($exitCode -eq 0) { 'passed' } else { 'failed' }
        sampleCount = $metricRows.Count
        metricSummaries = $metricSummaries
        metricSeries = $metricSeries
        logLines = $logLines
        hasReport = Test-Path -LiteralPath $reportPath
        files = @(
            if (Test-Path -LiteralPath $metaPath) { 'run.json' }
            if (Test-Path -LiteralPath $metricsPath) { 'metrics.csv' }
            if (Test-Path -LiteralPath $summaryPath) { 'k6-summary.json' }
            if (Test-Path -LiteralPath $logPath) { 'k6-console.log' }
            if (Test-Path -LiteralPath $reportPath) { 'report.html' }
        )
        k6 = [pscustomobject]@{
            requests = if ($null -ne $requestsMetric) { Get-Number $requestsMetric.count } else { $null }
            rps = if ($null -ne $requestsMetric) { Get-Number $requestsMetric.rate } else { $null }
            averageMs = if ($null -ne $durationMetric) { Get-Number $durationMetric.avg } else { $null }
            p95Ms = if ($null -ne $durationMetric) { Get-Number $durationMetric.'p(95)' } else { $null }
            maximumMs = if ($null -ne $durationMetric) { Get-Number $durationMetric.max } else { $null }
            failedRate = if ($null -ne $failedMetric) { Get-Number $failedMetric.value } else { $null }
            successfulPurchases = if ($null -ne $purchaseMetric) { Get-Number $purchaseMetric.count } else { $null }
        }
    })
}

Write-Host 'Encoding dashboard data...'
if ($runs.Count -eq 0) {
    $runsJson = '[]'
}
else {
    $plainRuns = ConvertTo-PlainObject ($runs.ToArray())
    $runsJson = ConvertTo-Json -InputObject $plainRuns -Depth 6 -Compress
}
$runsJson = $runsJson.Replace('<', '\u003c')

$template = @'
<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<title>Results dashboard</title>
<style>
:root{color-scheme:dark;--bg:#0b1220;--panel:#111b2e;--panel2:#0d1728;--line:#263755;--text:#e7edf7;--muted:#91a2bd;--accent:#62a8ff;--ok:#51d6a0;--bad:#ff7b8e;--warn:#ffbd69}*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--text);font:14px/1.45 system-ui,sans-serif}main{max-width:none;margin:auto;padding:12px}h1,h2{margin:0}h1{font-size:24px}h2{font-size:17px}.sub{color:var(--muted);margin-top:5px}.cards{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:12px;margin:20px 0}.card,.panel{background:var(--panel);border:1px solid var(--line);border-radius:8px;padding:14px}.card span{color:var(--muted)}.card strong{display:block;font-size:22px;margin-top:5px}.tabs{display:flex;gap:4px;border-bottom:1px solid var(--line);margin-bottom:16px}.tabs button{border:0;border-bottom:2px solid transparent;background:transparent;color:var(--muted);padding:10px 14px;cursor:pointer;font:inherit}.tabs button[aria-selected=true]{color:var(--text);border-color:var(--accent)}[role=tabpanel][hidden]{display:none}.toolbar{display:flex;flex-wrap:wrap;align-items:center;gap:8px;margin-bottom:12px}input,select{min-height:36px;background:#09111f;color:var(--text);border:1px solid var(--line);border-radius:6px;padding:7px 9px;font:inherit}input{width:340px;max-width:100%}.table-wrap{max-height:65vh;overflow:auto;border:1px solid var(--line);border-radius:6px}table{width:100%;border-collapse:collapse;font-variant-numeric:tabular-nums}th,td{text-align:left;padding:8px 10px;border-bottom:1px solid var(--line);vertical-align:top}th{position:sticky;top:0;background:var(--panel2);color:var(--muted);z-index:1}tbody tr{cursor:pointer}tbody tr:hover{background:#16233a}.tag{display:inline-block;border:1px solid var(--line);border-radius:999px;padding:2px 8px}.passed{color:var(--ok)}.failed{color:var(--bad)}.partial{color:var(--warn)}.split{display:grid;grid-template-columns:minmax(0,2fr) minmax(300px,1fr);gap:12px}.detail-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px;margin-top:12px}.detail-grid div{background:var(--panel2);padding:10px;border-radius:6px}.detail-grid span{display:block;color:var(--muted)}.links a{color:var(--accent);margin-right:10px}.chart-wrap{height:420px;position:relative}.run-metrics{margin-top:12px}.metric-detail-grid{display:grid;grid-template-columns:minmax(0,3fr) minmax(520px,2fr);gap:12px}.metric-detail-grid table{table-layout:fixed;font-size:12px}.metric-detail-grid th,.metric-detail-grid td{padding:7px 6px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}.metric-detail-grid th:first-child,.metric-detail-grid td:first-child{width:28%}.metric-detail-grid th:last-child,.metric-detail-grid td:last-child{width:8%}.metric-detail-grid .table-wrap{max-height:420px;overflow-y:auto;overflow-x:hidden}.sample-table{margin-top:12px}.message{white-space:pre-wrap;word-break:break-word;font:12px/1.4 ui-monospace,Consolas,monospace}.empty{padding:32px;text-align:center;color:var(--muted)}@media(max-width:850px){.cards{grid-template-columns:repeat(2,minmax(0,1fr))}.split,.metric-detail-grid{grid-template-columns:1fr}}
</style></head><body><main>
<h1>Results dashboard</h1><div class="sub">Read-only view of generated k6 and Prometheus result files</div>
<div class="cards" id="cards"></div>
<nav class="tabs" role="tablist"><button role="tab" aria-selected="true" data-view="runs">Runs</button><button role="tab" aria-selected="false" data-view="compare">Compare metrics</button><button role="tab" aria-selected="false" data-view="logs">All logs</button></nav>
<section id="runs" role="tabpanel"><div class="split"><div class="panel"><div class="toolbar"><input id="run-search" type="search" placeholder="Search run id"><select id="run-status"><option value="all">All statuses</option><option value="passed">Passed</option><option value="failed">Failed</option><option value="partial">Partial</option></select></div><div class="table-wrap"><table><thead><tr><th>Run</th><th>Status</th><th>RPS</th><th>p95</th><th>Samples</th></tr></thead><tbody id="run-rows"></tbody></table></div></div><aside class="panel" id="detail"></aside></div><div class="panel run-metrics"><div class="toolbar"><h2>Selected run metrics</h2><label for="detail-metric-select">Metric</label><select id="detail-metric-select"></select></div><div class="metric-detail-grid"><div class="chart-wrap"><canvas id="detail-chart"></canvas></div><div class="table-wrap"><table><thead><tr><th>Metric</th><th>Avg</th><th>Min</th><th>Max</th><th>Last</th><th>N</th></tr></thead><tbody id="detail-summary-rows"></tbody></table></div></div><div class="table-wrap sample-table"><table><thead><tr><th>Time</th><th>Series</th><th>Value</th><th>Unit</th></tr></thead><tbody id="detail-sample-rows"></tbody></table></div></div></section>
<section id="compare" role="tabpanel" hidden><div class="panel"><div class="toolbar"><label for="metric-select">Prometheus metric</label><select id="metric-select"></select></div><div class="chart-wrap"><canvas id="comparison-chart"></canvas></div></div></section>
<section id="logs" role="tabpanel" hidden><div class="panel"><div class="toolbar"><input id="log-search" type="search" placeholder="Search all k6 logs"><select id="log-run"><option value="all">All runs</option></select><select id="log-level"><option value="all">All types</option><option value="error">Error</option><option value="info">Info</option></select></div><div class="sub" id="log-count"></div><div class="table-wrap"><table><thead><tr><th>Run</th><th>Type</th><th>Message</th></tr></thead><tbody id="log-rows"></tbody></table></div></div></section>
</main><script src="https://cdn.jsdelivr.net/npm/chart.js@4.5.1/dist/chart.umd.min.js"></script><script>
const runs=__RUNS__;
const cards=document.querySelector('#cards');const passed=runs.filter(r=>r.status==='passed').length,failed=runs.filter(r=>r.status==='failed').length,partial=runs.filter(r=>r.status==='partial').length;
for(const [label,value] of [['Runs',runs.length],['Passed',passed],['Failed',failed],['Partial',partial]]){const card=document.createElement('div');card.className='card';const s=document.createElement('span');s.textContent=label;const strong=document.createElement('strong');strong.textContent=value;card.append(s,strong);cards.append(card)}
const tabs=[...document.querySelectorAll('[role=tab]')];for(const tab of tabs)tab.addEventListener('click',()=>{for(const item of tabs){const active=item===tab;item.setAttribute('aria-selected',active);document.querySelector('#'+item.dataset.view).hidden=!active}if(tab.dataset.view==='compare')renderChart()});
const fmt=(value,digits=2)=>value==null?'-':Number(value).toFixed(digits);const runBody=document.querySelector('#run-rows'),runSearch=document.querySelector('#run-search'),runStatus=document.querySelector('#run-status'),detail=document.querySelector('#detail');let selected=runs[0]||null;
function renderRuns(){const q=runSearch.value.toLowerCase(),status=runStatus.value;const filtered=runs.filter(r=>r.id.toLowerCase().includes(q)&&(status==='all'||r.status===status));runBody.replaceChildren(...filtered.map(run=>{const tr=document.createElement('tr');for(const value of [run.id,run.status,fmt(run.k6.rps),fmt(run.k6.p95Ms)+' ms',run.sampleCount]){const td=document.createElement('td');td.textContent=value;if(value===run.status)td.className=run.status;tr.append(td)}tr.addEventListener('click',()=>{selected=run;renderDetail();renderSelectedMetrics()});return tr}));if(!filtered.length){const tr=document.createElement('tr'),td=document.createElement('td');td.colSpan=5;td.className='empty';td.textContent='No matching runs.';tr.append(td);runBody.replaceChildren(tr)}}
function renderDetail(){if(!selected){detail.innerHTML='<h2>No runs</h2>';return}detail.replaceChildren();const h=document.createElement('h2');h.textContent=selected.id;const status=document.createElement('span');status.className='tag '+selected.status;status.textContent=selected.status;const grid=document.createElement('div');grid.className='detail-grid';for(const [label,value] of [['Requests',fmt(selected.k6.requests,0)],['RPS',fmt(selected.k6.rps)],['Average',fmt(selected.k6.averageMs)+' ms'],['p95',fmt(selected.k6.p95Ms)+' ms'],['Maximum',fmt(selected.k6.maximumMs)+' ms'],['Purchases',fmt(selected.k6.successfulPurchases,0)]]){const box=document.createElement('div'),s=document.createElement('span'),strong=document.createElement('strong');s.textContent=label;strong.textContent=value;box.append(s,strong);grid.append(box)}const links=document.createElement('p');links.className='links';for(const file of selected.files){const a=document.createElement('a');a.href=`${selected.id}/${file}`;a.textContent=file;links.append(a)}detail.append(h,status,grid,links)}
const detailMetricSelect=document.querySelector('#detail-metric-select'),detailSummaryBody=document.querySelector('#detail-summary-rows'),detailSampleBody=document.querySelector('#detail-sample-rows');let detailChart=null;
function renderSelectedMetrics(){if(detailChart){detailChart.destroy();detailChart=null}detailMetricSelect.replaceChildren();detailSummaryBody.replaceChildren();detailSampleBody.replaceChildren();if(!selected||!selected.metricSummaries.length){const tr=document.createElement('tr'),td=document.createElement('td');td.colSpan=6;td.className='empty';td.textContent='No Prometheus samples for this run.';tr.append(td);detailSummaryBody.append(tr);return}for(const metric of selected.metricSummaries){const option=document.createElement('option');option.value=metric.name;option.textContent=metric.name;detailMetricSelect.append(option);const tr=document.createElement('tr');for(const value of [metric.name,fmt(metric.average,3),fmt(metric.minimum,3),fmt(metric.maximum,3),fmt(metric.last,3),metric.samples]){const td=document.createElement('td');td.textContent=value;tr.append(td)}tr.addEventListener('click',()=>{detailMetricSelect.value=metric.name;renderSelectedMetricSeries()});detailSummaryBody.append(tr)}renderSelectedMetricSeries()}
function renderSelectedMetricSeries(){if(detailChart){detailChart.destroy();detailChart=null}if(!selected||!detailMetricSelect.value)return;const samples=selected.metricSeries.filter(row=>row.metric===detailMetricSelect.value),labels=[...new Set(samples.map(row=>row.timestamp))].sort(),series=[...new Set(samples.map(row=>row.series))],unit=samples[0]?.unit||'value',colors=['#62a8ff','#51d6a0','#ffbd69','#df77ff','#ff7b8e','#67d8ef'];const datasets=series.map((name,index)=>{const values=new Map(samples.filter(row=>row.series===name).map(row=>[row.timestamp,row.value]));return{label:name,data:labels.map(time=>values.get(time)??null),borderColor:colors[index%colors.length],backgroundColor:colors[index%colors.length],pointRadius:1,tension:0,spanGaps:true}});if(typeof Chart!=='undefined')detailChart=new Chart(document.querySelector('#detail-chart'),{type:'line',data:{labels,datasets},options:{responsive:true,maintainAspectRatio:false,animation:false,interaction:{mode:'index',intersect:false},plugins:{legend:{labels:{color:'#e7edf7'}}},scales:{x:{ticks:{color:'#91a2bd',maxTicksLimit:8,callback:function(value){return new Date(this.getLabelForValue(value)).toLocaleTimeString()}},grid:{color:'#263755'}},y:{beginAtZero:true,title:{display:true,text:unit,color:'#91a2bd'},ticks:{color:'#91a2bd'},grid:{color:'#263755'}}}}});detailSampleBody.replaceChildren(...samples.slice(0,5000).map(sample=>{const tr=document.createElement('tr');for(const value of [new Date(sample.timestamp).toLocaleString(),sample.series,fmt(sample.value,6),sample.unit]){const td=document.createElement('td');td.textContent=value;tr.append(td)}return tr}))}
detailMetricSelect.addEventListener('input',renderSelectedMetricSeries);runSearch.addEventListener('input',renderRuns);runStatus.addEventListener('input',renderRuns);renderRuns();renderDetail();renderSelectedMetrics();
const metricSelect=document.querySelector('#metric-select');const metricNames=[...new Set(runs.flatMap(r=>r.metricSummaries.map(m=>m.name)))].sort();for(const name of metricNames){const option=document.createElement('option');option.value=name;option.textContent=name;metricSelect.append(option)}let chart=null;function renderChart(){if(typeof Chart==='undefined'||!metricSelect.value)return;const name=metricSelect.value,labels=runs.map(r=>r.id).reverse(),lookup=id=>runs.find(r=>r.id===id).metricSummaries.find(m=>m.name===name);const averages=labels.map(id=>lookup(id)?.average??null),maximums=labels.map(id=>lookup(id)?.maximum??null),unit=runs.map(run=>run.metricSummaries.find(m=>m.name===name)?.unit).find(Boolean)||'value';if(chart)chart.destroy();chart=new Chart(document.querySelector('#comparison-chart'),{type:'line',data:{labels,datasets:[{label:'average',data:averages,borderColor:'#62a8ff',backgroundColor:'#62a8ff',tension:0,spanGaps:true},{label:'maximum',data:maximums,borderColor:'#ffbd69',backgroundColor:'#ffbd69',tension:0,spanGaps:true}]},options:{responsive:true,maintainAspectRatio:false,plugins:{legend:{labels:{color:'#e7edf7'}}},scales:{x:{ticks:{color:'#91a2bd'},grid:{color:'#263755'}},y:{beginAtZero:true,title:{display:true,text:unit,color:'#91a2bd'},ticks:{color:'#91a2bd'},grid:{color:'#263755'}}}}})}metricSelect.addEventListener('input',renderChart);
const allLogs=runs.flatMap(run=>run.logLines.map(line=>({run:run.id,level:/error|failed|crossed/i.test(line)?'error':'info',message:line.replace(/\x1b\[[0-9;?]*[ -\/]*[@-~]/g,'')})));const logRun=document.querySelector('#log-run');for(const run of runs){const option=document.createElement('option');option.value=run.id;option.textContent=run.id;logRun.append(option)}const logSearch=document.querySelector('#log-search'),logLevel=document.querySelector('#log-level'),logBody=document.querySelector('#log-rows'),logCount=document.querySelector('#log-count');function renderLogs(){const q=logSearch.value.toLowerCase(),run=logRun.value,level=logLevel.value;const filtered=allLogs.filter(row=>(run==='all'||row.run===run)&&(level==='all'||row.level===level)&&row.message.toLowerCase().includes(q));logCount.textContent=`${filtered.length.toLocaleString()} / ${allLogs.length.toLocaleString()} lines`;logBody.replaceChildren(...filtered.slice(0,10000).map(row=>{const tr=document.createElement('tr'),id=document.createElement('td'),type=document.createElement('td'),message=document.createElement('td');id.textContent=row.run;type.textContent=row.level;type.className=row.level==='error'?'failed':'';message.textContent=row.message;message.className='message';tr.append(id,type,message);return tr}))}for(const control of [logSearch,logRun,logLevel])control.addEventListener('input',renderLogs);renderLogs();
</script></body></html>
'@

$html = $template.Replace('__RUNS__', $runsJson)
Write-Host 'Writing dashboard HTML...'
[IO.File]::WriteAllText($outputFullPath, $html, [Text.UTF8Encoding]::new($false))
Write-Host "Dashboard: $outputFullPath"
Write-Host "Runs:      $($runs.Count)"
