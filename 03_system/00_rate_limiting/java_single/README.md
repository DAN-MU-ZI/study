# Java Single Server - Global Weighted Rate Limiting

Spring Boot 단일 서버를 이용한 Global Weighted Rate Limiting 구현

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
java_single/
├── docker-compose.yml
├── Dockerfile
├── build.gradle
├── settings.gradle
├── src/main/java/com/example/ratelimit/
│   ├── RateLimitApplication.java
│   ├── RateLimitService.java      # Global Weighted Rate Limiting
│   └── ApiController.java
└── src/main/resources/
    └── application.yml
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

# API 테스트
curl http://localhost:8080/api/resource     # GET (1 token)
curl -X POST http://localhost:8080/api/resource  # POST (5 tokens)
curl http://localhost:8080/api/stats        # Token usage
```

## API Endpoints

| Endpoint | Method | Tokens | Description |
|----------|--------|--------|-------------|
| `/` | GET | 0 | Health check |
| `/api/resource` | GET | 1 | Read resource |
| `/api/resource` | POST | 5 | Create resource |
| `/api/resource/{id}` | DELETE | 10 | Delete resource |
| `/api/search` | GET | 3 | Search resources |
| `/api/stats` | GET | 0 | Token usage stats |

## 핵심 코드

### RateLimitService.java

```java
// Global key (not IP-based)
private static final String GLOBAL_KEY = "global";

// Redis ZSET 활용 Weighted Sliding Window
Set<String> members = zSetOps.rangeByScore(key, windowStart, now);
int currentUsage = members.stream()
    .mapToInt(m -> Integer.parseInt(m.split(":")[1]))
    .sum();

if (currentUsage + cost <= maxTokens) {
    zSetOps.add(key, UUID + ":" + cost, now);  // 토큰 비용 포함
}
```
