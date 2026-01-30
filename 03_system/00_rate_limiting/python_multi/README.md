# Python Multi Server - Global Weighted Rate Limiting

FastAPI 다중 서버 환경에서 **Lua Script**를 활용한 원자적 Global Weighted Rate Limiting 구현

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
python_multi/
├── docker-compose.yml    # 3 API 서버 + Nginx + Redis
├── nginx.conf            # Load Balancer 설정
├── Dockerfile
├── requirements.txt
└── app/
    ├── main.py           # FastAPI 앱 (Global Weighted)
    └── rate_limit.lua    # Weighted Token Lua Script
```

## 왜 Global Rate Limiting인가?

외부 API 의존 서비스에서 사용:
- 서드파티 API 호출 제한 준수
- 전체 서비스 레벨에서 요청량 관리
- 클라이언트 IP와 무관하게 통합 제한

## 실행

```bash
# 전체 스택 시작 (3 API 서버 + Nginx + Redis)
docker-compose up --build -d

# 테스트 스크립트 실행 (상위 폴더에서)
../scripts/test_rate_limit.sh 8080     # Linux/Mac
../scripts/test_rate_limit.ps1 -Port 8080  # Windows PowerShell
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

## 테스트 예시

```powershell
# PowerShell - Mixed Request Test
.\scripts\test_mixed.ps1

# 결과 예시:
# GET #1 (1 token) - remaining: 99, server: api1
# POST #1 (5 tokens) - remaining: 94, server: api2
# DELETE #1 (10 tokens) - remaining: 84, server: api3
# ...
# 429 Too Many Requests (limit exceeded)
```
