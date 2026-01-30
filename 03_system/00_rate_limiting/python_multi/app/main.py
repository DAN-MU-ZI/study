"""
Rate Limiting API - Multi Server Implementation
Global Rate Limiting for External API Dependencies
Using Lua Script for Atomic Operations (Race Condition Prevention)
With Weighted Token Consumption (different costs per request type)
"""

import os
import time
import uuid
from pathlib import Path
from fastapi import FastAPI, HTTPException
import redis

app = FastAPI(title="Rate Limiting API - Multi Server (Global Weighted)")

SERVER_ID = os.getenv("SERVER_ID", "unknown")

# Global rate limit key (not IP-based, for external API dependency scenario)
GLOBAL_KEY = "global"

# Redis connection
redis_client = redis.Redis(
    host=os.getenv("REDIS_HOST", "localhost"),
    port=int(os.getenv("REDIS_PORT", 6379)),
    decode_responses=True
)

# Rate limit configuration
TOKEN_LIMIT = 100  # total tokens per window
WINDOW_SIZE = 60   # seconds

# Token costs per operation type
TOKEN_COSTS = {
    "read": 1,
    "write": 5,
    "delete": 10,
    "search": 3,
}

# Load Lua script
LUA_SCRIPT_PATH = Path(__file__).parent / "rate_limit.lua"
with open(LUA_SCRIPT_PATH, "r") as f:
    RATE_LIMIT_SCRIPT = redis_client.register_script(f.read())


def check_rate_limit_atomic(cost: int = 1) -> tuple[bool, int]:
    """
    Check rate limit using Lua script for atomicity with weighted tokens.
    Global rate limiting - all requests share the same token bucket.
    """
    key = f"rate_limit:{GLOBAL_KEY}"
    now = time.time()
    window_start = now - WINDOW_SIZE
    request_id = f"{uuid.uuid4()}:{cost}"
    
    result = RATE_LIMIT_SCRIPT(
        keys=[key],
        args=[window_start, now, request_id, TOKEN_LIMIT, WINDOW_SIZE, cost]
    )
    
    if result >= 0:
        return True, int(result)
    else:
        return False, 0


def get_token_usage() -> tuple[int, int]:
    """Get current global token usage"""
    key = f"rate_limit:{GLOBAL_KEY}"
    now = time.time()
    window_start = now - WINDOW_SIZE
    
    redis_client.zremrangebyscore(key, 0, window_start)
    members = redis_client.zrangebyscore(key, window_start, now)
    current_usage = sum(int(m.split(":")[-1]) for m in members) if members else 0
    
    return current_usage, TOKEN_LIMIT - current_usage


@app.get("/")
async def root():
    """Health check endpoint"""
    return {
        "status": "healthy",
        "service": "rate-limit-api-global-weighted",
        "server_id": SERVER_ID,
        "rate_limit_type": "global",
        "token_costs": TOKEN_COSTS
    }


@app.get("/api/resource")
async def get_resource():
    """READ operation - consumes 1 token."""
    cost = TOKEN_COSTS["read"]
    is_allowed, remaining = check_rate_limit_atomic(cost)
    
    if not is_allowed:
        raise HTTPException(
            status_code=429,
            detail={
                "error": "Too Many Requests",
                "message": f"Token limit exceeded. You need {cost} tokens.",
                "limit": TOKEN_LIMIT,
                "window": f"{WINDOW_SIZE} seconds",
                "server_id": SERVER_ID
            }
        )
    
    return {
        "message": "GET request successful",
        "operation": "read",
        "tokens_consumed": cost,
        "tokens_remaining": remaining,
        "server_id": SERVER_ID
    }


@app.post("/api/resource")
async def create_resource():
    """WRITE operation - consumes 5 tokens."""
    cost = TOKEN_COSTS["write"]
    is_allowed, remaining = check_rate_limit_atomic(cost)
    
    if not is_allowed:
        raise HTTPException(
            status_code=429,
            detail={
                "error": "Too Many Requests",
                "message": f"Token limit exceeded. You need {cost} tokens.",
                "limit": TOKEN_LIMIT,
                "window": f"{WINDOW_SIZE} seconds",
                "server_id": SERVER_ID
            }
        )
    
    return {
        "message": "POST request successful",
        "operation": "write",
        "tokens_consumed": cost,
        "tokens_remaining": remaining,
        "server_id": SERVER_ID
    }


@app.delete("/api/resource/{resource_id}")
async def delete_resource(resource_id: str):
    """DELETE operation - consumes 10 tokens."""
    cost = TOKEN_COSTS["delete"]
    is_allowed, remaining = check_rate_limit_atomic(cost)
    
    if not is_allowed:
        raise HTTPException(
            status_code=429,
            detail={
                "error": "Too Many Requests",
                "message": f"Token limit exceeded. You need {cost} tokens.",
                "limit": TOKEN_LIMIT,
                "window": f"{WINDOW_SIZE} seconds",
                "server_id": SERVER_ID
            }
        )
    
    return {
        "message": f"DELETE request successful for resource {resource_id}",
        "operation": "delete",
        "tokens_consumed": cost,
        "tokens_remaining": remaining,
        "server_id": SERVER_ID
    }


@app.get("/api/search")
async def search_resources(q: str = ""):
    """SEARCH operation - consumes 3 tokens."""
    cost = TOKEN_COSTS["search"]
    is_allowed, remaining = check_rate_limit_atomic(cost)
    
    if not is_allowed:
        raise HTTPException(
            status_code=429,
            detail={
                "error": "Too Many Requests",
                "message": f"Token limit exceeded. You need {cost} tokens.",
                "limit": TOKEN_LIMIT,
                "window": f"{WINDOW_SIZE} seconds",
                "server_id": SERVER_ID
            }
        )
    
    return {
        "message": "Search completed",
        "operation": "search",
        "query": q,
        "tokens_consumed": cost,
        "tokens_remaining": remaining,
        "server_id": SERVER_ID
    }


@app.get("/api/stats")
async def get_stats():
    """Get current global token usage stats"""
    used, remaining = get_token_usage()
    
    return {
        "rate_limit_type": "global",
        "tokens_used": used,
        "tokens_remaining": remaining,
        "token_limit": TOKEN_LIMIT,
        "window_seconds": WINDOW_SIZE,
        "server_id": SERVER_ID,
        "token_costs": TOKEN_COSTS
    }
