import pytest
from httpx import AsyncClient

@pytest.mark.asyncio
async def test_shorten_url(client: AsyncClient):
    payload = {"longUrl": "https://www.google.com/search?q=python"}
    response = await client.post("/api/v1/data/shorten", json=payload)
    
    assert response.status_code == 200
    data = response.json()
    assert "shortUrl" in data
    assert len(data["shortUrl"]) == 7

@pytest.mark.asyncio
async def test_shorten_existing_url(client: AsyncClient):
    payload = {"longUrl": "https://www.naver.com"}
    
    # 첫 번째 요청
    res1 = await client.post("/api/v1/data/shorten", json=payload)
    assert res1.status_code == 200
    short1 = res1.json()["shortUrl"]
    
    # 두 번째 요청 (동일 URL)
    res2 = await client.post("/api/v1/data/shorten", json=payload)
    assert res2.status_code == 200
    short2 = res2.json()["shortUrl"]
    
    # 동일한 Short URL 반환 확인
    assert short1 == short2
