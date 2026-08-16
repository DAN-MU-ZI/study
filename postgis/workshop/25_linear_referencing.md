# 25. 선형 참조 (Linear Referencing)

선형 참조(Linear Referencing, LRS)는 선형 객체(도로, 철도, 하천, 송유관 등) 위의 위치를 절대 좌표(X, Y) 대신 **시작점으로부터의 거리(또는 백분율 0.0~1.0)**로 표현하는 기법입니다.

(예: "경부고속도로 서울 기점 42.5km 지점의 포트홀 발생")

![선형 참조 개념](screenshots/lrs1.jpg)

---

## 주요 선형 참조 함수

### 1. `ST_LineLocatePoint(line, point)`
선의 시작점(0.0)부터 끝점(1.0) 사이에서 주어진 점에 가장 가까운 위치의 **상대적 비율(0.0 ~ 1.0)**을 반환합니다.

```sql
SELECT ST_LineLocatePoint(
  'LINESTRING(0 0, 10 0)'::geometry,
  'POINT(4 5)'::geometry
); -- 0.4 반환 (길이 10 중 4 지점)
```

---

### 2. `ST_LineInterpolatePoint(line, fraction)`
선의 시작점으로부터 주어진 비율(0.0 ~ 1.0) 위치에 해당하는 보간된 **Point 좌표**를 반환합니다.

```sql
SELECT ST_AsText(ST_LineInterpolatePoint(
  'LINESTRING(0 0, 10 0)'::geometry,
  0.5
)); -- POINT(5 0) 반환
```

---

### 3. `ST_LineSubstring(line, start_fraction, end_fraction)`
선의 일부분(구간)을 잘라내어 새로운 하위 LineString으로 추출합니다.

---

| [⬅️ 24. 동등성 (Equality)](24_equality.md) | [🏠 워크숍 목차](README.md) | [26. 차원 확장 9-교차 모델 (DE-9IM) ➡️](26_de9im.md) |
| :--- | :---: | ---: |
