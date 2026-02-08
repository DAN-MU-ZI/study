import pytest
from httpx import AsyncClient

@pytest.mark.asyncio
async def test_redirect_url(client: AsyncClient):
    # 1. 단축 URL 생성
    long_url = "https://github.com/fastapi/fastapi"
    create_res = await client.post("/api/v1/data/shorten", json={"longUrl": long_url})
    short_url = create_res.json()["shortUrl"]
    
    # 2. 리디렉션 요청 (기본 모드 302 가정)
    # follow_redirects=False로 설정하여 302 응답 자체를 확인
    redirect_res = await client.get(f"/api/v1/{short_url}", follow_redirects=False)
    
    assert redirect_res.status_code == 302
    assert redirect_res.headers["location"] == long_url

@pytest.mark.asyncio
async def test_redirect_not_found(client: AsyncClient):
    redirect_res = await client.get("/api/v1/unknown123", follow_redirects=False)
    assert redirect_res.status_code == 404
