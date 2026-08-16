# 26. 차원 확장 9-교차 모델 (DE-9IM)

**DE-9IM (Dimensionally Extended 9-Intersection Model)**은 두 지오메트리 간의 모든 가능한 위상학적 공간 관계를 3×3 행렬(내부 Interior, 경계 Boundary, 외부 Exterior)로 정의하는 수학적 표준입니다.

![DE-9IM 행렬](screenshots/de9im3.jpg)

---

## 1. 9개 교차 영역 행렬

두 지오메트리 $a$와 $b$의 교차 차원(-1: 공집합, 0: 점, 1: 선, 2: 면):

$$egin{bmatrix}
\dim(I(a) \cap I(b)) & \dim(I(a) \cap B(b)) & \dim(I(a) \cap E(b)) \
\dim(B(a) \cap I(b)) & \dim(B(a) \cap B(b)) & \dim(B(a) \cap E(b)) \
\dim(E(a) \cap I(b)) & \dim(E(a) \cap B(b)) & \dim(E(a) \cap E(b))
\end{bmatrix}$$

---

## 2. `ST_Relate` 함수

`ST_Relate(a, b)`를 실행하면 9자리 문자열 패턴 코드가 반환됩니다:

```sql
SELECT ST_Relate(
  'POLYGON((0 0, 0 2, 2 2, 2 0, 0 0))'::geometry,
  'POINT(1 1)'::geometry
);
```

결과:
```text
0FFFFF212
```

특정 복합 위상 조건을 만족하는지 마스크 패턴(`'T*F**FFF*'`)을 지정하여 검사할 수도 있습니다:
```sql
SELECT ST_Relate(a.geom, b.geom, 'T*F**FFF*')
FROM my_table a, my_table b;
```

---

| [⬅️ 25. 선형 참조 (Linear Referencing)](25_linear_referencing.md) | [🏠 워크숍 목차](README.md) | [27. 인덱스 기반 클러스터링 (Clustering on Indices) ➡️](27_clusterindex.md) |
| :--- | :---: | ---: |
