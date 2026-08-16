# 34. 트리거를 활용한 변경 이력 추적 (Tracking Edit History using Triggers)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/history_tracking.html>](https://postgis.net/workshops/postgis-intro/history_tracking.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

프로덕션 데이터베이스에 대한 일반적인 요구 사항은 기록을 추적하는 기능입니다. 즉, 두 날짜 사이에 데이터가 어떻게 변경되었는지, 변경한 사람은 누구인지, 어디서 발생했는지 등을 추적하는 기능입니다. 일부 GIS 시스템은 클라이언트 인터페이스에 변경 관리를 포함시켜 변경 사항을 추적하지만 이로 인해 편집 도구에 **complexity**가 많이 추가됩니다.

데이터베이스와 트리거 시스템을 사용하면 기본 테이블에 대한 간단한 "직접 편집" 액세스를 유지하면서 모든 테이블에 기록 추적을 추가할 수 있습니다.

기록 추적은 모든 편집에 대해 기록하는 기록 테이블을 유지하여 작동합니다.

- 기록이 생성된 경우, 언제, 누가 추가했는지.
- 기록을 삭제한 경우, 언제, 누가 삭제했는지.
- 레코드가 업데이트된 경우 삭제 레코드(기존 상태)와 생성 레코드(새 상태)를 추가합니다.

## TSTZRANGE 사용하기

기록 테이블은 PostgreSQL 관련 기능("\`timestamp range \<[https://www.postgresql.org/docs/current/rangetypes.html\\\\\\](https://www.postgresql.org/docs/current/rangetypes.html\>\`\)\_" 유형)을 사용하여 기록 레코드가 "라이브" 레코드였던 시간 범위를 저장합니다. 특정 기능에 대한 기록 테이블의 모든 타임스탬프 범위는 겹치지 않지만 인접할 것으로 예상할 수 있습니다.

새 레코드의 범위는 `now()`에서 시작하고 끝이 열려 있으므로 범위는 현재 시간부터 미래까지의 모든 시간을 포괄합니다.

```sql
SELECT tstzrange(current_timestamp, NULL);
```

    tstzrange
    ------------------------------------
    ["2021-06-01 14:49:40.910074-07",)

마찬가지로, 삭제된 기록의 시간 범위는 현재 시간을 시간 범위의 끝점으로 포함하도록 업데이트됩니다.

시간 범위 검색은 타임스탬프 쌍을 검색하는 것보다 훨씬 간단합니다. 왜냐하면 열린 시간 범위가 시작점부터 무한대까지 모든 시간을 포함하는 방식 때문입니다. 범위에 대한 "포함" 연산자 `@>`가 우리가 사용할 연산자입니다.

```sql
-- Does the range of "ten minutes ago to the future" include now?
-- It should! :)
--
SELECT tstzrange(current_timestamp - '10m'::interval, NULL) @> current_timestamp;
```

아래에서 볼 수 있듯이 범위는 공간 데이터와 마찬가지로 GIST 인덱스를 사용하여 매우 효율적으로 인덱싱할 수 있습니다. 이는 기록 쿼리를 매우 효율적으로 만듭니다.

## 기록 테이블 구축

이 정보를 사용하면 언제든지 편집 테이블의 상태를 재구성할 수 있습니다. 이 예에서는 **nyc_streets** 테이블에 기록 추적을 추가합니다.

- 먼저 새로운 **nyc_streets_history** 테이블을 추가합니다. 이것은 모든 기록 편집 정보를 저장하는 데 사용할 테이블입니다. **nyc_streets**의 모든 필드 외에도 5개의 필드를 더 추가합니다.

  - **hid** 기록 테이블의 기본 키
  - **created_by** 레코드를 생성한 데이터베이스 사용자
  - **deleted_by** 레코드가 삭제된 것으로 표시되도록 만든 데이터베이스 사용자
  - **valid_range** 레코드가 "라이브"된 시간 범위

  실제로 기록 테이블의 레코드를 삭제하는 것이 아니라 편집 테이블의 현재 상태의 일부가 중단된 시간만 표시할 뿐입니다.

  ```sql
  DROP TABLE IF EXISTS nyc_streets_history;
  CREATE TABLE nyc_streets_history (
    hid SERIAL PRIMARY KEY,
    gid INTEGER,
    id FLOAT8,
    name VARCHAR(200),
    oneway VARCHAR(10),
    type VARCHAR(50),
    geom GEOMETRY(MultiLinestring,26918),
    valid_range TSTZRANGE,
    created_by VARCHAR(32),
    deleted_by VARCHAR(32)
  );

  CREATE INDEX nyc_streets_history_geom_x
    ON nyc_streets_history USING GIST (geom);

  CREATE INDEX nyc_streets_history_tstz_x
    ON nyc_streets_history USING GIST (valid_range);
  ```

