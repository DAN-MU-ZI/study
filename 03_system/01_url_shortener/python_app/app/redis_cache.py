import redis.asyncio as redis
from app.config import settings

# Redis 연결 풀
redis_client: redis.Redis = None

async def get_redis() -> redis.Redis:
    """Redis 클라이언트 반환 (Dependency Injection용)"""
    return redis_client

async def init_redis():
    """Redis 연결 초기화 (앱 시작 시 호출)"""
    global redis_client
    redis_url = getattr(settings, 'REDIS_URL', 'redis://localhost:6379')
    # 고부하 상황을 대비해 커넥션 풀 크기를 늘림 (기본값은 10개 내외일 수 있음)
    redis_client = redis.from_url(
        redis_url, 
        decode_responses=True,
        max_connections=500  # 커넥션 풀 대폭 증설
    )
    print(f"[Redis] Connected to {redis_url} (max_connections=500)")

async def close_redis():
    """Redis 연결 종료 (앱 종료 시 호출)"""
    global redis_client
    if redis_client:
        await redis_client.close()
        print("[Redis] Connection closed")

# URL 캐싱 함수들
CACHE_TTL = 3600  # 1시간

async def get_cached_url(short_url: str) -> str | None:
    """캐시에서 Long URL 조회"""
    if redis_client:
        return await redis_client.get(f"url:{short_url}")
    return None

async def set_cached_url(short_url: str, long_url: str):
    """캐시에 URL 저장"""
    if redis_client:
        await redis_client.setex(f"url:{short_url}", CACHE_TTL, long_url)
