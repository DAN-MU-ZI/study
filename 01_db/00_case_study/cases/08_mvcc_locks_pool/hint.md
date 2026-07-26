# 힌트

- 테스트 대상 ID는 `SELECT id FROM users ORDER BY id LIMIT 1`로 찾는다.
- 실제 값을 바꾸지 않아도 `SET views = views`는 행 잠금을 획득한다.
- `pg_blocking_pids(pid)`는 특정 세션을 막는 PID 배열을 반환한다.
- 종료 후 `ROLLBACK`하고, 상태가 꼬이면 `scripts/reset-lab.ps1`을 실행한다.

