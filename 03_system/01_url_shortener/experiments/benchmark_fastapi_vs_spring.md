# 프레임워크별 성능 비교
이번 실험은 FastAPI와 Spring Boot의 성능을 비교하기 위해 진행되었습니다. 이 실험을 진행하게 된 계기는 FastAPI 기반 URL 단축기 서버의 RPS 한계가 나타났기 때문입니다. 따라서 더미 API를 작성한 뒤, k6 를 통해서 RPS를 측정한 결과 2359 RPS가 나왔습니다. Go 언어 기반인 레퍼런스에서는 RPS가 6천 대를 기록했기 때문에, 기능이 없는 상태임에도 터무니 없이 낮은 퍼포먼스를 보여주었습니다. 이건 Python의 태생적 한계였기 때문이라고 생각되었고, 차선책으로 익숙한 기술 스택인 Spring Boot 를 사용해서 동일한 테스트를 진행했습니다. Java 기반이라면 컴파일 언어의 특성상 인터프리터 언어인 Python 보다는 훨씬 나은 성능을 보여줄 것이라고 예상했습니다.

# 프레임워크별 성능 측정
## FastAPI
```text
  █ TOTAL RESULTS

    checks_total.......: 75768   2359.295375/s
    checks_succeeded...: 100.00% 75768 out of 75768
    checks_failed......: 0.00%   0 out of 75768

    ✓ Create status must be 200
    ✓ Redirect status must be 301 or 302

    HTTP
    http_req_duration..............: avg=401.72ms min=640.83µs med=354.53ms max=2.95s p(90)=716.1ms p(95)=849.92ms       
      { expected_response:true }...: avg=401.72ms min=640.83µs med=354.53ms max=2.95s p(90)=716.1ms p(95)=849.92ms       
    http_req_failed................: 0.00%  0 out of 75768
    http_reqs......................: 75768  2359.295375/s

    EXECUTION
    dropped_iterations.............: 23113  719.70217/s
    iteration_duration.............: avg=4.44s    min=165.32ms med=4.28s    max=9.23s p(90)=6.51s   p(95)=6.98s
    iterations.....................: 6888   214.481398/s
    vus............................: 406    min=406        max=1000
    vus_max........................: 1000   min=1000       max=1000

    NETWORK
    data_received..................: 11 MB  334 kB/s
    data_sent......................: 7.3 MB 228 kB/s




running (0m32.1s), 0000/1000 VUs, 6888 complete and 0 interrupted iterations
load_test ✓ [ 100% ] 0000/1000 VUs  30s  1000.00 iters/s
```

## Spring Boot
```text
  █ TOTAL RESULTS

    checks_total.......: 159203  5197.56648/s
    checks_succeeded...: 100.00% 159203 out of 159203
    checks_failed......: 0.00%   0 out of 159203

    ✓ Create status must be 200
    ✓ Redirect status must be 301 or 302

    HTTP
    http_req_duration..............: avg=182.79ms min=326.01µs med=146.99ms max=1.14s p(90)=340.74ms p(95)=417.53ms
      { expected_response:true }...: avg=182.79ms min=326.01µs med=146.99ms max=1.14s p(90)=340.74ms p(95)=417.53ms
    http_req_failed................: 0.00%  0 out of 159203
    http_reqs......................: 159203 5197.56648/s

    EXECUTION
    dropped_iterations.............: 15542  507.406131/s
    iteration_duration.............: avg=2.03s    min=66.23ms  med=1.96s    max=4.01s p(90)=2.61s    p(95)=2.98s
    iterations.....................: 14473  472.506044/s
    vus............................: 999    min=471         max=1000
    vus_max........................: 1000   min=1000        max=1000

    NETWORK
    data_received..................: 19 MB  633 kB/s
    data_sent......................: 15 MB  501 kB/s




running (0m30.6s), 0000/1000 VUs, 14473 complete and 0 interrupted iterations
load_test ✓ [ 100% ] 0000/1000 VUs  30s  1000.00 iters/s
```

# 측정 결과 분석
Spring Boot의 RPS가 2배 더 높은 5197을 달성했습니다. p95 응답 시간도 417ms로 FastAPI의 절반 수준이며 특히 dropped_iterations는 15542로 Go환경에서도 2만 대를 기록했던 것과 비교하면 훨씬 적은 수치를 기록했습니다. 전반적으로 높은 성능임을 확인할 수 있었고, 이를 바탕으로 URL 단축기 서버를 구현해보고자 합니다.