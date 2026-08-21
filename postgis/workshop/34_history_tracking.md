# 34. 트리거를 활용한 변경 이력 추적 (Tracking Edit History using Triggers)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/history_tracking.html>](https://postgis.net/workshops/postgis-intro/history_tracking.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

운영 환경의 지리정보 데이터베이스에서는 데이터의 변경 이력(누가, 언제, 어디서, 어떻게 변경 또는 삭제했는지)을 투명하게 추적하고 과거 시점의 데이터를 복원할 수 있는 기능이 필수적입니다.

일부 GIS 소프트웨어는 클라이언트 프로그램 레벨에서 변경 이력을 관리하지만, 이는 데스크톱 편집 도구의 복잡성을 가중시키고 외부 SQL 쿼리나 웹 앱을 통한 변경 사항을 놓치기 쉽습니다.

PostgreSQL의 **트리거(Trigger)** 시스템과 **범위 타입(TSTZRANGE)**을 활용하면, 사용자가 QGIS, 웹 앱, 파이썬 스크립트, SQL CLI 등 어떤 도구를 사용해 직접 데이터를 수정하더라도 데이터베이스 차원에서 모든 변경 이력을 100% 누락 없이 자동으로 추적할 수 있습니다.

---

## 1. 타임스탬프 범위 타입 (TSTZRANGE)

이력 관리 테이블은 특정 레코드가 유효했던 시간 구간을 저장하기 위해 PostgreSQL의 **`TSTZRANGE` (타임스탬프 범위)** 타입을 사용합니다.

- 신규 생성된 현재 유효한 레코드의 유효 기간: `[생성시각, NULL)` (현재부터 미래까지 무한대)
- 삭제된 레코드의 유효 기간: `[생성시각, 삭제시각)`

```sql
-- 현재 시각부터 무한대까지 열린 범위 생성
SELECT tstzrange(current_timestamp, NULL);
```

```text
tstzrange
------------------------------------
["2026-08-21 14:49:40.910074+09",)
```

`@>` (포함) 연산자를 사용하면 특정 시점이 해당 유효 기간 내에 포함되는지 직관적으로 질의할 수 있습니다.

```sql
-- 10분 전부터 시작된 범위가 현재 시점을 포함하는지 검사 (TRUE)
SELECT tstzrange(current_timestamp - '10m'::interval, NULL) @> current_timestamp;
```

> [!TIP]
> `TSTZRANGE` 컬럼은 GiST 인덱스를 지원하므로, 공간 지오메트리 인덱스와 시간 범위 인덱스를 결합하여 시공간(Spatio-Temporal) 이력 쿼리를 초고속으로 처리할 수 있습니다.

---

## 2. 이력 추적 테이블 구축

`nyc_streets` 도로 테이블에 대한 이력 추적 시스템을 구축해 보겠습니다.

### 단계 1: 이력 테이블 및 인덱스 생성
원본 테이블의 모든 컬럼 외에 4개의 감사(Audit) 메타데이터 컬럼을 추가합니다.
- `hid`: 이력 테이블의 고유 기본 키
- `valid_range`: 레코드가 유효했던 시간 범위 (`TSTZRANGE`)
- `created_by`: 레코드를 생성/수정한 사용자 계정
- `deleted_by`: 레코드를 삭제한 사용자 계정

```sql
DROP TABLE IF EXISTS nyc_streets_history;
CREATE TABLE nyc_streets_history (
  hid SERIAL PRIMARY KEY,
  gid INTEGER,
  id FLOAT8,
  name VARCHAR(200),
  oneway VARCHAR(10),
  type VARCHAR(50),
  geom GEOMETRY(MultiLinestring, 26918),
  valid_range TSTZRANGE,
  created_by VARCHAR(32),
  deleted_by VARCHAR(32)
);

CREATE INDEX nyc_streets_history_geom_x
  ON nyc_streets_history USING GIST (geom);

CREATE INDEX nyc_streets_history_tstz_x
  ON nyc_streets_history USING GIST (valid_range);
```

### 단계 2: 현재 원본 테이블 데이터로 초기화
현재 활성 상태인 모든 도로를 이력 테이블의 시작점으로 적재합니다.

```sql
INSERT INTO nyc_streets_history
  (gid, id, name, oneway, type, geom, valid_range, created_by)
SELECT
  gid, id, name, oneway, type, geom,
  tstzrange(now(), NULL),
  current_user
FROM nyc_streets;
```

