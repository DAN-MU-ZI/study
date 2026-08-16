# 24. 동등성 (Equality)

두 지오메트리가 "같다"고 판단하는 데에는 여러 가지 기준이 있습니다.

---

## 1. 공간적 동등성 (`ST_Equals`)
두 지오메트리가 동일한 공간 영역을 점유하고 있으면 구조(정점의 시작 위치나 순서)가 달라도 `true`를 반환합니다.

```sql
SELECT ST_Equals(
  'LINESTRING(0 0, 2 2)'::geometry,
  'LINESTRING(0 0, 1 1, 2 2)'::geometry
); -- true 반환
```

---

## 2. 엄격한 바이너리 일치 (`=`)
두 지오메트리의 저장된 바이트 데이터 및 모든 정점 순서까지 100% 동일한지 비교합니다.

---

## 3. 바운딩 박스 일치 (`~=`)
두 지오메트리의 최소 경계 사각형(MBR)이 정확히 같은지 비교합니다.

---

| [⬅️ 23. 유효성 (Validity)](23_validity.md) | [🏠 워크숍 목차](README.md) | [25. 선형 참조 (Linear Referencing) ➡️](25_linear_referencing.md) |
| :--- | :---: | ---: |
