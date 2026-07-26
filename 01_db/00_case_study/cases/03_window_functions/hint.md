# 힌트

- 먼저 월·사용자 단위로 집계한다.
- 집계 결과에 `RANK() OVER (PARTITION BY ... ORDER BY ...)`를 적용한다.
- 윈도 함수 결과는 같은 SELECT의 `WHERE`에서 바로 사용할 수 없으므로 CTE를 한 단계 더 둔다.

