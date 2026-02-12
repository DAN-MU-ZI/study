# CPU 코어 추가로 스케일업
우연히 도커 설정을 확인하다가 CPU 코어가 2개로 설정된 것을 발견했습니다. 지금까지 성능이 낮았던 이유가 여기 있었네요. 그래서 CPU 코어를 4개로 늘리고 기존처럼 더미 API와 실제 기능에서 부하 테스트를 진행했습니다. 이전 2코어에 비해 선형적으로 성능이 향상되었습니다. 더미 API에서 RPS가 약 8천을 기록했기에 여기서 내부 로직이나 DB 접근이 추가되면서 발생하는 성능저하를 고려해서 RPS 7천을 목표로 개선해보겠습니다. 

## 도커 설정
### Before
```
[wsl2]
memory=4GB
processors=2
swap=1GB
```

### After
```
[wsl2]
memory=8GB
processors=4
swap=1GB
```

## 측정 결과
### 더미 API
```
  █ TOTAL RESULTS

    checks_total.......: 240020  7836.83301/s
    checks_succeeded...: 100.00% 240020 out of 240020
    checks_failed......: 0.00%   0 out of 240020

    ✓ Create status must be 200
    ✓ Redirect status must be 301 or 302

    HTTP
    http_req_duration..............: avg=118.14ms min=232.91µs med=104.31ms max=815.46ms p(90)=219.82ms p(95)=271.66ms   
      { expected_response:true }...: avg=118.14ms min=232.91µs med=104.31ms max=815.46ms p(90)=219.82ms p(95)=271.66ms   
    http_req_failed................: 0.00%  0 out of 240020
    http_reqs......................: 240020 7836.83301/s

    EXECUTION
    dropped_iterations.............: 8181   267.115786/s
    iteration_duration.............: avg=1.33s    min=24.31ms  med=1.34s    max=2.31s    p(90)=1.68s    p(95)=1.78s      
    iterations.....................: 21820  712.439365/s
    vus............................: 949    min=469         max=1000
    vus_max........................: 1000   min=1000        max=1000

    NETWORK
    data_received..................: 27 MB  873 kB/s
    data_sent......................: 24 MB  798 kB/s




 running (0m30.6s), 0000/1000 VUs, 21820 complete and 0 interrupted iterations
 load_test ✓ [ 100% ] 0000/1000 VUs  30s  1000.00 iters/s
```

### 실제 기능
```
  █ TOTAL RESULTS

    checks_total.......: 169917  5555.851313/s
    checks_succeeded...: 100.00% 169917 out of 169917
    checks_failed......: 0.00%   0 out of 169917

    ✓ Create status must be 200
    ✓ Redirect status must be 301 or 302

    HTTP
    http_req_duration..............: avg=169.74ms min=685.95µs med=144.26ms max=1.63s p(90)=283.47ms p(95)=350.82ms      
      { expected_response:true }...: avg=169.74ms min=685.95µs med=144.26ms max=1.63s p(90)=283.47ms p(95)=350.82ms      
    http_req_failed................: 0.00%  0 out of 169917
    http_reqs......................: 169917 5555.851313/s

    EXECUTION
    dropped_iterations.............: 14554  475.878576/s
    iteration_duration.............: avg=1.9s     min=234.76ms med=1.82s    max=4.13s p(90)=2.45s    p(95)=2.76s
    iterations.....................: 15447  505.077392/s
    vus............................: 983    min=543         max=1000
    vus_max........................: 1000   min=1000        max=1000

    NETWORK
    data_received..................: 34 MB  1.1 MB/s
    data_sent......................: 16 MB  531 kB/s




 running (0m30.6s), 0000/1000 VUs, 15447 complete and 0 interrupted iterations
 load_test ✓ [ 100% ] 0000/1000 VUs  30s  1000.00 iters/s
```