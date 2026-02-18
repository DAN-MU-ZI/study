# URL Shortener Project

Spring Boot 기반 URL 단축기 시스템입니다.
현재 기준 구현은 `java_app`이며, MongoDB + Redis 조합과 301/302 리다이렉트 모드 비교를 포함합니다.

---

## 1) 현재 구현 아키텍처 (Spring 기준)

### 핵심 구성
- API: Spring Boot (`/api/v1`)
- DB: MongoDB (`url_mappings`, `database_sequences`)
- Cache: Redis (키: `url:{short_url}`, TTL 3600초)

### 단축 URL 생성 방식
- 시퀀스 발급: MongoDB `findAndModify + inc(seq, 1)`
- 인코딩: Sequence ID를 Base62로 변환
- 저장 순서:
  1. MongoDB에 원본 URL 저장
  2. Redis 캐시에 short->long 매핑 저장

### 리다이렉트 조회 방식
- Look-aside 캐시
  1. Redis 조회
  2. miss면 MongoDB 조회
  3. hit면 Redis 갱신 후 redirect
  4. 없으면 404

---

## 2) API 명세 (구현 기준)

### `POST /api/v1/data/shorten`
Long URL을 short URL로 변환합니다.

요청 예시:
```json
{
  "longUrl": "https://www.google.com"
}
```

응답 예시:
```json
{
  "shortUrl": "abc123"
}
```

### `GET /api/v1/{shortUrl}`
short URL을 long URL로 리다이렉트합니다.
- `REDIRECT_MODE=302` (기본): `302 Found`
- `REDIRECT_MODE=301`: `301 Moved Permanently`

### `GET /health`
헬스체크 엔드포인트입니다.

---

## 3) 실행 방법

### 요구 사항
- Docker
- Docker Compose

### 실행
```powershell
cd d:\_Workspace\02_프로젝트\진행중\study\03_system\01_url_shortener\java_app
docker-compose up --build -d
```

### 확인 URL
- API base: `http://localhost:8000/api/v1`
- Health: `http://localhost:8000/health`

---

## 4) 수동 테스트

```powershell
# 1) 단축 URL 생성
curl -X POST http://localhost:8000/api/v1/data/shorten `
  -H "Content-Type: application/json" `
  -d '{"longUrl": "https://www.google.com"}'

# 2) 리다이렉트 확인
curl -I http://localhost:8000/api/v1/{shortUrl}
```

---

## 5) 부하 테스트 (k6)

```powershell
cd d:\_Workspace\02_프로젝트\진행중\study\03_system\01_url_shortener\scripts
./run_k6.ps1
```

직접 실행 시:
```powershell
k6 run load_test.js
```

---

## 6) 최종 성능 개선 결과 (Spring 기준)

아래 수치는 Spring 실험 결과 문서 기준입니다.

### 측정 환경 (CPU)
- 운영체제: `Windows 10`
- 프로세서: `Intel(R) Core(TM) i7-7700HQ CPU @ 2.80GHz, 2808Mhz, 4 코어, 8 논리 프로세서`

### 성능 개선 요약 (Spring 초기 -> Spring 최종)
- 초기 기준: `experiments/03_spring_boot_mvc.md`
- 최종 기준: `experiments/07_refact_url_generator.md`

| 항목 | 초기 (Spring MVC) | 최종 (Sequence + Base62) | 개선율 |
|---|---:|---:|---:|
| 처리량 (`http_reqs/s`) | 897.63 req/s | 5,982.34 req/s | +566.44% (약 6.66배) |
| 평균 지연 (`avg`) | 1.04s | 155.94ms | 약 85.01% 감소 |
| P95 지연 (`p(95)`) | 2.17s | 330.83ms | 약 84.75% 감소 |
| 실패율 (`http_req_failed`) | 0.00% | 0.00% | 동일 |

### 무엇을 개선해서 빨라졌는가
- `DB 변경`: PostgreSQL 대비 읽기/쓰기 경로를 MongoDB에 맞춰 단순화 (`experiments/04_replace_mongo_db.md`)
- `런타임 변경`: Java 21 Virtual Thread 적용으로 동시성 처리 효율 개선 (`experiments/05_virtual_thread.md`)
- `리소스 확장`: Docker/WSL CPU 코어를 2 -> 4로 확장 (`experiments/06_cpu_scale_up.md`)
- `핵심 알고리즘 개선`: `Random Salt + Hash` 재시도 방식에서 `Sequence + Base62` 방식으로 변경해 충돌 검증 비용 제거 (트레이드오프: short URL 순서성이 생겨 URL 추측/열거 가능성이 상대적으로 증가) (`experiments/07_refact_url_generator.md`)
- `캐시/접근 패턴 정리`: Redis Look-aside + write/update 흐름으로 DB hit를 줄이고 tail latency 완화 (`experiments/09_mongo_pool_tuning_on_existing_flow.md`)

### 참고
- CPU/리소스 스케일업 실험: `experiments/06_cpu_scale_up.md`
- Mongo 연결 풀 튜닝 반복 측정: `experiments/09_mongo_pool_tuning_on_existing_flow.md`

---

## 7) 301 vs 302 모드 전환

`java_app/docker-compose.yml`의 환경변수로 제어합니다.

```yaml
environment:
  - REDIRECT_MODE=${REDIRECT_MODE:-302}
```

예시:
```powershell
$env:REDIRECT_MODE="301"
docker-compose up --build -d
```

---

## 8) 로그 분석 스크립트

`scripts/analytics.py`는 아래 패턴 로그를 기대합니다.
- `ACCESS_LOG: {short_url} -> ...`

현재 Spring 기본 로그 설정에서는 해당 패턴이 자동 출력되지 않으므로,
스크립트 사용 전 로그 포맷(또는 접근 로그)을 맞춰야 합니다.
