# 26. 차원 확장 9-교차 모델 (DE-9IM)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/de9im.html>](https://postgis.net/workshops/postgis-intro/de9im.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

선형 참조가 선 위의 위치를 수치로 표현했다면, 이번 장에서는 점·선·면 사이의 관계 자체를 하나의 일관된 규칙으로 표현합니다.

**차원 확장 9-교차 모델(DE-9IM, Dimensionally Extended 9-Intersection Model)**은 두 공간 객체 간의 위상학적 관계를 정밀하게 표현하고 분석하기 위한 수학적 프레임워크입니다.

---

## 1. 공간 객체의 3대 구성 요소: 내부, 경계, 외부

모든 지오메트리 객체는 공간을 세 부분으로 나눕니다.

- **내부(Interior, I)**: 객체 자체를 구성하는 핵심 영역
- **경계(Boundary, B)**: 객체의 내부와 외부를 가르는 한계선
- **외부(Exterior, E)**: 객체 바깥쪽의 나머지 모든 공간

### 지오메트리 타입별 내부/경계/외부 정의
- **폴리곤(Polygon)**:
  - 내부(I): 링으로 둘러싸인 2차원 면적 영역 (차원: 2)
  - 경계(B): 폐곡선 링 선분 자체 (차원: 1)
  - 외부(E): 폴리곤 바깥의 모든 2차원 공간 (차원: 2)
  ![폴리곤의 내부 경계 외부를 구분한 DE-9IM 구성](screenshots/de9im1.jpg)
- **라인스트링(LineString)**:
  - 내부(I): 양 끝점을 제외한 선분 경로 (차원: 1)
  - 경계(B): 시작점과 끝점 2개의 정점 (차원: 0)
  - 외부(E): 선 바깥의 모든 2차원 평면 공간 (차원: 2)
  ![라인스트링의 내부 경계 외부를 구분한 DE-9IM 구성](screenshots/de9im2.jpg)
- **포인트(Point)**:
  - 내부(I): 점 자체 (차원: 0)
  - 경계(B): 존재하지 않음(공집합, $\emptyset$ / F)
  - 외부(E): 점 바깥의 모든 2차원 공간 (차원: 2)

---

## 2. DE-9IM 3×3 교차 행렬 구조

두 공간 객체 A와 B가 주어졌을 때, 각각의 내부(I), 경계(B), 외부(E)가 서로 교차하는 9가지 조합의 **교집합 차원(Dimension)**을 3×3 행렬로 기록합니다.

| A \ B | 내부 ($I_B$) | 경계 ($B_B$) | 외부 ($E_B$) |
| :---: | :---: | :---: | :---: |
| **내부 ($I_A$)** | $\dim(I_A \cap I_B)$ | $\dim(I_A \cap B_B)$ | $\dim(I_A \cap E_B)$ |
| **경계 ($B_A$)** | $\dim(B_A \cap I_B)$ | $\dim(B_A \cap B_B)$ | $\dim(B_A \cap E_B)$ |
| **외부 ($E_A$)** | $\dim(E_A \cap I_B)$ | $\dim(E_A \cap B_B)$ | $\dim(E_A \cap E_B)$ |

행렬 셀에 들어가는 값:
- **`2`**: 2차원 면적으로 교차 (Area)
- **`1`**: 1차원 선분으로 교차 (Line)
- **`0`**: 0차원 점으로 교차 (Point)
- **`F`**: 교차하지 않음 (공집합, False / Empty)

![두 공간 객체의 내부 경계 외부 교차를 배치한 3 곱하기 3 행렬](screenshots/de9im3.jpg)

---

## 3. ST_Relate 함수를 통한 DE-9IM 행렬 생성

`ST_Relate(geometry A, geometry B)` 함수는 두 지오메트리의 3×3 DE-9IM 관계를 행 단위로 나열한 **9자리 문자열**로 반환합니다.

```sql
SELECT ST_Relate(
  'LINESTRING(0 0, 2 0)',
  'POLYGON((1 -1, 1 1, 3 1, 3 -1, 1 -1))'
);
```

```text
st_relate
---------
1010F0212
```

이 문자열을 3×3 행렬로 배치하면 다음과 같습니다.

