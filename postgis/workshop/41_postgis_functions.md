# 41. 부록 A: PostGIS 함수 목록 (Appendix A: PostGIS Functions)

> 공식 원문: [<https://postgis.net/workshops/postgis-intro/postgis-functions.html>](https://postgis.net/workshops/postgis-intro/postgis-functions.html)\
> 공식 소스의 본문·표·SQL·이미지를 현재 페이지 순서대로 반영했습니다.

## 생성자

`ST_MakePoint(Longitude, Latitude)`\
새로운 포인트를 반환합니다. 좌표 순서(경도, 위도)를 확인하세요.

`ST_GeomFromText(WellKnownText, srid)`\
표준 WKT 문자열 및 srid에서 새 기하학을 반환합니다.

`ST_SetSRID(geometry, srid)`\
형상의 srid를 업데이트합니다. 동일한 기하학을 반환합니다. 이는 지오메트리의 좌표를 변경하지 않고 srid만 업데이트합니다. 이 기능은 Srid 없이 생성된 형상을 조절하는 데 유용합니다.

`ST_Expand(geometry, Radius)`\
입력 도형의 확장된 경계 상자인 새 도형을 반환합니다. 이 기능은 색인 검색에 사용할 봉투를 만드는 데 유용합니다.

## 출력

`ST_AsText(geometry)`\
사람이 읽을 수 있는 텍스트 형식으로 도형을 반환합니다.

`ST_AsGML(geometry)`\
표준 OGC `GML` 형식으로 도형을 반환합니다.

`ST_AsGeoJSON(geometry)`\
도형을 표준 [GeoJSON](http://geojson.org) 형식으로 반환합니다.

## 측정

`ST_Area(geometry)`\
공간 참조 시스템의 단위로 도형의 영역을 반환합니다.

`ST_Length(geometry)`\
공간 참조 시스템의 단위로 도형의 길이를 반환합니다.

`ST_Perimeter(geometry)`\
공간 참조 시스템의 단위로 기하학의 둘레를 반환합니다.

`ST_NumPoints(linestring)`\
라인스트링의 정점 수를 반환합니다.

`ST_NumRings(polygon)`\
다각형의 링 수를 반환합니다.

`ST_NumGeometries(geometry)`\
도형 컬렉션의 도형 수를 반환합니다.

## 관계

`ST_Distance(geometry, geometry)`\
공간 참조 시스템의 단위로 두 Geometry 사이의 거리를 리턴합니다.

`ST_DWithin(geometry, geometry, radius)`\
도형이 서로의 반경 거리 내에 있으면 true를 반환하고, 그렇지 않으면 false를 반환합니다.

`ST_Intersects(geometry, geometry)`\
기하학이 서로 분리되지 않으면 true를 반환하고, 그렇지 않으면 false를 반환합니다.

`ST_Contains(geometry, geometry)`\
첫 번째 도형이 두 번째 도형을 완전히 포함하면 true를 반환하고, 그렇지 않으면 false를 반환합니다.

`ST_Crosses(geometry, geometry)`\
선이나 다각형 경계가 다른 선이나 다각형 경계와 교차하면 true를 반환하고, 그렇지 않으면 false를 반환합니다.


---

[← 이전](40_advanced_geometry_construction.md) · [목차](00_index.md) · [다음 →](42_glossary.md)