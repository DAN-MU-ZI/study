# QA 작업공간

이 디렉터리는 `03_system/02_Idempotency` 실습의 공용 QA 작업공간이다.

구성
- `qa-plan.md`: 검증 목표, 범위, 실행 순서, 산출물 기준
- `test-data.md`: 시드 데이터, 요청 예시, 기대 결과
- `bug-report-template.md`: 재현 가능한 결함 보고서 템플릿
- `test-cases.csv`: 단일 QA 관점의 공용 테스트 케이스 표
- `tests/`: Playwright 실행 코드, Docker 러너, 가독용 산출물 생성 코드

검증 분리 원칙
- QA의 기준 명세는 `advanced`가 만족해야 하는 안정성 요구사항이다.
- baseline은 이 기준 명세를 일부러 만족하지 못하는 비교군이다.
- Playwright E2E는 실제 사용자 조작으로 재현 가능한 흐름만 다룬다.
- `page.request`처럼 브라우저 밖에서 직접 API를 때리는 검증은 E2E 범위로 보지 않는다.
- 같은 키 결과 재사용, 다른 payload 충돌, 실패 결과 재전달 같은 API 레벨 시나리오는 백엔드 통합 테스트가 담당한다.
- baseline 자동화는 "안전함"을 증명하는 테스트가 아니라 "취약점이 실제로 재현됨"을 증명하는 테스트다.
- 따라서 baseline 러너는 중복 승인 문제가 재현되면 통과하고, 재현되지 않으면 오히려 실패해야 한다.
- 다만 QA 산출물에서는 실행 성공 여부와 최종 QA 판정을 분리해 기록한다.
- baseline의 `TB-002`, `TB-003`은 advanced 기준 안정성 요구사항을 충족하지 못하므로, 재현에 성공해도 QA 판정은 `FAIL`로 기록한다.

검증 대상
- 기준 비교 버전: `03_system/02_Idempotency/00_baseline`
- 보호 적용 버전: `03_system/02_Idempotency/01_advanced`

현재 QA 규칙
- `reset` 엔드포인트에 의존하지 않는다.
- 새 상태가 필요하면 `POST /api/orders/next`로 다음 주문을 만든다.
- 증적 비교는 전체 결제 건수가 아니라 `orderId` 기준으로 한다.
- baseline과 advanced는 독립 테스트로 실행하되, 테스트 케이스 표는 하나의 QA 표에서 비교형 기대 결과로 관리한다.

권장 실행 순서
1. 검증할 대상을 하나 고른다.
2. 해당 대상의 Docker 서비스를 올린다.
3. 해당 대상의 백엔드 테스트를 실행한다.
4. `qa/tests`에서 대상에 맞는 Playwright 러너를 실행한다.
5. `qa/tests/test-results/<target>` 아래 산출물을 검토한다.

주요 명령어

```bash
# from 03_system/02_Idempotency/00_baseline
docker compose up --build -d backend frontend
docker compose run --rm backend-test

# from 03_system/02_Idempotency/qa/tests
docker compose run --rm playwright-baseline

# from 03_system/02_Idempotency/01_advanced
docker compose up --build -d backend frontend
docker compose run --rm backend-test

# from 03_system/02_Idempotency/qa/tests
docker compose run --rm playwright-advanced
```

산출물 위치
- baseline 요약: `qa/tests/test-results/00_baseline/qa-run-results.csv`
- advanced 요약: `qa/tests/test-results/01_advanced/qa-run-results.csv`
- baseline 가독용 증적: `qa/tests/test-results/00_baseline/tb-*`
- advanced 가독용 증적: `qa/tests/test-results/01_advanced/tb-*`
- Playwright 원본 산출물: `qa/tests/test-results/<target>/.playwright-artifacts`

요약 CSV 원칙
- baseline과 advanced는 실행 파일이 분리되어도, `qa-run-results.csv`의 열 구조와 테스트 케이스 ID 체계는 동일해야 한다.
- 기대 결과의 차이는 별도 CSV로 나누지 않고 `qa/test-cases.csv` 한 장에서 비교형 문구로 관리한다.
- `qa-run-results.csv`는 최소 `실행상태`와 `QA판정`을 구분해 제공해야 한다.

인코딩 기준
- QA 문서, 테스트 케이스 CSV, Playwright가 생성하는 CSV/JSON 산출물은 UTF-8 기준으로 다룬다.
- PowerShell에서 직접 확인할 때도 UTF-8을 전제로 읽고 저장한다.