```text
1 0 1  (A의 내부 vs B의 I, B, E)
0 F 0  (A의 경계 vs B의 I, B, E)
2 1 2  (A의 외부 vs B의 I, B, E)
```

---

## 4. DE-9IM 패턴 매칭 질의 (Pattern Matching)

DE-9IM의 진정한 강점은 표준 공간 술어(`ST_Intersects`, `ST_Contains` 등)로는 표현하기 어려운 **매우 독특하고 복잡한 위상학적 관계를 패턴 문자열로 직접 필터링할 수 있다는 점**입니다.

### 패턴 문자열의 와일드카드 규칙
- `*`: 어떤 차원(0, 1, 2, F)이든 상관없음 (와일드카드)
- `T`: 공집합이 아닌 모든 교차(0, 1, 2 중 하나) 허용
- `F`: 교차하지 않음(공집합)
- `0`, `1`, `2`: 해당 차원으로 정확히 교차

### 실습: 호수(Lakes)와 선착장(Docks) 관계 분석

> **비즈니스 규칙**: "정상적인 선착장(Dock)은 한쪽 끝이 호수(Lake) 경계에 닿아 있고, 몸체는 호수 내부에 완전히 들어가 있어야 한다."

![호수와 부두의 관계를 DE-9IM 패턴으로 구분한 예제](screenshots/de9im7.jpg)

이 조건을 DE-9IM 행렬로 도출하면 `1*F00F212` 패턴이 됩니다.

```sql
CREATE TABLE lakes ( id serial primary key, geom geometry );
CREATE TABLE docks ( id serial primary key, good boolean, geom geometry );

INSERT INTO lakes ( geom ) VALUES
  ('POLYGON ((100 200, 140 230, 180 310, 280 310, 390 270, 400 210, 320 140, 215 141, 150 170, 100 200))');

INSERT INTO docks ( geom, good ) VALUES
  ('LINESTRING (170 290, 205 272)', true),
  ('LINESTRING (120 215, 176 197)', true),
  ('LINESTRING (290 260, 340 250)', false),
  ('LINESTRING (350 300, 400 320)', false),
  ('LINESTRING (370 230, 420 240)', false),
  ('LINESTRING (370 180, 390 160)', false);
```

```sql
SELECT docks.*
FROM docks
JOIN lakes
  ON ST_Intersects(docks.geom, lakes.geom)
WHERE ST_Relate(docks.geom, lakes.geom, '1*F00F212');
```

---

## 5. 공간 데이터 품질 검증(QA)에의 활용

DE-9IM은 데이터 정합성 및 오류 검출(QA)에 매우 강력합니다.

### 1) 인구조사 블록 간의 면적 중복 검출
인구조사 블록끼리는 내부가 2차원 면적(`2`)으로 겹치면 안 됩니다.

```sql
SELECT a.gid, b.gid
FROM nyc_census_blocks AS a, nyc_census_blocks AS b
WHERE ST_Intersects(a.geom, b.geom)
  AND ST_Relate(a.geom, b.geom, '2********')
  AND a.gid != b.gid
LIMIT 10;
```

### 2) 도로망 네트워크의 노드 끝점 일치성 검출
도로망의 교차점이 선분의 끝점(경계, `0`)이 아닌 선분 중간(내부, `1`)에서 교차하고 있는 비정상 도로를 검출합니다.

```sql
SELECT a.gid, b.gid
FROM nyc_streets AS a, nyc_streets AS b
WHERE ST_Intersects(a.geom, b.geom)
  AND NOT ST_Relate(a.geom, b.geom, '****0****')
  AND a.gid != b.gid
LIMIT 10;
```

---

## 함수 목록 (Function List)

- [ST_Relate(geometry A, geometry B)](http://postgis.net/docs/ST_Relate.html): 두 지오메트리 간의 9자리 DE-9IM 위상 관계 문자열을 반환합니다.
- [ST_Relate(geometry A, geometry B, pattern)](http://postgis.net/docs/ST_Relate.html): 두 지오메트리의 관계가 지정된 DE-9IM 패턴 문자열(와일드카드 `*`, `T`, `F`, `0~2` 포함)과 일치하면 `TRUE`를 반환합니다.


---

[← 이전](25_linear_referencing.md) · [목차](00_index.md) · [다음 →](27_clusterindex.md)
