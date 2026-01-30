"""
Rate Limiting API - Single Server Implementation
Global Rate Limiting for External API Dependencies
Using Redis Sorted Set (ZSET) for Sliding Window Algorithm
With Weighted Token Consumption (different costs per request type)
"""

import os
import time
import uuid
from fastapi import FastAPI, Request, HTTPException
from fastapi.responses import JSONResponse
import redis

app = FastAPI(title="Rate Limiting API - Global Weighted")

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
    "read": 1,      # GET requests - lightweight
    "write": 5,     # POST/PUT requests - moderate
    "delete": 10,   # DELETE requests - heavy
    "search": 3,    # Search operations - moderate
}


# Global rate limiting - no per-client identification
# All requests share the same token bucket


def check_rate_limit(cost: int = 1) -> tuple[bool, int]:
    """
    Check if request is allowed using Sliding Window algorithm with weighted tokens.
    Global rate limiting - all requests share the same token bucket.
    
    Args:
        cost: Number of tokens this request will consume
    
    Returns:
        tuple: (is_allowed, remaining_tokens)
    """
    key = f"rate_limit:{GLOBAL_KEY}"
    now = time.time()
    window_start = now - WINDOW_SIZE
    
    # Remove expired entries (older than window)
    redis_client.zremrangebyscore(key, 0, window_start)
    
    # Calculate current token usage (sum of all token costs in window)
    # Each entry stores: score=timestamp, member="{request_id}:{cost}"
    members = redis_client.zrangebyscore(key, window_start, now)
    current_usage = sum(int(m.split(":")[-1]) for m in members) if members else 0
    
    if current_usage + cost <= TOKEN_LIMIT:
        # Add new request with cost encoded in member
        request_id = f"{uuid.uuid4()}:{cost}"
        redis_client.zadd(key, {request_id: now})
        redis_client.expire(key, WINDOW_SIZE + 1)
        return True, TOKEN_LIMIT - current_usage - cost
    
    return False, max(0, TOKEN_LIMIT - current_usage)


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
        "service": "rate-limit-api-weighted",
        "token_costs": TOKEN_COSTS
    }


@app.get("/api/resource")
async def get_resource():
    """
    READ operation - consumes 1 token.
    Lightweight read operation.
    """
    cost = TOKEN_COSTS["read"]
    is_allowed, remaining = check_rate_limit(cost)
    
    if not is_allowed:
        raise HTTPException(
            status_code=429,
            detail={
                "error": "Too Many Requests",
                "message": f"Token limit exceeded. You need {cost} tokens.",
                "tokens_remaining": remaining,
                "limit": TOKEN_LIMIT,
                "window": f"{WINDOW_SIZE} seconds"
            }
        )
    
    return {
        "message": "GET request successful",
        "operation": "read",
        "tokens_consumed": cost,
        "tokens_remaining": remaining
    }


@app.post("/api/resource")
async def create_resource():
    """
    WRITE operation - consumes 5 tokens.
    Moderate write operation (create).
    """
    cost = TOKEN_COSTS["write"]
    is_allowed, remaining = check_rate_limit(cost)
    
    if not is_allowed:
        raise HTTPException(
            status_code=429,
            detail={
                "error": "Too Many Requests",
                "message": f"Token limit exceeded. You need {cost} tokens.",
                "tokens_remaining": remaining,
                "limit": TOKEN_LIMIT,
                "window": f"{WINDOW_SIZE} seconds"
            }
        )
    
    return {
        "message": "POST request successful",
        "operation": "write",
        "tokens_consumed": cost,
        "tokens_remaining": remaining
    }


@app.put("/api/resource/{resource_id}")
async def update_resource(resource_id: str):
    """
    WRITE operation - consumes 5 tokens.
    Moderate write operation (update).
    """
    cost = TOKEN_COSTS["write"]
    is_allowed, remaining = check_rate_limit(cost)
    
    if not is_allowed:
        raise HTTPException(
            status_code=429,
            detail={
                "error": "Too Many Requests",
                "message": f"Token limit exceeded. You need {cost} tokens.",
                "tokens_remaining": remaining,
                "limit": TOKEN_LIMIT,
                "window": f"{WINDOW_SIZE} seconds"
            }
        )
    
    return {
        "message": f"PUT request successful for resource {resource_id}",
        "operation": "write",
        "tokens_consumed": cost,
        "tokens_remaining": remaining
    }


@app.delete("/api/resource/{resource_id}")
async def delete_resource(resource_id: str):
    """
    DELETE operation - consumes 10 tokens.
    Heavy operation requiring more tokens.
    """
    cost = TOKEN_COSTS["delete"]
    is_allowed, remaining = check_rate_limit(cost)
    
    if not is_allowed:
        raise HTTPException(
            status_code=429,
            detail={
                "error": "Too Many Requests",
                "message": f"Token limit exceeded. You need {cost} tokens.",
                "tokens_remaining": remaining,
                "limit": TOKEN_LIMIT,
                "window": f"{WINDOW_SIZE} seconds"
            }
        )
    
    return {
        "message": f"DELETE request successful for resource {resource_id}",
        "operation": "delete",
        "tokens_consumed": cost,
        "tokens_remaining": remaining
    }


@app.get("/api/search")
async def search_resources(q: str = ""):
    """
    SEARCH operation - consumes 3 tokens.
    Database search operation.
    """
    cost = TOKEN_COSTS["search"]
    is_allowed, remaining = check_rate_limit(cost)
    
    if not is_allowed:
        raise HTTPException(
            status_code=429,
            detail={
                "error": "Too Many Requests",
                "message": f"Token limit exceeded. You need {cost} tokens.",
                "tokens_remaining": remaining,
                "limit": TOKEN_LIMIT,
                "window": f"{WINDOW_SIZE} seconds"
            }
        )
    
    return {
        "message": "Search completed",
        "operation": "search",
        "query": q,
        "tokens_consumed": cost,
        "tokens_remaining": remaining
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
        "token_costs": TOKEN_COSTS
    }
