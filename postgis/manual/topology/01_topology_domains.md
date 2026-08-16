# 9.4. 토폴로지 도메인 (Topology Domains)

PostGIS Topology 확장을 설치하면 PostgreSQL에 새로운 **도메인(Domain)**들이 생성됩니다. 도메인은 테이블의 컬럼이나 함수의 반환 타입 등 일반적인 객체 데이터 타입처럼 사용할 수 있습니다.

> [!NOTE]
> **도메인(Domain)이란?**  
> PostgreSQL에서 도메인은 기존 기본 데이터 타입에 특정 **검사 제약 조건(CHECK Constraint)**이 결합된 사용자 정의 데이터 타입입니다. 유효하지 않은 형태의 데이터가 입력되는 것을 방지합니다.

---

## 목차
- [TopoElement](#topoelement)
- [TopoElementArray](#topoelementarray)

---

## TopoElement

**`TopoElement`** — 일반적으로 `TopoGeometry`의 구성 요소를 식별하는 데 사용되는 **2개의 정수로 이루어진 1차원 정수 배열 (`integer[2]`)** 도메인입니다.

### 설명 (Description)
단순(Simple) 또는 계층(Hierarchical) `TopoGeometry` 객체를 구성하는 단위 요소 1개를 표현하는 2개 원소 배열(`ARRAY[id, type]`)입니다.

1. **단순 TopoGeometry (Simple TopoGeometry)**
   - **첫 번째 요소 (`te[1]`)**: 토폴로지 기본 요소(Topological Primitive)의 고유 식별자(ID)
   - **두 번째 요소 (`te[2]`)**: 해당 기본 요소의 유형 코드
     - `1`: **노드 (Node / Point)**
     - `2`: **에지 (Edge / Line)**
     - `3`: **면 (Face / Polygon)**
2. **계층 TopoGeometry (Hierarchical TopoGeometry)**
   - **첫 번째 요소 (`te[1]`)**: 자식(Child) `TopoGeometry`의 식별자(ID)
   - **두 번째 요소 (`te[2]`)**: 해당 자식 레이어의 식별자(Layer ID)

> [!NOTE]
> 계층 `TopoGeometry`의 경우, 모든 자식 `TopoGeometry` 요소들은 해당 토폴로지 레이어의 메타데이터(`topology.layer` 레코드)에 지정된 동일한 자식 레이어에 속해야 합니다.

---

### SQL 사용 예시

#### 1. TopoElement 생성 및 원소(ID, Type) 분리 추출
```sql
SELECT te[1] AS id, te[2] AS type
FROM (
  SELECT ARRAY[1, 2]::topology.topoelement AS te
) f;
```

**실행 결과:**
```text
 id | type 
----+------
  1 |    2
```

---

#### 2. 단순 배열 캐스팅
```sql
SELECT ARRAY[1, 2]::topology.topoelement;
```

**실행 결과:**
```text
  te   
-------
 {1,2}
```

---

#### 3. 차원 제약 조건(CHECK Constraint) 위반 예시
`topology.topoelement` 도메인은 반드시 2개의 원소를 가져야 합니다. 3개 이상의 원소를 가진 배열을 캐스팅하려고 하면 차원 제약 조건 위반 오류가 발생합니다:

```sql
SELECT ARRAY[1, 2, 3]::topology.topoelement;
```

**실행 결과:**
```text
ERROR:  value for domain topology.topoelement violates check constraint "dimensions"
```

---

### 관련 참고 함수 및 타입
- `GetTopoGeomElements`
- `TopoElementArray`
- `TopoGeometry`
- `TopoGeom_addElement`
- `TopoGeom_remElement`

---

## TopoElementArray

**`TopoElementArray`** — `TopoElement` 객체들의 배열(2차원 정수 배열, `integer[][2]`) 도메인입니다.

### 설명 (Description)
1개 이상의 `TopoElement`(`ARRAY[id, type]`) 객체들을 묶어 놓은 2차원 배열입니다. 일반적으로 여러 개의 토폴로지 구성 요소 목록을 함수에 전달하거나 반환할 때 사용됩니다.

- 모든 행(Row)은 정확히 2개의 열(ID와 Type)을 가져야 합니다 (`dimensions` 제약 조건).

---

### SQL 사용 예시

#### 1. 문자열 리터럴 캐스팅을 통한 생성
```sql
SELECT '{{1,2},{4,3}}'::topology.topoelementarray AS tea;
```

**실행 결과:**
```text
      tea       
----------------
 {{1,2},{4,3}}
```

---

#### 2. `ARRAY[...]` 중첩 문법을 통한 생성
```sql
SELECT ARRAY[ARRAY[1, 2], ARRAY[4, 3]]::topology.topoelementarray AS tea;
```

**실행 결과:**
```text
      tea       
----------------
 {{1,2},{4,3}}
```

---

#### 3. `TopoElementArray_Agg` 집계 함수 활용
토폴로지 확장에 내장된 전용 집계 함수 `topology.TopoElementArray_Agg`를 사용하여 여러 행의 데이터를 하나의 `TopoElementArray`로 집계할 수 있습니다:

```sql
SELECT topology.TopoElementArray_Agg(ARRAY[e, t] ORDER BY e, t) AS tea
FROM generate_series(1, 4) AS e 
CROSS JOIN generate_series(1, 3) AS t;
```

**실행 결과:**
```text
                                       tea                                        
----------------------------------------------------------------------------------
 {{1,1},{1,2},{1,3},{2,1},{2,2},{2,3},{3,1},{3,2},{3,3},{4,1},{4,2},{4,3}}
```

---

#### 4. 잘못된 차원 입력 시 제약 조건 오류
원소의 길이가 2가 아닌 배열이 포함되면 `dimensions` 제약 조건에 의해 에러가 발생합니다:

```sql
SELECT '{{1,2,4},{3,4,5}}'::topology.topoelementarray AS tea;
```

**실행 결과:**
```text
ERROR:  value for domain topology.topoelementarray violates check constraint "dimensions"
```

---

### 관련 참고 함수 및 타입
- `TopoElement`
- `GetTopoGeomElementArray`
- `TopoElementArray_Agg`

---

| | [🏠 토폴로지 목차](README.md) | [9.3. 토폴로지 데이터 타입 (Topology Types) ➡️](02_topology_types.md) |
| :--- | :---: | ---: |
