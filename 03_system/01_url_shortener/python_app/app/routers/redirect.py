from fastapi import APIRouter, HTTPException, status, Request, Depends
from fastapi.responses import RedirectResponse, ORJSONResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from app.config import settings
from app.redis_cache import get_redis_client
from app.database import get_db
from app.models import URLMapping

router = APIRouter(default_response_class=ORJSONResponse)

@router.get("/{short_url}")
async def redirect_to_long(request: Request, short_url: str, db: AsyncSession = Depends(get_db)):
    request_id = getattr(request.state, "request_id", None)
    cache_key = f"url:{short_url}"

    # Look Aside 패턴 적용
    redis_client = get_redis_client()

    # 1. Look Aside: Cache 조회
    cached_long_url = await redis_client.get(cache_key)
    if cached_long_url:
        return create_redirect_response(cached_long_url)

    # 2. Cache Miss -> DB 조회
    result = await db.execute(select(URLMapping).where(URLMapping.short_url == short_url))
    mapping = result.scalar_one_or_none()

    if mapping:
        # 3. Cache Update (TTL 1시간)
        await redis_client.set(cache_key, mapping.long_url, ex=3600)
        return create_redirect_response(mapping.long_url)
    
    # 4. Data Not Found
    raise HTTPException(status_code=404, detail="Short URL not found")



def create_redirect_response(url: str):
    if settings.REDIRECT_MODE == "301":
        return RedirectResponse(url=url, status_code=status.HTTP_301_MOVED_PERMANENTLY)
    return RedirectResponse(url=url, status_code=status.HTTP_302_FOUND)