- 다음으로 활성 테이블 **nyc_streets**의 현재 상태를 기록 테이블로 가져오므로 기록 추적의 시작점이 됩니다. 생성 시간과 생성 사용자는 입력하되, 종료 시간 범위와 삭제자 정보는 NULL로 남겨둡니다.

  ```sql
  INSERT INTO nyc_streets_history
    (gid, id, name, oneway, type, geom, valid_range, created_by)
     SELECT gid, id, name, oneway, type, geom,
       tstzrange(now(), NULL),
       current_user
     FROM nyc_streets;
  ```

- 이제 활성 테이블에 INSERT, DELETE 및 UPDATE 작업을 위한 세 가지 트리거가 필요합니다. 먼저 트리거 함수를 생성한 다음 이를 트리거로 테이블에 바인딩합니다.

  삽입의 경우 생성 시간/사용자와 함께 기록 테이블에 새 레코드를 추가하기만 하면 됩니다.

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
    $$
    LANGUAGE plpgsql;

  CREATE TRIGGER nyc_streets_insert_trigger
  AFTER INSERT ON nyc_streets
    FOR EACH ROW EXECUTE PROCEDURE nyc_streets_insert();
  ```

삭제의 경우 현재 활성 기록 레코드(삭제 시간이 NULL인 레코드)를 삭제된 것으로 표시합니다.

```plpgsql
CREATE OR REPLACE FUNCTION nyc_streets_delete() RETURNS trigger AS
  $$
    BEGIN
      UPDATE nyc_streets_history
        SET valid_range = tstzrange(lower(valid_range), current_timestamp),
            deleted_by = current_user
        WHERE valid_range @> current_timestamp AND gid = OLD.gid;
      RETURN NULL;
    END;
  $$
  LANGUAGE plpgsql;


CREATE TRIGGER nyc_streets_delete_trigger
AFTER DELETE ON nyc_streets
  FOR EACH ROW EXECUTE PROCEDURE nyc_streets_delete();
```

업데이트의 경우 먼저 활성 기록 레코드를 삭제된 것으로 표시한 다음 업데이트된 상태에 대한 새 레코드를 삽입합니다.

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
$$
LANGUAGE plpgsql;

CREATE TRIGGER nyc_streets_update_trigger
AFTER UPDATE ON nyc_streets
  FOR EACH ROW EXECUTE PROCEDURE nyc_streets_update();
```

## 테이블 편집하기

이제 기록 테이블이 활성화되었으므로 기본 테이블을 편집하고 기록 테이블에 로그 항목이 표시되는 것을 볼 수 있습니다.

기록에 대한 데이터베이스 기반 접근 방식의 강력한 점에 유의하십시오. **SQL 명령줄, 웹 기반 JDBC 도구, QGIS와 같은 데스크톱 도구 등 편집을 위해 어떤 도구를 사용하든 상관없이 기록은 일관되게 추적됩니다.**

### SQL 편집

"Cumberland Walk"라는 이름의 두 거리를 더욱 세련된 "Cumberland Wynde"로 바꿔보겠습니다.

```sql
UPDATE nyc_streets
SET name = 'Cumberland Wynde'
WHERE name = 'Cumberland Walk';
```

두 거리를 업데이트하면 원래 거리가 기록 테이블에 삭제된 것으로 표시되어 삭제 시간이 현재로 표시되고 새 이름이 추가된 두 개의 새로운 거리가 현재의 추가 시간으로 표시됩니다. 기록 기록을 검사할 수 있습니다.

```sql
SELECT * FROM nyc_streets_history
  WHERE name LIKE 'Cumberland W%';
```

## 기록 테이블 쿼리

이제 기록 테이블이 생겼으니 어떤 용도로 사용되나요? 시간여행에 유용해요! 특정 시간 **T**로 이동하려면 다음을 포함하는 쿼리를 생성해야 합니다.

