from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.ext.asyncio import AsyncSession


from app.database import engine, Base, get_db
from app.routers import shorten, redirect
from app.redis_cache import init_redis
from app.bloom_filter import short_url_filter, long_url_filter
from app.middleware import PerformanceMiddleware

app = FastAPI(title="URL Shortener API")

# Performance Logging Middleware
app.add_middleware(PerformanceMiddleware)

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
    # 1. Redis 초기화 (가장 먼저 실행)
    await init_redis()
    from app.redis_cache import redis_client
    import asyncio

    # Gunicorn 멀티 워커 동기화를 위한 분산 락 로직
    lock_key = "system:init_lock"
    done_key = "system:init_done"

    # 2. 락 획득 시도 (Leader Election)
    # nx=True: 키가 없을 때만 설정 (성공 시 True), ex=60: 60초 후 자동 만료 (Deadlock 방지)
    is_leader = await redis_client.set(lock_key, "1", nx=True, ex=60)

    if is_leader:
        print("[Startup] Acquired init lock. Starting initialization as Leader.")
        try:
            # 3. DB 스키마 생성 (Leader Only) 및 Retry 로직
            max_retries = 10
            for attempt in range(max_retries):
                try:
                    async with engine.begin() as conn:
                        await conn.run_sync(Base.metadata.create_all)
                    print("[Startup] DB Schema created.")
                    break
                except Exception as e:
                    if attempt == max_retries - 1:
                        raise e
                    print(f"[Startup] DB Connection failed ({e}). Retrying in 2s... ({attempt+1}/{max_retries})")
                    await asyncio.sleep(2)

            # 4. 블룸 필터 초기화 (Leader Only)
            from app.bloom_filter import initialize_bloom_filter_from_db
            from app.database import AsyncSessionLocal
            async with AsyncSessionLocal() as session:
                await initialize_bloom_filter_from_db(session)
            
            # 완료 플래그 설정
            await redis_client.set(done_key, "1")
            print("[Startup] Initialization complete. Set init_done flag.")
        except Exception as e:
            print(f"[Startup] Error during initialization: {e}")
            # 에러 발생 시 락 해제하여 다른 워커가 시도할 수 있도록 함 (혹은 재시작 유도)
            raise e
        finally:
            await redis_client.delete(lock_key)
    else:
        # 5. Follower: 초기화 완료 대기
        print("[Startup] Follower worker waiting for initialization...")
        wait_count = 0
        while not await redis_client.get(done_key):
            await asyncio.sleep(0.5)
            wait_count += 1
            if wait_count % 10 == 0:
                print(f"[Startup] Still waiting for init_done... ({wait_count * 0.5}s)")
        print("[Startup] Initialization done detected. Worker starting.")

@app.get("/health")
async def health_check():
    """상세 헬스 체크: DB 및 Redis 연결 확인"""
    health_status = {"status": "healthy", "components": {}}
    
    # 1. DB Check
    try:
        async for db in get_db():
            from sqlalchemy import text
            await db.execute(text("SELECT 1"))
            health_status["components"]["database"] = "ok"
            break
    except Exception as e:
        health_status["status"] = "unhealthy"
        health_status["components"]["database"] = f"error: {str(e)}"

    # 2. Redis Check
    try:
        from app.redis_cache import redis_client
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
