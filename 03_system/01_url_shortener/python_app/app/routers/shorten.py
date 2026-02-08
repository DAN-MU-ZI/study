import asyncio
from fastapi import APIRouter, Depends, HTTPException, Request
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select
from pydantic import BaseModel, HttpUrl

from app.database import get_db
from app.models import UrlMapping
from app.hash_generator import generate_short_url, generate_random_salt
from app.bloom_filter import short_url_filter, long_url_filter
from app.redis_cache import set_cached_url
from app.utils.timer import Timer

router = APIRouter()

class ShortenRequest(BaseModel):
    longUrl: HttpUrl

class ShortenResponse(BaseModel):
    shortUrl: str

@router.post("/data/shorten", response_model=ShortenResponse)
async def shorten_url(request: Request, body: ShortenRequest, db: AsyncSession = Depends(get_db)):
    long_url_str = str(body.longUrl)
    request_id = getattr(request.state, "request_id", None)

    # 1. 이미 존재하는 URL인지 확인 (Bloom Filter Optimization)
    # 1. 이미 존재하는 URL인지 확인 (Bloom Filter Optimization)
    with Timer("check_existing_bloom", request_id):
        exists_in_filter = await long_url_filter.contains(long_url_str)

    if exists_in_filter:
        with Timer("check_existing_db", request_id):
            stmt = select(UrlMapping).where(UrlMapping.long_url == long_url_str)
            result = await db.execute(stmt)
            mapping = result.scalar_one_or_none()
        
        if mapping:
            return {"shortUrl": mapping.short_url}

    # 2. 새로운 단축 URL 생성 (충돌 처리)
    salt = ""
    for _ in range(10):
        with Timer("generate_hash", request_id):
            candidate_short = generate_short_url(long_url_str + salt)
        
        # Bloom Filter Check
        with Timer("check_collision_bloom", request_id):
            is_collision_bloom = await short_url_filter.contains(candidate_short)

        if is_collision_bloom:
            with Timer("check_collision_db", request_id):
                stmt = select(UrlMapping).where(UrlMapping.short_url == candidate_short)
                result = await db.execute(stmt)
                if result.scalar_one_or_none():
                    salt = generate_random_salt()
                    continue
        
        # 중복 없음 -> 저장 시도
        try:
            with Timer("db_save_mapping", request_id):
                new_mapping = UrlMapping(short_url=candidate_short, long_url=long_url_str)
                db.add(new_mapping)
                await db.commit()
                await db.refresh(new_mapping)

            # 블룸 필터 업데이트와 Redis 캐싱을 병렬로 처리
            with Timer("cache_update_async", request_id):
                await asyncio.gather(
                    short_url_filter.add(candidate_short),
                    long_url_filter.add(long_url_str),
                    set_cached_url(candidate_short, long_url_str)
                )
            
            return {"shortUrl": new_mapping.short_url}
        except Exception:
            await db.rollback()
            salt = generate_random_salt()
            continue
    
    raise HTTPException(status_code=500, detail="Failed to generate unique short URL")