---

## 3. 이력 관리 트리거 함수 등록

원본 `nyc_streets` 테이블에서 발생하는 `INSERT`, `UPDATE`, `DELETE` 이벤트를 가로채어 이력 테이블에 기록하는 트리거를 작성합니다.

### 1) INSERT 트리거
새 레코드가 추가되면 이력 테이블에 `[current_timestamp, NULL)` 유효 범위로 새 행을 삽입합니다.

```plpgsql
CREATE OR REPLACE FUNCTION nyc_streets_insert() RETURNS trigger AS
$$
  BEGIN
    INSERT INTO nyc_streets_history
      (gid, id, name, oneway, type, geom, valid_range, created_by)
    VALUES
      (NEW.gid, NEW.id, NEW.name, NEW.oneway, NEW.type, NEW.geom,
       tstzrange(current_timestamp, NULL), current_user);
    RETURN NEW;
  END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER nyc_streets_insert_trigger
AFTER INSERT ON nyc_streets
  FOR EACH ROW EXECUTE PROCEDURE nyc_streets_insert();
```

### 2) DELETE 트리거
기존 레코드가 삭제되면, 이력 테이블의 활성 레코드의 유효 범위 종료 시각을 `current_timestamp`로 마감하고 삭제자 정보를 기록합니다.

```plpgsql
CREATE OR REPLACE FUNCTION nyc_streets_delete() RETURNS trigger AS
$$
  BEGIN
    UPDATE nyc_streets_history
      SET valid_range = tstzrange(lower(valid_range), current_timestamp),
          deleted_by = current_user
      WHERE valid_range @> current_timestamp AND gid = OLD.gid;
    RETURN OLD;
  END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER nyc_streets_delete_trigger
AFTER DELETE ON nyc_streets
  FOR EACH ROW EXECUTE PROCEDURE nyc_streets_delete();
```

### 3) UPDATE 트리거
기존 활성 레코드의 유효 범위를 마감(Delete)하고, 수정된 새 상태를 새로운 유효 범위(Insert)로 추가합니다.

```plpgsql
CREATE OR REPLACE FUNCTION nyc_streets_update() RETURNS trigger AS
$$
  BEGIN
    UPDATE nyc_streets_history
      SET valid_range = tstzrange(lower(valid_range), current_timestamp),
          deleted_by = current_user
      WHERE valid_range @> current_timestamp AND gid = OLD.gid;

    INSERT INTO nyc_streets_history
      (gid, id, name, oneway, type, geom, valid_range, created_by)
    VALUES
      (NEW.gid, NEW.id, NEW.name, NEW.oneway, NEW.type, NEW.geom,
       tstzrange(current_timestamp, NULL), current_user);

    RETURN NEW;
  END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER nyc_streets_update_trigger
AFTER UPDATE ON nyc_streets
  FOR EACH ROW EXECUTE PROCEDURE nyc_streets_update();
```

---

## 4. 데이터 수정 테스트 및 이력 확인

도로명을 'Cumberland Walk'에서 'Cumberland Wynde'로 변경해 봅니다.

```sql
UPDATE nyc_streets
SET name = 'Cumberland Wynde'
WHERE name = 'Cumberland Walk';
```

이력 테이블을 조회하면 이전 이름의 레코드는 마감 처리되고 새 이름의 레코드가 유효 상태로 등록된 것을 확인할 수 있습니다.

```sql
SELECT hid, gid, name, valid_range, created_by, deleted_by
FROM nyc_streets_history
WHERE name LIKE 'Cumberland W%';
```

---

## 5. 과거 시점 데이터 복원 및 시간 여행 뷰 (Time-Travel Views)

이력 테이블을 사용하면 "10분 전의 데이터 상태", "어제 자정 기준의 지도" 등 특정 시점 **T**의 데이터를 완벽하게 재구성할 수 있습니다.

```sql
-- 10분 전 시점의 도로망 상태를 재현하는 뷰 생성
CREATE OR REPLACE VIEW nyc_streets_ten_min_ago AS
SELECT *
FROM nyc_streets_history
WHERE valid_range @> (now() - '10min'::interval);
```


---

[← 이전](33_topology_topo_types.md) · [목차](00_index.md) · [다음 →](35_tuning.md)
