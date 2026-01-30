# Python Single Server - Global Weighted Rate Limiting

FastAPI 단일 서버를 이용한 Global Weighted Rate Limiting 구현

## 핵심 특징

- **Global Rate Limiting**: IP 기반이 아닌 전체 서비스 공유 토큰 버킷
- **Weighted Tokens**: 요청 타입별 차등 토큰 비용
- **Sliding Window**: Redis ZSET 기반 정확한 시간 윈도우

## Token Costs

| Operation | Token Cost | Description |
|-----------|------------|-------------|
| GET (read) | 1 token | 경량 읽기 작업 |
| SEARCH | 3 tokens | 검색 작업 |
| POST/PUT (write) | 5 tokens | 쓰기 작업 |
| DELETE | 10 tokens | 삭제 작업 (무거움) |

**Total Limit**: 100 tokens / 60 seconds (전체 서비스 공유)

## 구조

```
python_single/
├── docker-compose.yml
├── Dockerfile
├── requirements.txt
└── app/
    └── main.py           # FastAPI 앱 (Global Weighted)
```

## 왜 Global Rate Limiting인가?

외부 API 의존 서비스에서 사용:
- 서드파티 API 호출 제한 준수
- 전체 서비스 레벨에서 요청량 관리
- 클라이언트 IP와 무관하게 통합 제한

## 실행

```bash
# 서버 시작
docker-compose up --build -d

# 테스트 스크립트 실행 (상위 폴더에서)
../scripts/test_rate_limit.sh 8000     # Linux/Mac
../scripts/test_rate_limit.ps1 -Port 8000  # Windows PowerShell
```

## API Endpoints

| Endpoint | Method | Tokens | Description |
|----------|--------|--------|-------------|
| `/` | GET | 0 | Health check |
| `/api/resource` | GET | 1 | Read resource |
| `/api/resource` | POST | 5 | Create resource |
| `/api/resource/{id}` | PUT | 5 | Update resource |
| `/api/resource/{id}` | DELETE | 10 | Delete resource |
| `/api/search` | GET | 3 | Search resources |
| `/api/stats` | GET | 0 | Token usage stats |

## 알고리즘 설명

Redis ZSET을 활용한 Weighted Sliding Window:

1. `ZREMRANGEBYSCORE` - 60초 이전 요청 삭제
2. `ZRANGEBYSCORE` - 현재 윈도우 내 요청들 조회
3. Cost 합산 - 각 요청의 토큰 비용 합계 계산
4. `ZADD` - 새 요청 추가 (limit 이내인 경우)

Member 형식: `{uuid}:{cost}` (예: `abc-123:10`)
