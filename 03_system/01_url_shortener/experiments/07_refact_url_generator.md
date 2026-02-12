# URL 생성기 리펙토링 (Sequence + Base62)
MongoDB는 RDB와 달리 Auto Increment 기능이 없고, ObjectId를 그대로 사용하면 URL이 길어지는 단점이 있습니다. 또한 기존의 Random Salt + Hash 방식은 충돌 가능성 때문에 재시도 로직이 필요해 성능상 비효율적이었습니다. 이를 개선하기 위해 MongoDB Sequence를 활용하여 유일한 숫자 ID를 먼저 발급받고, 이를 Base62로 인코딩하여 단축 URL을 생성하는 방식으로 리팩토링했습니다. 이 결과 충돌 가능성이 0%가 되어 검증 로직을 제거할 수 있었으며, RPS는 약 6,000 수준으로 준수하게 유지되었습니다.

## 변경 사항
ID 생성 전략: Random Salt + CRC32 -> MongoDB Sequence
단축 URL 생성: Hash -> Base62 Encoding
DB 스키마: String shortUrl (PK) -> Long id (PK)

## 측정 결과
```
k6-1  |   █ TOTAL RESULTS
k6-1  | 
k6-1  |     checks_total.......: 183007  5982.339317/s
k6-1  |     checks_succeeded...: 100.00% 183007 out of 183007
k6-1  |     checks_failed......: 0.00%   0 out of 183007
k6-1  | 
k6-1  |     ✓ Create status must be 200
k6-1  |     ✓ Redirect status must be 301 or 302
k6-1  | 
k6-1  |     HTTP
k6-1  |     http_req_duration..............: avg=155.94ms min=497.84µs med=131.93ms max=1.11s p(90)=265.31ms p(95)=330.83ms      
k6-1  |       { expected_response:true }...: avg=155.94ms min=497.84µs med=131.93ms max=1.11s p(90)=265.31ms p(95)=330.83ms      
k6-1  |     http_req_failed................: 0.00%  0 out of 183007
k6-1  |     http_reqs......................: 183007 5982.339317/s
k6-1  | 
k6-1  |     EXECUTION
k6-1  |     dropped_iterations.............: 13363  436.824822/s
k6-1  |     iteration_duration.............: avg=1.74s    min=280.88ms med=1.73s    max=3.27s p(90)=2.13s    p(95)=2.44s
k6-1  |     iterations.....................: 16637  543.849029/s
k6-1  |     vus............................: 965    min=645         max=1000
k6-1  |     vus_max........................: 1000   min=1000        max=1000
k6-1  | 
k6-1  |     NETWORK
k6-1  |     data_received..................: 37 MB  1.2 MB/s
k6-1  |     data_sent......................: 17 MB  550 kB/s
k6-1  | 
k6-1  | 
k6-1  | 
k6-1  | 
k6-1  | running (0m30.6s), 0000/1000 VUs, 16637 complete and 0 interrupted iterations
k6-1  | load_test ✓ [ 100% ] 0000/1000 VUs  30s  1000.00 iters/s
k6-1 exited with code 0
```