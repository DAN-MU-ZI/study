# 23. 유효성 (Validity)

GIS에서 폴리곤(Polygon)은 특정한 수학적 규칙을 만족해야 올바른 기하 객체로 인정받습니다. 이를 **지오메트리 유효성(Geometry Validity)**이라고 합니다.

---

## 1. 폴리곤 유효성 규칙 (OGC 사양)

유효한(Valid) 폴리곤의 기본 요건:
1. 폴리곤의 링(외곽선)은 반드시 닫혀 있어야 합니다 (시작점 = 끝점).
2. 폴리곤의 경계선은 스스로 교차(Self-intersection)하지 않아야 합니다 (나비 넥타이 형태 불가).
3. 내부 홀(Hole)은 외부 링 내부에 완전히 위치해야 하며 서로 교차하지 않아야 합니다.

![유효하지 않은 폴리곤 예시](validity/figure_eight.png)

---

## 2. 유효성 검사 및 복구 함수

- `ST_IsValid(geom)`: 유효하면 `true`, 무효하면 `false` 반환
- `ST_IsValidReason(geom)`: 유효하지 않은 구체적인 이유와 발생 좌표를 문자열로 반환
- `ST_MakeValid(geom)`: 깨진 폴리곤을 OGC 규칙에 맞는 유효한 MultiPolygon 등으로 자동 수정

```sql
-- 무효한 폴리곤 검사
SELECT ST_IsValidReason('POLYGON((0 0, 0 2, 2 0, 2 2, 0 0))'::geometry);
```

결과:
```text
Self-intersection [1 1]
```

### 깨진 지오메트리 자동 복구:
```sql
SELECT ST_AsText(ST_MakeValid('POLYGON((0 0, 0 2, 2 0, 2 2, 0 0))'::geometry));
```

결과:
```text
MULTIPOLYGON(((0 0, 0 2, 1 1, 0 0)), ((1 1, 2 2, 2 0, 1 1)))
```

---

| [⬅️ 22. 고급 공간 조인 (More Spatial Joins)](22_joins_advanced.md) | [🏠 워크숍 목차](README.md) | [24. 동등성 (Equality) ➡️](24_equality.md) |
| :--- | :---: | ---: |
