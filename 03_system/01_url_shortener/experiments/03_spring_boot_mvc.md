# Spring boot MVC 기반 서버 미그레이션
이전 01 실험에서 FastAPI 환경은 RPS 547을 기록했습니다. 이번에는 Spring boot MVC 기반으로 서버를 구성하여 동일한 조건으로 테스트를 진행했습니다. 코드가 간결해졌고, Redis 활용 전략을 명확히 했습니다. 목표로하는 RPS 11,000을 달성하지는 못했지만 FastAPI 환경보다는 처리속도가 빠른것이 확인되었으므로 더 개선할 여지는 있습니다.

```text
  █ TOTAL RESULTS

    checks_total.......: 32142   897.625332/s
    checks_succeeded...: 100.00% 32142 out of 32142
    checks_failed......: 0.00%   0 out of 32142

    ✓ Create status must be 200
    ✓ Redirect status must be 301 or 302

    HTTP
    http_req_duration..............: avg=1.04s  min=1.51ms med=1.03s  max=6.24s  p(90)=1.76s  p(95)=2.17s
      { expected_response:true }...: avg=1.04s  min=1.51ms med=1.03s  max=6.24s  p(90)=1.76s  p(95)=2.17s
    http_req_failed................: 0.00%  0 out of 32142
    http_reqs......................: 32142  897.625332/s

    EXECUTION
    dropped_iterations.............: 27079  756.231609/s
    iteration_duration.............: avg=11.53s min=1.53s  med=11.64s max=19.03s p(90)=15.27s p(95)=16.06s
    iterations.....................: 2922   81.602303/s
    vus............................: 193    min=193        max=1000
    vus_max........................: 1000   min=1000       max=1000

    NETWORK
    data_received..................: 6.4 MB 179 kB/s
    data_sent......................: 3.1 MB 86 kB/s




 running (0m35.8s), 0000/1000 VUs, 2922 complete and 0 interrupted iterations
 load_test ✓ [ 100% ] 0000/1000 VUs  30s  1000.00 iters/s
```