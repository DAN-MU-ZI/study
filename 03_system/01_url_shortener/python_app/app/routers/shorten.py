from fastapi import APIRouter, HTTPException, Depends
from fastapi.responses import ORJSONResponse
import logging
import traceback
from pydantic import BaseModel, HttpUrl
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.exc import IntegrityError

from app.hash_generator import generate_short_url, generate_random_salt
from app.redis_cache import get_redis_client
from app.database import get_db
from app.models import URLMapping

router = APIRouter(default_response_class=ORJSONResponse)

class ShortenRequest(BaseModel):
    longUrl: HttpUrl

class ShortenResponse(BaseModel):
    shortUrl: str

@router.post("/data/shorten", response_model=ShortenResponse)
async def shorten_url(body: ShortenRequest, db: AsyncSession = Depends(get_db)):
    long_url_str = str(body.longUrl)
    redis_client = get_redis_client()
    
    salt = ""
    max_retries = 5
    ttl = 3600  # 1시간

    for i in range(max_retries):
        if i > 0:
            salt = generate_random_salt()

        # 해시 생성
        candidate_short = generate_short_url(long_url_str + salt)
        
        # 1. DB 저장 시도 (Write-Through)
        new_mapping = URLMapping(short_url=candidate_short, long_url=long_url_str)
        db.add(new_mapping)
        
        try:
            await db.commit()
            cache_key = f"url:{candidate_short}"
            await redis_client.set(cache_key, long_url_str, ex=ttl)
            
            return ShortenResponse(shortUrl=candidate_short)

        except IntegrityError:
            await db.rollback()
            continue
        except Exception as e:
            await db.rollback()
            logging.error(f"Error during shorten_url: {str(e)}")
            logging.error(traceback.format_exc())
            raise HTTPException(status_code=500, detail="Internal Server Error")

    # 5번 시도 후에도 실패 시 에러 반환
    raise HTTPException(status_code=500, detail="Failed to generate unique short URL")
