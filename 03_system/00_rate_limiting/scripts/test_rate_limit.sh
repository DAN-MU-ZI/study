#!/bin/bash
# Global Weighted Rate Limiting - Unified Test Script
# Tests all implementations with mixed request types

PORT="${1:-8080}"
HOST="${2:-localhost}"
BASE_URL="http://${HOST}:${PORT}"
total=0

echo "╔════════════════════════════════════════════════════════════╗"
echo "║     Global Weighted Rate Limiting Test (100 tokens)        ║"
echo "╚════════════════════════════════════════════════════════════╝"
echo "Target: $BASE_URL"
echo ""

# Health check
echo "[1] Health Check"
curl -s "$BASE_URL/" | jq .
echo ""

# 5x GET (1 token each = 5 total)
echo "[2] GET requests (1 token each)"
for i in {1..5}; do
    response=$(curl -s "$BASE_URL/api/resource")
    remaining=$(echo $response | jq -r '.tokens_remaining // "error"')
    server=$(echo $response | jq -r '.server_id // empty')
    total=$((total + 1))
    server_str=""
    [ -n "$server" ] && server_str=", server: $server"
    echo "    GET #$i: remaining=$remaining$server_str (total: $total)"
done

# 5x POST (5 tokens each = 25 total)
echo ""
echo "[3] POST requests (5 tokens each)"
for i in {1..5}; do
    response=$(curl -s -X POST "$BASE_URL/api/resource")
    remaining=$(echo $response | jq -r '.tokens_remaining // "error"')
    server=$(echo $response | jq -r '.server_id // empty')
    total=$((total + 5))
    server_str=""
    [ -n "$server" ] && server_str=", server: $server"
    echo "    POST #$i: remaining=$remaining$server_str (total: $total)"
done

# 5x SEARCH (3 tokens each = 15 total)
echo ""
echo "[4] SEARCH requests (3 tokens each)"
for i in {1..5}; do
    response=$(curl -s "$BASE_URL/api/search?q=test$i")
    remaining=$(echo $response | jq -r '.tokens_remaining // "error"')
    server=$(echo $response | jq -r '.server_id // empty')
    total=$((total + 3))
    server_str=""
    [ -n "$server" ] && server_str=", server: $server"
    echo "    SEARCH #$i: remaining=$remaining$server_str (total: $total)"
done

# 5x DELETE (10 tokens each = 50 total)
echo ""
echo "[5] DELETE requests (10 tokens each)"
for i in {1..5}; do
    response=$(curl -s -X DELETE "$BASE_URL/api/resource/$i")
    remaining=$(echo $response | jq -r '.tokens_remaining // "error"')
    server=$(echo $response | jq -r '.server_id // empty')
    total=$((total + 10))
    server_str=""
    [ -n "$server" ] && server_str=", server: $server"
    echo "    DELETE #$i: remaining=$remaining$server_str (total: $total)"
done

# Final POST to reach 100 tokens
echo ""
echo "[6] Final POST to reach limit (5 tokens)"
response=$(curl -s -X POST "$BASE_URL/api/resource")
remaining=$(echo $response | jq -r '.tokens_remaining // "error"')
server=$(echo $response | jq -r '.server_id // empty')
total=$((total + 5))
server_str=""
[ -n "$server" ] && server_str=", server: $server"
echo "    POST #6: remaining=$remaining$server_str (total: $total)"

# Exceeded test
echo ""
echo "[7] EXCEEDED TEST - GET (1 token)"
response=$(curl -s "$BASE_URL/api/resource")
echo "    Response: $response"

# Stats
echo ""
echo "[8] Final Stats"
curl -s "$BASE_URL/api/stats" | jq .

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo " Test Complete! Total tokens consumed: $total"
echo "═══════════════════════════════════════════════════════════════"