- T 이전에 생성되었으며 아직 삭제되지 않은 모든 기록; 그리고 또한
- T 이전에 생성되었으나 **after** T를 삭제한 모든 레코드입니다.

이 논리를 사용하여 과거 데이터 상태에 대한 쿼리 또는 보기를 만들 수 있습니다. 아마도 모든 테스트 편집이 지난 몇 분 동안 발생했기 때문에 **편집을 시작하기 전**(즉, 원본 데이터) 10분 전의 테이블 상태를 표시하는 기록 테이블 뷰를 만들어 보겠습니다.

```sql
-- Records with a valid range that includes 10 minutes ago
-- are the ones valid at that moment.

CREATE OR REPLACE VIEW nyc_streets_ten_min_ago AS
  SELECT * FROM nyc_streets_history
    WHERE valid_range @> (now() - '10min'::interval)
```

특정 사용자가 추가한 내용만 보여주는 보기를 만들 수도 있습니다. 예를 들면 다음과 같습니다.

```sql
CREATE OR REPLACE VIEW nyc_streets_postgres AS
  SELECT * FROM nyc_streets_history
    WHERE created_by = 'postgres';
```

## 참고 항목

- [QGIS 오픈소스 GIS](http://qgis.org)
- [PostgreSQL 트리거](http://www.postgresql.org/docs/current/static/plpgsql-trigger.html)
- [PostgreSQL 범위 유형](https://www.postgresql.org/docs/current/rangetypes.html)

------------------------------------------------------------------------

<details markdown="1">
<summary><strong>기존 로컬 문서 보존본</strong> — 로컬 실습 확장·요약 내용</summary>

# 34. 트리거를 활용한 변경 이력 추적 (Tracking Edit History using Triggers)

GIS 데이터는 여러 편집자에 의해 실시간으로 수정되거나 삭제될 수 있습니다. PostgreSQL의 **트리거(Trigger)**를 사용하면 모든 지오메트리 변경 이력(누가, 언제, 어떤 좌표를 수정/삭제했는지)을 자동으로 완벽하게 기록할 수 있습니다.

---

## 1. 이력 저장용 히스토리 테이블 생성

```sql
CREATE TABLE nyc_streets_history (
  history_id SERIAL PRIMARY KEY,
  gid INTEGER,
  name VARCHAR,
  geom GEOMETRY(MultiLineString, 26918),
  action VARCHAR(10),       -- 'INSERT', 'UPDATE', 'DELETE'
  changed_by VARCHAR(50),   -- 수정한 DB 사용자 계정
  changed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## 2. 트리거 함수 정의

```sql
CREATE OR REPLACE FUNCTION track_street_changes()
RETURNS TRIGGER AS $$
BEGIN
  IF (TG_OP = 'DELETE') THEN
    INSERT INTO nyc_streets_history (gid, name, geom, action, changed_by)
    VALUES (OLD.gid, OLD.name, OLD.geom, 'DELETE', current_user);
    RETURN OLD;
  ELSIF (TG_OP = 'UPDATE') THEN
    INSERT INTO nyc_streets_history (gid, name, geom, action, changed_by)
    VALUES (OLD.gid, OLD.name, OLD.geom, 'UPDATE', current_user);
    RETURN NEW;
  ELSIF (TG_OP = 'INSERT') THEN
    INSERT INTO nyc_streets_history (gid, name, geom, action, changed_by)
    VALUES (NEW.gid, NEW.name, NEW.geom, 'INSERT', current_user);
    RETURN NEW;
  END IF;
  RETURN NULL;
END;
$$ LANGUAGE plpgsql;
```

---

## 3. 테이블에 트리거 연결

```sql
CREATE TRIGGER streets_history_trigger
AFTER INSERT OR UPDATE OR DELETE ON nyc_streets
FOR EACH ROW EXECUTE FUNCTION track_street_changes();
```

---

| [⬅️ 33. 토폴로지와 지오메트리 표현 (Topology and Geometry Representation)](33_topology_topo_types.md) | [🏠 워크숍 목차](README.md) | [35. PostgreSQL 기본 튜닝 (Basic PostgreSQL Tuning) ➡️](35_tuning.md) |
| :--- | :---: | ---: |

</details>

---

[← 이전](33_topology_topo_types.md) · [목차](00_index.md) · [다음 →](35_tuning.md)
