# Global Weighted Rate Limiting - Unified Test Script
# Tests all implementations with mixed request types

param(
    [int]$Port = 8080,
    [string]$HostName = "localhost"
)

$BaseUrl = "http://${HostName}:${Port}"
$total = 0

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "     Global Weighted Rate Limiting Test - 100 tokens        " -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "Target: $BaseUrl"
Write-Host ""

# Health check
Write-Host "[1] Health Check" -ForegroundColor Yellow
$health = curl.exe -s "$BaseUrl/" | ConvertFrom-Json
Write-Host "    Status: $($health.status), Service: $($health.service)"
if ($health.server_id) { Write-Host "    Server: $($health.server_id)" }
Write-Host ""

# 5x GET - 1 token each = 5 total
Write-Host "[2] GET requests - 1 token each" -ForegroundColor Yellow
for ($i = 1; $i -le 5; $i++) {
    $r = curl.exe -s "$BaseUrl/api/resource" | ConvertFrom-Json
    $total += 1
    $serverInfo = ""
    if ($r.server_id) { $serverInfo = ", server: $($r.server_id)" }
    Write-Host "    GET ${i}: remaining=$($r.tokens_remaining)$serverInfo (total: $total)"
}

# 5x POST - 5 tokens each = 25 total
Write-Host ""
Write-Host "[3] POST requests - 5 tokens each" -ForegroundColor Yellow
for ($i = 1; $i -le 5; $i++) {
    $r = curl.exe -s -X POST "$BaseUrl/api/resource" | ConvertFrom-Json
    $total += 5
    $serverInfo = ""
    if ($r.server_id) { $serverInfo = ", server: $($r.server_id)" }
    Write-Host "    POST ${i}: remaining=$($r.tokens_remaining)$serverInfo (total: $total)"
}

# 5x SEARCH - 3 tokens each = 15 total
Write-Host ""
Write-Host "[4] SEARCH requests - 3 tokens each" -ForegroundColor Yellow
for ($i = 1; $i -le 5; $i++) {
    $r = curl.exe -s "$BaseUrl/api/search?q=test$i" | ConvertFrom-Json
    $total += 3
    $serverInfo = ""
    if ($r.server_id) { $serverInfo = ", server: $($r.server_id)" }
    Write-Host "    SEARCH ${i}: remaining=$($r.tokens_remaining)$serverInfo (total: $total)"
}

# 5x DELETE - 10 tokens each = 50 total
Write-Host ""
Write-Host "[5] DELETE requests - 10 tokens each" -ForegroundColor Yellow
for ($i = 1; $i -le 5; $i++) {
    $r = curl.exe -s -X DELETE "$BaseUrl/api/resource/$i" | ConvertFrom-Json
    $total += 10
    $serverInfo = ""
    if ($r.server_id) { $serverInfo = ", server: $($r.server_id)" }
    Write-Host "    DELETE ${i}: remaining=$($r.tokens_remaining)$serverInfo (total: $total)"
}

# Final POST to reach 100 tokens
Write-Host ""
Write-Host "[6] Final POST to reach limit - 5 tokens" -ForegroundColor Yellow
$r = curl.exe -s -X POST "$BaseUrl/api/resource" | ConvertFrom-Json
$total += 5
$serverInfo = ""
if ($r.server_id) { $serverInfo = ", server: $($r.server_id)" }
Write-Host "    POST 6: remaining=$($r.tokens_remaining)$serverInfo (total: $total)"

# Exceeded test
Write-Host ""
Write-Host "[7] EXCEEDED TEST - GET 1 token" -ForegroundColor Red
$r = curl.exe -s "$BaseUrl/api/resource"
Write-Host "    Response: $r"

# Stats
Write-Host ""
Write-Host "[8] Final Stats" -ForegroundColor Yellow
curl.exe -s "$BaseUrl/api/stats" | ConvertFrom-Json | ConvertTo-Json -Depth 3

Write-Host ""
Write-Host "============================================================" -ForegroundColor Green
Write-Host " Test Complete! Total tokens consumed: $total" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Green
