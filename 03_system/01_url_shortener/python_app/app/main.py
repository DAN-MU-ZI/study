from fastapi import FastAPI
from fastapi.responses import ORJSONResponse
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.ext.asyncio import AsyncSession


# from app.database import engine, Base, get_db
from app.routers import shorten, redirect
from app.redis_cache import init_redis, get_redis_client

app = FastAPI(
    title="URL Shortener API",
    default_response_class=ORJSONResponse
)

# CORS 설정
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.on_event("startup")
async def startup_event():
    # 0. DB 테이블 생성 (비동기)
    from app.database import engine, Base
    async with engine.begin() as conn:
        # 개발 편의상: 존재하지 않으면 생성
        await conn.run_sync(Base.metadata.create_all)

    # 1. Redis 초기화
    await init_redis()
    redis_client = get_redis_client()
    import asyncio

    # Gunicorn 멀티 워커 동기화를 위한 분산 락 로직
    lock_key = "system:init_lock"
    done_key = "system:init_done"

    # 2. 락 획득 시도 (Leader Election)
    # nx=True: 키가 없을 때만 설정 (성공 시 True), ex=60: 60초 후 자동 만료 (Deadlock 방지)
    is_leader = await redis_client.set(lock_key, "1", nx=True, ex=60)

    if is_leader:
        try:
            # 완료 플래그 설정
            await redis_client.set(done_key, "1")
        except Exception as e:
            # 에러 발생 시 락 해제하여 다른 워커가 시도할 수 있도록 함 (혹은 재시작 유도)
            raise e
        finally:
            await redis_client.delete(lock_key)
    else:
        # 5. Follower: 초기화 완료 대기
        wait_count = 0
        while not await redis_client.get(done_key):
            await asyncio.sleep(0.5)
            wait_count += 1
            if wait_count > 10: # 5초 대기 후 진행
                break

@app.get("/health")
async def health_check():
    """상세 헬스 체크: DB 및 Redis 연결 확인"""
    health_status = {"status": "healthy", "components": {}}
    
    # 1. Redis Check
    try:
        redis_client = get_redis_client()
        if redis_client and await redis_client.ping():
            health_status["components"]["redis"] = "ok"
        else:
            health_status["status"] = "unhealthy"
            health_status["components"]["redis"] = "disconnected"
    except Exception as e:
        health_status["status"] = "unhealthy"
        health_status["components"]["redis"] = f"error: {str(e)}"

    return health_status

app.include_router(shorten.router, prefix="/api/v1")
app.include_router(redirect.router, prefix="/api/v1")
