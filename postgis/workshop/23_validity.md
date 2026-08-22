# 23. 지오메트리 유효성 (Validity)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/validity.html>](https://postgis.net/workshops/postgis-intro/validity.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

"공간 쿼리를 실행할 때 왜 `TopologyException` 오류가 발생할까요?"라는 질문의 90% 이상은 **"입력 데이터의 지오메트리 중 하나 이상이 OGC 유효성(Validity) 규칙을 위반했기 때문"**입니다.

포인트나 단순 선형 객체는 유효성 위반이 거의 없지만, 폐곡선과 내부 구멍 구조를 가진 **폴리곤(Polygon / MultiPolygon)**의 경우 위상학적 유효성이 매우 중요합니다.

---

## 폴리곤 유효성 규칙 (OGC SFSQL 규격)

OGC의 `SFSQL` 표준에 따른 폴리곤의 유효성 조건은 다음과 같습니다.

1. **닫힌 링(Closed Rings)**: 폴리곤을 구성하는 모든 링(외곽 링 및 내부 구멍 링)의 시작점과 끝점 좌표는 정확히 일치해야 합니다.
2. **구멍의 포함성(Hole Containment)**: 모든 내부 구멍 링은 반드시 외곽 경계 링의 내부에 완전히 포함되어야 합니다.
3. **자가 교차 금지(No Self-Intersection)**: 링의 경계선이 자기 자신과 십자로 교차하거나 꼬여서는 안 됩니다(8자 형태의 보타이(Bow-tie) 형상 금지).
4. **링 간의 접촉 제한**: 내부 구멍 링과 외곽 링은 한 점에서만 접할 수 있으며, 선분을 공유하며 겹쳐서는 안 됩니다.
5. **멀티폴리곤 요소 간의 분리**: 멀티폴리곤을 이루는 개별 폴리곤 요소들은 서로 겹치거나 선분으로 맞닿아서는 안 됩니다.

이러한 규칙이 엄격히 준수되어야만 데이터베이스의 공간 연산 엔진(GEOS)이 면적 계산, 교차, 버퍼, 클리핑 알고리즘을 신뢰성 있게 고속으로 처리할 수 있습니다.

---

## 유효하지 않은 지오메트리의 문제: 8자(Figure-Eight) 폴리곤 예시

```text
POLYGON((0 0, 0 1, 1 1, 2 1, 2 2, 1 2, 1 1, 1 0, 0 0))
```

![경계가 자기 교차하여 유효하지 않은 8자형 폴리곤](validity/figure_eight.png)

위 그림처럼 중앙 $(1, 1)$ 좌표에서 스스로 꼬인 8자 형태의 폴리곤이 있다고 가정해 보겠습니다. 1×1 크기의 정사각형 2개로 이루어져 있으므로 시각적으로는 총 면적이 2가 되어야 할 것 같습니다.

하지만 PostGIS에서 면적을 계산해 보면 다음과 같은 결과가 나옵니다.

```sql
SELECT ST_Area(
  ST_GeometryFromText('POLYGON((0 0, 0 1, 1 1, 2 1, 2 2, 1 2, 1 1, 1 0, 0 0))')
);
```

```text
st_area
-------
      0
```

### 면적이 0이 되는 이유
표준 면적 계산 알고리즘은 링의 정점 회전 방향(시계 방향 vs 반시계 방향)을 기준으로 외적을 적분합니다. 8자 형태로 꼬인 폴리곤은 한쪽 루프는 양의 면적($+1$), 다른 쪽 루프는 음의 면적($-1$)으로 계산되어 서로 상쇄되므로 최종 면적이 0으로 계산됩니다.

---

## 유효성 검사 함수: ST_IsValid & ST_IsValidReason

수백만 건의 대용량 테이블에서 오류가 있는 지오메트리를 찾으려면 `ST_IsValid`와 `ST_IsValidReason`을 사용합니다.

- `ST_IsValid(geometry)`: 지오메트리가 유효하면 `TRUE`, 오류가 있으면 `FALSE`를 반환합니다.
- `ST_IsValidReason(geometry)`: 유효하지 않은 구체적인 이유와 문제가 발생한 오류 좌표 위치를 문자열로 반환합니다.

