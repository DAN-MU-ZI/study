import redis.asyncio as redis
import hiredis
from app.config import settings

# 전역 변수 선언
_redis_client: redis.Redis = None
_pool: redis.ConnectionPool = None

async def init_redis():
    """Redis 연결 초기화 (앱 시작 시 호출)"""
    global _redis_client, _pool
    redis_url = getattr(settings, 'REDIS_URL', 'redis://localhost:6379')
    
    _pool = redis.ConnectionPool.from_url(
        redis_url,
        decode_responses=True,
        max_connections=1000,
    )
    
    _redis_client = redis.Redis(connection_pool=_pool)

async def close_redis():
    """Redis 연결 종료"""
    global _redis_client, _pool
    if _redis_client:
        await _redis_client.close()
    if _pool:
        await _pool.disconnect()

def get_redis_client() -> redis.Redis:
    global _redis_client
    return _redis_client
