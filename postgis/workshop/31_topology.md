# 31. 토폴로지 (Topology)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/topology.html>](https://postgis.net/workshops/postgis-intro/topology.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

PostGIS는 **postgis_topology**라는 확장을 통해 SQL/MM SQL-MM 3 Topo-Geo 및 Topo-Net 3 사양을 지원합니다. 이 확장 기능이 제공하는 모든 기능과 유형은 [설명서: PostGIS 토폴로지](https://postgis.net/docs/Topology.html)에서 알아볼 수 있습니다. *postgis_topology* 확장에는 **topogeometry**라는 또 다른 종류의 핵심 공간 유형이 포함되어 있습니다. *topogeometry* 공간 유형 외에도 *위상*을 구축하고 위상을 채우는 기능을 찾을 수 있습니다.

토폴로지 사용을 시작하기 전에 다음과 같이 postgis_topology 확장을 설치해야 합니다:

```sql
CREATE EXTENSION postgis_topology;
```

확장을 설치하면 데이터베이스에 <span class="title-ref">topology</span>라는 새 스키마가 표시됩니다. <span class="title-ref">topology</span> 스키마는 데이터베이스의 모든 토폴로지를 카탈로그화합니다.

<span class="title-ref">topology</span> 스키마에는 두 개의 테이블과 토폴로지에 대한 모든 도우미 함수가 포함되어 있습니다.

- 토폴로지 - 데이터베이스의 모든 토폴로지와 토폴로지가 저장된 스키마를 나열합니다.
- 레이어 - TopoGeometry를 보유하는 데이터베이스의 모든 테이블 열을 나열합니다.

*layer* 테이블은 이전에 배운 <span class="title-ref">raster_columns</span>, <span class="title-ref">geometry_columns</span> 및 <span class="title-ref">geography_columns</span> 카탈로그와 매우 유사하지만 특히 지형기하학.

## 토폴로지 생성

토폴로지와 TopoGeometry는 정확히 무엇이며, 어떻게 관련되어 있나요? 설명하기 전에 [CreateTopology](https://postgis.net/docs/CreateTopology.html) 기능을 사용하여 NYC 토폴로지적으로 완벽한 데이터를 수용할 토폴로지를 생성하고 허용 오차를 0.5미터로 설정해 보겠습니다. 공간 참조 시스템은 State Plany NY 미터이므로 0.5는 미터 단위입니다.

```sql
SELECT topology.CreateTopology('nyc_topo', 26918, 0.5);
```

출력은 다음과 같습니다.

```sql
1
```

새 토폴로지에 할당된 ID는 무엇입니까? 위 명령을 실행하면 데이터베이스에 <span class="title-ref">nyc_topo</span>라는 새 스키마가 표시됩니다. 원하는 대로 토폴로지 이름을 지정할 수 있습니다. 내 규칙은 내 데이터베이스에 있는 다른 스키마와 구별하기 위해 끝에 <span class="title-ref">\_topo</span>를 추가하는 것입니다.

<span class="title-ref">topology.topology</span> 테이블을 탐색하면,

```sql
SELECT * FROM topology.topology;
```

다음 내용을 확인할 수 있습니다.

    id |   name    | srid  | precision | hasz
    ----+----------+-------+-----------+------
      1 | nyc_topo | 26918 |         0 | f
    (1 row)

## 토폴로지 및 TopoGeometry 저장

토폴로지는 PostgreSQL 데이터베이스에서 스키마로 구현됩니다. <span class="title-ref">nyc_topo</span> 스키마를 탐색하면 다음 테이블과 뷰가 표시됩니다.

- edge - 주로 SQL/MM 준수를 위해 edge_data에 대해 구축된 뷰입니다.\
  여기에는 <span class="title-ref">edge_data</span> 테이블 열의 하위 집합이 있습니다.

- edge_data - 토폴로지를 구성하는 모든 라인스트링을 포함합니다.

- 면 - edge_data에서 형성될 수 있는 모든 닫힌 표면 목록을 포함합니다.\
  여기에는 실제 형상이 포함되지 않고 대신 형상의 경계 상자만 포함됩니다.

- 노드 - 모든 모서리의 모든 시작점과 끝점은 물론 어떤 것에도 연결되지 않은 점(격리된 노드)을 포함합니다.

- 관계 - 지형기하학을 구성하는 토폴로지의 요소를 정의합니다.

그렇다면 지형기하학이란 무엇입니까? TopoGeometry는 토폴로지의 모서리, 면, 노드 및 기타 TopoGeometry로 구성된 기하학을 표현합니다.

지형기하학은 어디에 있나요? *관계* 테이블을 통해 토폴로지의 요소를 참조하는 다른 곳에 있습니다. <span class="title-ref">nyc_topo</span> 스키마에 TopoGeometry를 생성할 수 있지만 일반적인 규칙은 TopoGeometry가 있고 추적에 관심이 있을 수 있는 다른 종류의 데이터가 있는 다른 스키마에 다른 테이블을 정의하는 것입니다.

## TopoGeometry를 사용하는 이유는 무엇입니까?

TopoGeometry를 사용하면 데이터가 깔끔하게 연결되어 유지됩니다. 지형 기하학은 토지의 두 구획이 하나의 경계를 변경하더라도 서로 겹치지 않도록 하거나 도로를 형성하는 기하학을 변경할 때 도로가 계속 연결되어 있는지 확인하려는 지적 작업에 매우 유용합니다. 기하학은 자신만의 섬에 살고 있으며, 복제하고 변형할 수 있습니다. 기하학은 공간을 공유하는 다른 기하학에 대해 신경 쓰지 않고 평온합니다. 대조적으로 TopoGeometry는 위상의 규칙을 따릅니다. 이를 정의하는 가장자리, 노드, 면 또는 기타 지형 기하학이 없으면 존재할 수 없습니다. TopoGeometry는 단 하나의 토폴로지에 속합니다. TopoGeometry는 기하학의 관계형 모델이며 각 구성요소(모서리/면/노드)가 이동, 추가되는 등 하나의 TopoGeometry 모양이 아니라 공통 구성요소를 갖는 모든 TopoGeometry를 변경합니다.

데이터가 전혀 없는 <span class="title-ref">nyc_topo</span> 토폴로지가 있습니다. 이를 NYC 데이터로 채워보겠습니다. 토폴로지 모서리, 면 및 노드는 두 가지 주요 방법으로 생성할 수 있습니다.

- 토폴로지 기본 기능을 사용하여 Edge, Face, Node를 직접 생성할 수 있습니다.
- TopoGeometry를 생성하여 Edge, Face, Node를 형성할 수 있습니다. 형상에서 TopoGeometry가 생성되고 해당 좌표와 일치하는 가장자리, 면 또는 노드가 누락된 경우 프로세스의 일부로 새 가장자리, 면 및 노드가 생성됩니다.

## TopoGeometry 열 정의 및 TopoGeometry 생성

위상을 채우는 가장 일반적인 방법은 TopoGeometry를 만드는 것입니다. 먼저 이웃을 보관할 테이블을 만든 다음 [AddTopoGeometryColumn](https://postgis.net/docs/AddTopoGeometryColumn.html) 함수를 사용하여 TopoGeometry 열을 추가해 보겠습니다.

```sql
CREATE TABLE nyc_neighborhoods_t(boroname varchar(43), name varchar(67),
  CONSTRAINT pk_nyc_neighborhoods_t PRIMARY KEY(boroname,name) );
SELECT topology.AddTopoGeometryColumn('nyc_topo', 'public', 'nyc_neighborhoods_t',
  'topo', 'POLYGON') As  layer_id;
```

위의 출력은 다음과 같습니다.

    layer_id
    --------
    1

이제 테이블을 채울 준비가 되었습니다. 추가하기 전에 기하학이 유효한지 확인하는 것이 가장 좋습니다. 그렇지 않으면 SQLMM 기하학이 단순하지 않다는 오류가 발생합니다.

이제 유효한 항목을 추가하는 것부터 시작해 보겠습니다. 여기에 사용된 1은 이전 쿼리에서 생성된 layer_id를 나타냅니다. 레이어 ID를 모르는 경우 이후 예제에서 사용할 [FindLayer](https://postgis.net/docs/FindLayer.html) 기능을 사용하여 검색할 수 있습니다.

이러한 예에서는 [toTopoGeom](https://postgis.net/docs/toTopoGeom.html) 함수를 사용하여 기하학을 해당 지형 기하학으로 변환합니다. toTopoGeom 기능은 많은 장부를 처리합니다.

<span class="title-ref">toTopoGeom</span> 기능은 전달된 형상을 검사하고 필요에 따라 노드, 모서리 및 면을 토폴로지에 삽입하여 형상의 모양을 형성합니다. 그런 다음 이 새로운 TopoGeometry가 이러한 새 토폴로지 요소 및 기존 토폴로지 요소와 어떻게 관련되는지 정의하는 <span class="title-ref">relation</span> 테이블에 관계를 추가합니다. 어떤 경우에는 형상의 조각이 존재할 수도 있고 새 형상을 형성하기 위해 기존 조각을 분할해야 할 수도 있습니다.

```sql
INSERT INTO nyc_neighborhoods_t(boroname,name, topo)
SELECT boroname, name,  topology.toTopoGeom(geom, 'nyc_topo', 1)
  FROM nyc_neighborhoods
  WHERE ST_ISvalid(geom);
```

위 단계는 3~4초 정도 소요됩니다. 이제 유효하지 않은 항목을 추가해 보겠습니다.

```sql
INSERT INTO nyc_neighborhoods_t(boroname,name, topo)
SELECT boroname, name,  topology.toTopoGeom(
  ST_UnaryUnion(
    ST_CollectionExtract(
      ST_MakeValid(geom), 3)
      ), 'nyc_topo', 1)
  FROM nyc_neighborhoods
  WHERE NOT ST_ISvalid(geom);
```

위의 작업에는 약 300-400ms가 소요됩니다.

이제 토폴로지에 데이터가 있습니다. 빠른 확인을 통해 nyc_topo.edge, nyc_topo.node 및 nyc_topo.face에 다음 데이터가 있음을 알 수 있습니다.

```sql
SELECT 'edge' AS name, count(*)
  FROM nyc_topo.edge
UNION ALL
SELECT 'node' AS name, count(*)
  FROM nyc_topo.node
UNION ALL
SELECT 'face' AS name, count(*)
  FROM nyc_topo.face;
```

출력:

    name | count
    ------+-------
    edge |   580
    node |   396
    face |   218
    (3 rows)

이제 POLYGON 유형이고 *nyc_neighborhoods_t.topo* 열의 다른 지형학 모음인 *nyc_boros_t* 테이블에 *topo*라는 열을 정의하여 보로스가 이웃 모음에서 형성된다는 것을 선언적으로 표현할 수 있습니다.

```sql
CREATE TABLE nyc_boros_t(boroname varchar(43),
  CONSTRAINT pk_nyc_boros_t PRIMARY KEY(boroname) );
SELECT topology.AddTopoGeometryColumn('nyc_topo', 'public', 'nyc_boros_t',
  'topo', 'POLYGON',
    (topology.FindLayer('public', 'nyc_neighborhoods_t', 'topo')).layer_id
        ) AS  layer_id;
```

출력은 다음과 같습니다.

> ## 레이어_ID
>
> > 2
>
> (1행)

이 새 테이블을 채우기 위해 [CreateTopoGeom](https://postgis.net/docs/CreateTopoGeom.html) 함수를 사용합니다. 새로운 TopoGeometry를 형성하기 위해 Geometry로 시작하는 대신, CreateTopoGeom은 새로운 TopoGeometry를 정의하기 위한 기본 요소 또는 다른 TopoGeometry일 수 있는 기존 토폴로지 요소로 시작합니다.

```sql
INSERT INTO nyc_boros_t(boroname, topo)
SELECT n.boroname,
  topology.CreateTopoGeom('nyc_topo',
  3,  (topology.FindLayer('public', 'nyc_boros_t', 'topo')).layer_id ,
    topology.TopoElementArray_Agg( ARRAY[ (n.topo).id, (n.topo).layer_id ]::topoelement ) )
  FROM nyc_neighborhoods_t AS n
GROUP BY n.boroname;
```

그러면 뉴욕 자치구에 해당하는 5개의 레코드가 삽입됩니다.

> [!NOTE]
> PostGIS 3.4 이상을 사용하는 경우 새로운 형변환을 사용하여 TopoGeometry를 Topo요소로 형변환하고 위 예의 <span class="title-ref">topology.TopoElementArray_Agg( ARRAY\[ (n.topo).id, (n.topo).layer_id \]::topoelement ) )</span>를 더 짧은 값으로 바꿀 수 있습니다. <span class="title-ref">topology.TopoElementArray_Agg(n.topo::topoelement)</span>

pgAdmin에서 이를 보려면 다음과 같이 TopoGeometry를 Geometry로 캐스팅할 수 있습니다.

```sql
SELECT boroname, topo::geometry AS geom
 FROM nyc_boros_t;
```

출력은 다음과 같습니다.

![이미지](topology/boros_topogeom.png)

보기에도 데이터가 매우 복잡하게 흐트러져 있습니다. 각 지오메트리를 별개의 객체로 처리한 채 단순화와 여러 공간 처리를 반복하면 이런 문제가 생깁니다. 틈이 벌어지고 조각이 늘어나며, 인접한 영역이 서로 침범하게 됩니다.

운 좋게도 토폴로지를 사용하면 이러한 혼란을 정리하고 연결된 데이터를 깨끗하게 유지하는 데 도움이 될 수 있습니다.

토지 측량사 모자를 쓰고 질문해 봅시다. 만약 우리가 토지를 여러 구역(보로 또는 이웃)으로 나누어 각 구역이 다른 구역과 접할 수 있지만 어떤 공통된 구역도 공유해서는 안 된다면 구역이 공통 구역을 갖는 것이 타당합니까? 아니요, 말이 되지 않습니다. 그리고 여기에는 일부 지역이 둘 이상의 동네 또는 둘 이상의 자치구에 속한다는 데이터가 나와 있습니다.

먼저 보로스(boros)를 살펴보고 공통 요소를 공유하는 이웃을 찾아보겠습니다.

```sql
SELECT te, array_agg(DISTINCT b.boroname)
 FROM nyc_boros_t AS b, topology.GetTopoGeomelements(topo) AS te
 GROUP BY te
 HAVING count(DISTINCT b.boroname) > 1;
```

출력은 다음과 같습니다

    te    |     array_agg
    --------+-------------------
    {44,3}  | {Brooklyn,Queens}
    {51,3}  | {Brooklyn,Queens}
    {76,3}  | {Brooklyn,Queens}
    {114,3} | {Brooklyn,Queens}
    {117,3} | {Brooklyn,Queens}
    (5 rows)

이는 퀸즈와 브루클린이 국경 전쟁을 벌이고 있다는 것을 말해줍니다. 이 쿼리에서는 [GetTopoGeomElements](https://postgis.net/docs/GetTopoGeomElements.html) 함수를 사용하여 자치구 간에 공유되는 구성 요소를 선언적으로 검사합니다.

반환되는 것은 토폴먼트 세트입니다. topoelement는 첫 번째 숫자가 요소의 ID이고 두 번째 숫자가 요소의 레이어(또는 기본 유형)인 2개의 정수 배열로 표시됩니다. PostGIS GetTopoElements는 (1: 노드, 2: 가장자리, 3: 면)에 해당하는 유형 번호 1-3을 사용하여 지형기하학의 프리미티브를 반환합니다. 인근 지역 및 자치구에 대한 모든 지형 요소는 유형 3이며 이는 위상학적 면에 해당합니다. [ST_GetFaceGeometry](https://postgis.net/docs/ST_GetFaceGeometry.html)를 사용하여 다음과 같이 이러한 공유 얼굴의 시각적 표현을 얻을 수 있습니다.

```sql
SELECT te, t.geom, ST_Area(t.geom) AS area, array_agg(DISTINCT d.boroname) AS shared_boros
FROM nyc_boros_t AS d, topology.GetTopoGeomelements(topo) AS te
  , topology.ST_GetFaceGeometry('nyc_topo',te[1]) AS t(geom)
GROUP BY te, t.geom
HAVING count(DISTINCT d.boroname) > 1
ORDER BY area;
```

결과는 퀸즈와 브루클린 간의 국경 분쟁에 해당하는 5개의 행이 됩니다.

우리 동네를 살펴보면 비슷한 이야기를 볼 수 있지만 44개의 국경 분쟁이 있습니다.

```sql
SELECT te, t.geom, ST_Area(t.geom) AS area, array_agg(DISTINCT d.name) AS shared_d
FROM nyc_neighborhoods_t AS d, topology.GetTopoGeomelements(d.topo) AS te
  , topology.ST_GetFaceGeometry('nyc_topo',te[1]) AS t(geom)
GROUP BY te, t.geom
HAVING count(DISTINCT d.name) > 1
ORDER BY area;
```

자치구는 인근 지역의 집합체이기 때문에 인근 지역 국경 분쟁을 해결함으로써 자치구 문제를 해결할 수 있습니다.

이 문제를 해결할 수 있는 방법은 여러 가지가 있습니다. 우리는 사람들에게 자신이 어느 동네에 있다고 생각하는지 조사할 수도 있습니다. 아니면 면적이 가장 적은 동네나 가장 높은 입찰자에게 토지를 할당할 수도 있습니다.

TopoGeom_remElement\](<https://postgis.net/docs/TopoGeom_remElement.html>) 함수를 사용하여 TopoGeometry에서 요소 제거를 처리합니다. 그럼 다음과 같이 면적이 가장 많은 이웃에서 중복된 요소를 제거해 보겠습니다.

```sql
WITH to_remove AS (SELECT te, MAX( ST_Area(d.topo::geometry) ) AS max_area, array_agg(DISTINCT d.name) AS shared_d
  FROM nyc_neighborhoods_t AS d, topology.GetTopoGeomelements(d.topo) AS te
    , topology.ST_GetFaceGeometry('nyc_topo',te[1]) AS t(geom)
  GROUP BY te
  HAVING count(DISTINCT d.name) > 1)
  UPDATE nyc_neighborhoods_t AS d SET topo = TopoGeom_remElement(topo, te)
  FROM to_remove
  WHERE d.name = ANY(to_remove.shared_d)
    AND ST_Area(d.topo::geometry) = to_remove.max_area;
```

위의 결과 29개 동네가 업데이트 되었습니다. 인근 지역 및 자치구에 대한 국경 분쟁 쿼리를 다시 실행하면 더 이상 국경 분쟁이 없음을 알 수 있습니다.

우리는 여전히 집중적인 단순화로 인해 동네 사이에 빈 공간이 존재합니다. 이러한 문제는 [토폴로지 편집기 기능군](https://postgis.net/docs/Topology.html#Topology_Editing)을 사용하여 토폴로지를 직접 편집하거나 구멍을 채우고 이를 이웃에 할당하여 해결할 수 있습니다.


---

[← 이전](30_rasters.md) · [목차](00_index.md) · [다음 →](32_topology_base_types.md)
