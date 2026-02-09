# PostgreSQL에서 MongoDB로 교체
Go 레퍼런스는 MongoDB를 사용했습니다. 이유는 MongoDB가 단순 조회/삽입 속도가 더 빠르고, 따라서 속도 향상이 있을거라 판단했기 때문입니다. 실제로 측정한 결과 스프링의 순수 RPS인 5197에는 못미치지만 약 40%의 성능은 확보하게 되었습니다. 또한 MVC 환경임에도 충분한 성능을 얻을 수 있었기에 향상된 결과라고 볼 수 있습니다.

## 측정 결과
```
k6-1  |   █ TOTAL RESULTS
k6-1  | 
k6-1  |     checks_total.......: 64559   1962.701523/s
k6-1  |     checks_succeeded...: 100.00% 64559 out of 64559
k6-1  |     checks_failed......: 0.00%   0 out of 64559
k6-1  | 
k6-1  |     ✓ Create status must be 200
k6-1  |     ✓ Redirect status must be 301 or 302
k6-1  | 
k6-1  |     HTTP
k6-1  |     http_req_duration..............: avg=478.59ms min=1.79ms med=409.05ms max=2.4s  p(90)=813.57ms p(95)=1.02s
k6-1  |       { expected_response:true }...: avg=478.59ms min=1.79ms med=409.05ms max=2.4s  p(90)=813.57ms p(95)=1.02s
k6-1  |     http_req_failed................: 0.00%  0 out of 64559
k6-1  |     http_reqs......................: 64559  1962.701523/s
k6-1  | 
k6-1  |     EXECUTION
k6-1  |     dropped_iterations.............: 24226  736.510899/s
k6-1  |     iteration_duration.............: avg=5.33s    min=2.6s   med=5.17s    max=8.18s p(90)=7.02s    p(95)=7.37s
k6-1  |     iterations.....................: 5869   178.427411/s
k6-1  |     vus............................: 571    min=359        max=1000
k6-1  |     vus_max........................: 1000   min=1000       max=1000
k6-1  | 
k6-1  |     NETWORK
k6-1  |     data_received..................: 13 MB  392 kB/s
k6-1  |     data_sent......................: 6.2 MB 188 kB/s
k6-1  | 
k6-1  | 
k6-1  | 
k6-1  | 
k6-1  | running (0m32.9s), 0000/1000 VUs, 5869 complete and 0 interrupted iterations
k6-1  | load_test ✓ [ 100% ] 0000/1000 VUs  30s  1000.00 iters/s
```