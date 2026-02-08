from fastapi import APIRouter, Depends, HTTPException, status, Request
from fastapi.responses import RedirectResponse
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.database import get_db
from app.models import UrlMapping
from app.config import settings
from app.bloom_filter import short_url_filter
from app.redis_cache import get_cached_url, set_cached_url
from app.utils.timer import Timer

router = APIRouter()

@router.get("/{short_url}")
async def redirect_to_long(request: Request, short_url: str, db: AsyncSession = Depends(get_db)):
    request_id = getattr(request.state, "request_id", None)

    # 1. Redis 캐시 체크
    with Timer("redis_lookup", request_id):
        cached_long_url = await get_cached_url(short_url)
    if cached_long_url:
        return create_redirect_response(cached_long_url)
    
    
    # 2. Bloom Filter Check
    with Timer("bloom_check", request_id):
        exists = await short_url_filter.contains(short_url)

    if not exists:
        raise HTTPException(status_code=404, detail="Short URL not found")

    # 3. DB 조회
    with Timer("db_lookup", request_id):
        stmt = select(UrlMapping).where(UrlMapping.short_url == short_url)
        result = await db.execute(stmt)
        mapping = result.scalar_one_or_none()

    if not mapping:
        raise HTTPException(status_code=404, detail="Short URL not found")

    # 4. 캐시에 저장
    with Timer("cache_set", request_id):
        await set_cached_url(short_url, mapping.long_url)

    return create_redirect_response(mapping.long_url)

def create_redirect_response(url: str):
    if settings.REDIRECT_MODE == "301":
        return RedirectResponse(url=url, status_code=status.HTTP_301_MOVED_PERMANENTLY)
    return RedirectResponse(url=url, status_code=status.HTTP_302_FOUND)
