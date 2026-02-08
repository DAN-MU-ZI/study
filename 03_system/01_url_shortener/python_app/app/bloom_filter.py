import hashlib
import redis.asyncio as redis
from app import redis_cache

class RedisBloomFilter:
    """
    Redis 기반 블룸 필터.
    Gunicorn 멀티 워커 환경에서 모든 프로세스가 동일한 블룸 필터 상태를 공유.
    """
    def __init__(self, key: str, size: int = 5_000_000, hash_count: int = 5):
        self.key = f"bloom:{key}"
        self.size = size
        self.hash_count = hash_count

    def _get_hash_indices(self, item: str) -> list[int]:
        indices = []
        digest = hashlib.md5(item.encode()).hexdigest()
        base_hash = int(digest, 16)
        for i in range(self.hash_count):
            index = (base_hash + i * 1999) % self.size
            indices.append(index)
        return indices

    async def add(self, item: str):
        """블룸 필터에 항목 추가"""
        client = redis_cache.redis_client
        if client:
            pipe = client.pipeline()
            for index in self._get_hash_indices(item):
                pipe.setbit(self.key, index, 1)
            await pipe.execute()

    async def add_many(self, items: list[str]):
        """블룸 필터에 항목 다 건 추가 (Batch)"""
        client = redis_cache.redis_client
        if client and items:
            pipe = client.pipeline()
            for item in items:
                for index in self._get_hash_indices(item):
                    pipe.setbit(self.key, index, 1)
            await pipe.execute()

    async def contains(self, item: str) -> bool:
        """
        블룸 필터에서 항목 존재 여부 확인
        False -> 확실히 없음 (DB 조회 불필요)
        True -> 있을 수 있음 (DB 조회 필요, False Positive 가능)
        """
        client = redis_cache.redis_client
        if not client:
            return True  # Redis 없으면 항상 DB 조회
        
        pipe = client.pipeline()
        for index in self._get_hash_indices(item):
            pipe.getbit(self.key, index)
        results = await pipe.execute()
        return all(results)

# 전역 인스턴스
short_url_filter = RedisBloomFilter("short_url", size=5_000_000, hash_count=5)
long_url_filter = RedisBloomFilter("long_url", size=5_000_000, hash_count=5)

async def initialize_bloom_filter_from_db(db):
    """
    서버 시작 시 DB 데이터로 블룸 필터 초기화.
    Redis 기반이므로 한 번만 실행하면 모든 워커에서 공유됨.
    """
    from sqlalchemy.future import select
    from app.models import UrlMapping
    
    # 이미 초기화되었는지 확인 (Gunicorn 멀티 워커 중복 실행 방지)
    client = redis_cache.redis_client
    if client and await client.exists(short_url_filter.key) and await client.exists(long_url_filter.key):
        print(f"[RedisBloomFilter] Already initialized. Skipping.")
        return

    # Short URL 로딩
    stmt = select(UrlMapping.short_url)
    result = await db.execute(stmt)
    short_urls = result.scalars().all()
    
    if short_urls:
        await short_url_filter.add_many(short_urls)
    
    # Long URL 로딩
    stmt_long = select(UrlMapping.long_url)
    result_long = await db.execute(stmt_long)
    long_urls = result_long.scalars().all()
    
    if long_urls:
        await long_url_filter.add_many(long_urls)
    
    print(f"[RedisBloomFilter] Initialized: Short({len(short_urls)}), Long({len(long_urls)})")
