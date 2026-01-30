"""
Rate Limit Tests for Single Server
"""

import os
import asyncio
import pytest
import httpx

API_HOST = os.getenv("API_HOST", "localhost")
API_PORT = os.getenv("API_PORT", "8000")
BASE_URL = f"http://{API_HOST}:{API_PORT}"


@pytest.fixture
def client():
    return httpx.Client(base_url=BASE_URL, timeout=30.0)


def test_health_check(client):
    """Test health check endpoint"""
    response = client.get("/")
    assert response.status_code == 200
    assert response.json()["status"] == "healthy"


def test_single_request_allowed(client):
    """Test that a single request is allowed"""
    response = client.get("/api/resource")
    assert response.status_code == 200
    assert "remaining_requests" in response.json()


def test_stats_endpoint(client):
    """Test stats endpoint returns correct structure"""
    response = client.get("/api/stats")
    assert response.status_code == 200
    data = response.json()
    assert "requests_used" in data
    assert "requests_remaining" in data
    assert data["limit"] == 100


@pytest.mark.asyncio
async def test_rate_limit_exceeded():
    """
    Test that requests beyond limit return 429.
    Sends 110 requests and verifies at least some are rejected.
    """
    async with httpx.AsyncClient(base_url=BASE_URL, timeout=60.0) as client:
        # Use unique header to simulate different client for this test
        headers = {"X-Forwarded-For": "test-client-rate-limit"}
        
        async def make_request():
            try:
                response = await client.get("/api/resource", headers=headers)
                return response.status_code
            except Exception:
                return 500
        
        # Send 110 concurrent requests
        tasks = [make_request() for _ in range(110)]
        results = await asyncio.gather(*tasks)
        
        success_count = results.count(200)
        rejected_count = results.count(429)
        
        print(f"Success: {success_count}, Rejected: {rejected_count}")
        
        # Should have exactly 100 successes and at least some rejections
        assert success_count <= 100, f"Expected at most 100 successes, got {success_count}"
        assert rejected_count >= 10, f"Expected at least 10 rejections, got {rejected_count}"


@pytest.mark.asyncio
async def test_concurrent_requests():
    """Test handling of concurrent requests"""
    async with httpx.AsyncClient(base_url=BASE_URL, timeout=30.0) as client:
        headers = {"X-Forwarded-For": "test-client-concurrent"}
        
        async def make_request():
            response = await client.get("/api/resource", headers=headers)
            return response.status_code
        
        # Send 50 concurrent requests (should all succeed)
        tasks = [make_request() for _ in range(50)]
        results = await asyncio.gather(*tasks)
        
        success_count = results.count(200)
        assert success_count == 50, f"Expected 50 successes, got {success_count}"
