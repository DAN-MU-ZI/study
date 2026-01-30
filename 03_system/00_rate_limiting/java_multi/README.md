# Java Multi Server - Global Weighted Rate Limiting

Spring Boot 다중 서버 환경에서 **Lua Script**를 활용한 원자적 Global Weighted Rate Limiting 구현

## 핵심 특징

- **Global Rate Limiting**: IP 기반이 아닌 전체 서비스 공유 토큰 버킷
- **Weighted Tokens**: 요청 타입별 차등 토큰 비용
- **Atomic Operations**: Lua Script로 Race Condition 방지
- **Load Balancing**: Nginx 로드밸런서 (3 API 서버)

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
java_multi/
├── docker-compose.yml       # 3 API 서버 + Nginx + Redis
├── nginx.conf               # Load Balancer 설정
├── Dockerfile
├── build.gradle
├── src/main/java/com/example/ratelimit/
│   ├── RateLimitApplication.java
│   ├── RateLimitService.java   # Lua Script 실행
│   └── ApiController.java
└── src/main/resources/
    ├── application.yml
    └── scripts/
        └── rate_limit.lua      # Weighted Token Lua Script
```

## 왜 Lua Script가 필요한가?

### 문제: Race Condition
다중 서버 환경에서 토큰 계산과 추가 사이에 다른 서버의 요청이 끼어들면 제한 초과 발생

### 해결: Lua Script (원자적 실행)
```lua
-- 모든 연산이 원자적으로 실행
ZREMRANGEBYSCORE → token 합산 → ZADD
```

## 실행

```bash
# 전체 스택 시작 (3 API 서버 + Nginx + Redis)
docker-compose up --build -d

# API 테스트
curl http://localhost:8080/api/resource     # GET (1 token)
curl -X POST http://localhost:8080/api/resource  # POST (5 tokens)
curl http://localhost:8080/api/stats        # Token usage
```

## API Endpoints

| Endpoint | Method | Tokens | Description |
|----------|--------|--------|-------------|
| `/` | GET | 0 | Health check (serverId 포함) |
| `/api/resource` | GET | 1 | Read resource |
| `/api/resource` | POST | 5 | Create resource |
| `/api/resource/{id}` | DELETE | 10 | Delete resource |
| `/api/search` | GET | 3 | Search resources |
| `/api/stats` | GET | 0 | Token usage stats |

## 테스트

```bash
# 로드밸런서 확인 (여러 serverId 반환)
for i in {1..5}; do curl -s http://localhost:8080/ | jq .serverId; done

# Mixed Request Test
curl http://localhost:8080/api/resource      # 1 token
curl -X POST http://localhost:8080/api/resource  # 5 tokens
curl -X DELETE http://localhost:8080/api/resource/1  # 10 tokens
```
