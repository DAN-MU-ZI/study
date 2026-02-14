# 기존 URL Shortener 기능 기준 Mongo 연결 풀 튜닝 비교 (3회 반복)
이번에는 벤치 전용 컨트롤러를 제거하고, 기존 기능(`POST /api/v1/data/shorten` + `GET /api/v1/{shortUrl}`)으로만 연결 풀 튜닝 전/후를 비교했습니다.  
단일 측정 편차가 커서, 동일 조건으로 **각각 3회 반복**했습니다.

## 변경 사항
- 제거: `03_system/01_url_shortener/java_app/src/main/java/com/example/urlshortener/controller/MongoBenchController.java`
- compose 개선: `03_system/01_url_shortener/java_app/docker-compose.yml`
  - `SPRING_DATA_MONGODB_URI`를 환경변수로 주입 가능하게 변경
  - `SPRING_DATA_MONGODB_URI=${SPRING_DATA_MONGODB_URI:-mongodb://db:27017/shortener_db}`

## 테스트 조건
- 일자: 2026-02-14
- 시나리오: 기존 `load_test.js` (POST 1회 + GET 10회)
- 부하: `constant-arrival-rate 1000 iters/s, 30s`
- VU: `preAllocated 1000`
- Before URI: `mongodb://db:27017/shortener_db`
- After URI: `mongodb://db:27017/shortener_db?maxPoolSize=500&minPoolSize=50&maxConnecting=20&waitQueueTimeoutMS=2000`

## 반복 측정 결과 (Run 1~3)

### Before
| Run | req/s | avg | p95 | max | dropped |
| :---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 1794.49 | 541.00ms | 995.77ms | 7905.76ms | 24925 |
| 2 | 3671.47 | 261.21ms | 536.62ms | 2264.87ms | 19723 |
| 3 | 3731.23 | 248.86ms | 535.63ms | 6307.06ms | 19424 |

### After
| Run | req/s | avg | p95 | max | dropped |
| :---: | ---: | ---: | ---: | ---: | ---: |
| 1 | 1753.43 | 555.78ms | 1293.73ms | 5091.97ms | 25011 |
| 2 | 3769.31 | 255.92ms | 446.74ms | 4435.00ms | 19121 |
| 3 | 5895.83 | 158.89ms | 314.59ms | 1051.55ms | 13604 |

## 요약 비교

### 3회 평균 기준
| 항목 | Before | After | 변화 |
| :--- | ---: | ---: | ---: |
| req/s | 3065.73 | 3806.19 | **+24.2%** |
| avg latency | 350.36ms | 323.53ms | **-7.7%** |
| p95 latency | 689.34ms | 685.02ms | **-0.6%** |
| max latency | 5492.56ms | 3526.17ms | **-35.8%** |
| dropped iterations | 21357.33 | 19245.33 | **-9.9%** |

### 웜업 구간 제외(run2, run3 평균)
| 항목 | Before(run2~3) | After(run2~3) | 변화 |
| :--- | ---: | ---: | ---: |
| req/s | 3701.35 | 4832.57 | **+30.6%** |
| avg latency | 255.03ms | 207.40ms | **-18.7%** |
| p95 latency | 536.13ms | 380.66ms | **-29.0%** |
| max latency | 4285.97ms | 2743.27ms | **-36.0%** |
| dropped iterations | 19573.50 | 16362.50 | **-16.4%** |

## 07 실험과 교차 비교
`07_refact_url_generator.md` 기준 대표 수치:
- req/s: **5982.34**
- avg: **155.94ms**
- p95: **330.83ms**
- max: **1.11s**

현재 3회 반복의 `After`와 비교하면:
- 단일 최고치(`After run3`)는 **5895.83 req/s**로, 07 대비 약 **-1.45%** 수준까지 근접했습니다.
- 웜업 제외 평균(`After run2~3`)은 **4832.57 req/s**로, 07 대비 약 **-19.2%**입니다.
- 지연도 `run3` 기준은 07과 매우 유사(혹은 일부 구간 개선)하지만, 반복 평균 기준으로 보면 07보다 아직 느립니다.

| 항목 | 07 실험 | 09 After run3 | 차이 |
| :--- | ---: | ---: | ---: |
| req/s | 5982.34 | 5895.83 | **-1.45%** |
| avg latency | 155.94ms | 158.89ms | **+1.89%** |
| p95 latency | 330.83ms | 314.59ms | **-4.91%** |
| max latency | 1110.00ms | 1051.55ms | **-5.27%** |

## 해석
- run1은 before/after 모두 느리고 편차가 큽니다. 웜업 영향이 크게 보입니다.
- run2~3(웜업 이후)로 보면, 연결 풀 튜닝이 처리량과 지연 모두 개선됐습니다.
- 이번 3회 반복에서는 `http_req_failed = 0%`로 실패가 재현되지 않았습니다.

## 결론
- 기존 기능 기준에서도 연결 풀 튜닝은 유효했습니다.
- 성능 판단은 단일 측정보다 **반복 측정 + 웜업 분리(run2~3)** 기준이 더 신뢰도가 높습니다.
