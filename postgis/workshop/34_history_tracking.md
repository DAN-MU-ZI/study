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
