# 27. 인덱스 기반 클러스터링 (Clustering on Indices)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/clusterindex.html>](https://postgis.net/workshops/postgis-intro/clusterindex.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

데이터베이스의 쿼리 성능은 저장 장치(디스크/SSD/메모리)에서 필요한 데이터 페이지를 얼마나 빠르고 효율적으로 읽어오느냐에 직결됩니다.

데이터는 테이블에 삽입(INSERT)되는 순서대로 디스크 블록에 기록되기 때문에, 실제 물리적 저장 순서와 공간 쿼리에서 함께 조회되는 데이터의 지리적 인접성 사이에는 일치성이 없을 수 있습니다.

![image](screenshots/clustering1.jpg)

**클러스터링(Clustering)**은 함께 조회될 확률이 높은 레코드들을 디스크 및 메모리 페이지 상에서 물리적으로 서로 인접하게 정렬하여 저장하는 최적화 기법입니다.

![image](screenshots/clustering2.jpg)

---

## 1. 공간 인덱스(GiST) 기반 클러스터링

지도 화면(View Extent)을 확대하거나 공간 조인을 수행할 때, 특정 영역에 위치한 지리적 객체들은 항상 함께 조회됩니다.

따라서 2차원 공간 인덱스(R-Tree)가 정렬한 순서대로 테이블의 물리적 행들을 재배열하면 공간 검색 성능을 극대화할 수 있습니다.

```sql
-- 공간 인덱스(GiST) 순서에 맞춰 nyc_census_blocks 테이블 물리적 재정렬
CLUSTER nyc_census_blocks USING nyc_census_blocks_geom_idx;
```

이 명령을 실행하면 PostgreSQL은 `nyc_census_blocks_geom_idx` 공간 인덱스의 정렬 순서대로 테이블의 모든 행을 물리적으로 다시 기록합니다.

---

## 2. SSD 및 메모리 캐시 환경에서도 클러스터링이 필요할까?

최신 서버 환경은 고속 NVMe SSD를 사용하고 대용량 RAM 캐시를 운용합니다. 그렇다면 물리적 정렬이 여전히 의미가 있을까요?

**결론은 "매우 유효하다"입니다.**

![image](screenshots/clustering5.png)

최신 CPU와 운영체제는 RAM과 CPU 코어 사이에 L1, L2, L3 캐시 메모리를 두고 64바이트 단위의 캐시 라인(Cache Line)과 8KB 단위의 메모리 페이지로 데이터를 주고받습니다. 지리적으로 인접한 공간 데이터가 동일한 메모리 페이지에 모여 있으면, 한 번의 I/O로 연관된 수십 개의 피처가 CPU 캐시로 동시에 적재되어 **캐시 적중률(Cache Hit Ratio)**이 비약적으로 향상됩니다.

---

## 함수 목록 (Function List)

- [CLUSTER](https://www.postgresql.org/docs/current/sql-cluster.html): 지정된 인덱스의 정렬 순서에 따라 테이블의 물리적 행 저장 순서를 영구적으로 재배열합니다.


---

[← 이전](26_de9im.md) · [목차](00_index.md) · [다음 →](28_3d.md)
