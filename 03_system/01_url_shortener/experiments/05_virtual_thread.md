# 가상 스레드 도입으로 성능 향상
Java21 버전은 가상 스레드를 지원하면서 기존 동기화 작업에서 Blocking I/O 발생 시 자동으로 가상 스레드로 전환해서 적은 수의 스레드로 대량의 동시 요청을 처리할 수 있습니다. 이전 MongoDB에서 측정 결과 약 2,000 RPS 정도를 유지했지만, 가상 스레드에서는 2,700 RPS 정도로 Java17 Spring Boot MVC 의 더미 RPS인 5,197 RPS의 약 절반 성능으로 향상할 수 있었습니다. 만약 가상 스레드에서 더미 테스트를 진행했다면 더 높은 성능을 얻을 수 있었겠네요.

## 측정 결과
```
k6-1  |   █ TOTAL RESULTS
k6-1  | 
k6-1  |     checks_total.......: 84678   2710.835895/s
k6-1  |     checks_succeeded...: 100.00% 84678 out of 84678
k6-1  |     checks_failed......: 0.00%   0 out of 84678
k6-1  | 
k6-1  |     ✓ Create status must be 200
k6-1  |     ✓ Redirect status must be 301 or 302
k6-1  | 
k6-1  |     HTTP
k6-1  |     http_req_duration..............: avg=351.24ms min=476.75µs med=313.09ms max=1.85s p(90)=561.91ms p(95)=691.74ms      
k6-1  |       { expected_response:true }...: avg=351.24ms min=476.75µs med=313.09ms max=1.85s p(90)=561.91ms p(95)=691.74ms      
k6-1  |     http_req_failed................: 0.00%  0 out of 84678
k6-1  |     http_reqs......................: 84678  2710.835895/s
k6-1  | 
k6-1  |     EXECUTION
k6-1  |     dropped_iterations.............: 22303  713.996233/s
k6-1  |     iteration_duration.............: avg=3.89s    min=591.65ms med=3.9s     max=6.01s p(90)=4.89s    p(95)=5.15s
k6-1  |     iterations.....................: 7698   246.439627/s
k6-1  |     vus............................: 720    min=435        max=1000
k6-1  |     vus_max........................: 1000   min=1000       max=1000
k6-1  | 
k6-1  |     NETWORK
k6-1  |     data_received..................: 17 MB  541 kB/s
k6-1  |     data_sent......................: 8.1 MB 259 kB/s
k6-1  | 
k6-1  | 
k6-1  | 
k6-1  | 
k6-1  | running (0m31.2s), 0000/1000 VUs, 7698 complete and 0 interrupted iterations
k6-1  | load_test ✓ [ 100% ] 0000/1000 VUs  30s  1000.00 iters/s
```