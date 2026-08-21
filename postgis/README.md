# PostGIS 한국어 종합 문서 보관소

본 저장소는 PostGIS 공식 학습 자료를 바탕으로 만든 한국어 학습 노트와 개발자 매뉴얼을 체계적으로 정리한 문서 저장소입니다.
독립된 두 개의 프로젝트로 구성되어 있습니다.

---

## 📂 프로젝트 구성

```
postgis/
├── workshop/   # 1. PostGIS 입문 워크숍 (Introduction to PostGIS)
└── manual/     # 2. PostGIS 공식 개발자 매뉴얼 (PostGIS Reference Manual)
```

---

### 1. [PostGIS 입문 워크숍 (`workshop/`)](workshop/README.md)
- **출처**: [https://postgis.net/workshops/postgis-intro/](https://postgis.net/workshops/postgis-intro/)
- **대상**: PostGIS를 처음 접하거나 실무 공간 SQL 쿼리를 학습하고자 하는 개발자
- **주요 내용**:
  - 뉴욕시(NYC) 실제 오픈 지리 데이터를 활용한 실습 중심 튜토리얼
  - 40개 본문 챕터 + 3개 부록 (총 43개 모듈)
  - 각 모듈의 핵심 개념과 SQL을 정리한 한국어 요약

👉 **[입문 워크숍 바로가기 (`workshop/README.md`)](workshop/README.md)**

---

### 2. [PostGIS 공식 개발자 매뉴얼 (`manual/`)](manual/README.md)
- **출처**: [https://postgis.net/docs/manual-dev/postgis-ko_KR.html](https://postgis.net/docs/manual-dev/postgis-ko_KR.html)
- **대상**: PostGIS의 모든 기능, 데이터 타입, 도메인, 수백 개의 공간 함수 상세 명세를 찾는 개발자
- **주요 내용**:
  - 토폴로지 도메인(`TopoElement`, `TopoElementArray`), 데이터 타입, 원시 테이블 스키마
  - 영문 혼용 및 기계 번역 오역을 완전 교정한 고품질 한국어 표준 기술 레퍼런스
  - 챕터별 독립된 모듈형 문서 구성

👉 **[공식 개발자 매뉴얼 바로가기 (`manual/README.md`)](manual/README.md)**