```sql
SELECT ST_IsValidReason(
  ST_GeometryFromText('POLYGON((0 0, 0 1, 1 1, 2 1, 2 2, 1 2, 1 1, 1 0, 0 0))')
);
```

```text
Self-intersection[1 1]
```

### 테이블 내의 유효하지 않은 폴리곤 전수 검사

```sql
SELECT name, boroname, ST_IsValidReason(geom)
FROM nyc_neighborhoods
WHERE NOT ST_IsValid(geom);
```

```text
     name     | boroname |             st_isvalidreason
--------------+----------+-----------------------------------------
 Howard Beach | Queens   | Self-intersection[597264.08 4499924.54]
 Corona       | Queens   | Self-intersection[595483.05 4513817.95]
 Steinway     | Queens   | Self-intersection[593545.57 4514735.20]
 Red Hook     | Brooklyn | Self-intersection[584306.82 4502360.51]
```

---

## 유효성 자동 복구: ST_MakeValid

오류가 있는 지오메트리를 OGC 표준을 준수하는 유효한 다각형(또는 멀티폴리곤)으로 자동 재구성하려면 **`ST_MakeValid(geometry)` 함수**를 사용합니다.

대표적인 예로 외곽 링이 안쪽으로 꺾여 스스로 맞닿아 있는 "바나나 폴리곤(Banana Polygon)"이 있습니다.

```text
POLYGON((0 0, 2 0, 1 1, 2 2, 3 1, 2 0, 4 0, 4 4, 0 4, 0 0))
```

![ST_MakeValid로 자가 접촉 폴리곤을 유효한 형상으로 복구한 바나나 예제](validity/banana.png)

```sql
SELECT ST_AsText(
  ST_MakeValid(
    ST_GeometryFromText('POLYGON((0 0, 2 0, 1 1, 2 2, 3 1, 2 0, 4 0, 4 4, 0 4, 0 0))')
  )
);
```

```text
POLYGON((0 0,0 4,4 4,4 0,2 0,0 0),(2 0,3 1,2 2,1 1,2 0))
```

`ST_MakeValid`는 맞닿은 지점을 분리하여 외곽 링과 내부 구멍 링을 가진 표준 OGC 폴리곤으로 자동 변환해 줍니다.

---

## 테이블 일괄 유효성 복구 실습

기존 원본 지오메트리를 백업 컬럼에 보존하면서 테이블 전체의 유효하지 않은 지오메트리를 일괄 수정하는 패턴입니다.

```sql
-- 원본 형상을 보존할 백업 컬럼 추가
ALTER TABLE nyc_neighborhoods
  ADD COLUMN geom_invalid geometry DEFAULT NULL;

-- 오류가 있는 지오메트리를 백업하고 ST_MakeValid로 복구
UPDATE nyc_neighborhoods
  SET geom_invalid = geom,
      geom = ST_MakeValid(geom)
  WHERE NOT ST_IsValid(geom);

-- 수정된 지오메트리 확인
SELECT name, ST_IsValid(geom) AS is_now_valid
FROM nyc_neighborhoods
WHERE geom_invalid IS NOT NULL;
```

---

## 함수 목록 (Function List)

- [ST_IsValid(geometry)](http://postgis.net/docs/ST_IsValid.html): 지오메트리가 OGC 위상 유효성 규칙을 준수하는지 검사하여 부울(Boolean) 값을 반환합니다.
- [ST_IsValidReason(geometry)](http://postgis.net/docs/ST_IsValidReason.html): 지오메트리가 유효하지 않은 구체적인 원인과 문제가 발생한 좌표 위치를 반환합니다.
- [ST_MakeValid(geometry)](http://postgis.net/docs/ST_MakeValid.html): 유효하지 않은 지오메트리의 정점과 링을 재구성하여 OGC 표준을 준수하는 유효한 지오메트리로 자동 복구합니다.


---

[← 이전](22_joins_advanced.md) · [목차](00_index.md) · [다음 →](24_equality.md)
