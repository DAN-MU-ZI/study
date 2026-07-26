# QA 계획

## 대상
- `03_system/02_Idempotency/00_baseline`
- `03_system/02_Idempotency/01_advanced`

## 목적
- 최종 QA 판정 기준은 advanced가 충족해야 하는 안정성 요구사항으로 통일한다.
- baseline에서 중복 승인 문제가 실제로 재현되는지 확인한다.
- advanced에서 같은 주문에 대한 중복 승인이 방지되는지 확인한다.
- 프런트 상태, 백엔드 상태, 산출물 구조가 서로 일치하는지 확인한다.

## 범위
- 백엔드 API 동작
- 프런트 주문 상태와 결제 이력 렌더링
- Docker 기반 백엔드 테스트
- Playwright 기반 E2E 검증
- 실행 후 남는 QA 산출물의 적절성 검토

## 범위 밖
- 실제 PG 연동
- `cust-001`을 넘는 다중 권한 모델
- 부하 테스트와 성능 벤치마크

## 공통 전제
- Docker Desktop이 실행 중이다.
- 새 상태가 필요하면 `POST /api/orders/next`를 사용한다.
- 증적 비교는 반드시 활성 `orderId` 기준으로 한다.
- baseline과 advanced는 독립 테스트로 취급한다.
- baseline의 해석도 advanced 안정성 요구사항을 기준으로 한다.
- 테스트 케이스 정의는 `qa/test-cases.csv` 한 곳에서 관리하고, 기대 결과도 비교형 문구로 같은 표 안에서 관리한다.
- Playwright는 사용자 조작으로 재현 가능한 시나리오만 검증한다.
- API 직접 호출 기반 동시성 검증은 백엔드 통합 테스트가 담당한다.

## baseline 검증 목표
- 같은 주문의 중복 승인 문제가 재현되어야 한다.
- 더블 클릭 또는 백엔드 통합 테스트의 동시 요청 시 결제 이력이 2건 이상 쌓여야 한다.
- 처리 중에도 결제 버튼이 계속 활성 상태여야 한다.
- 다음 주문으로 넘어가면 새 pending 상태에서 다시 재현 가능해야 한다.
- baseline 자동화 테스트는 이 취약점이 재현되면 통과로 간주한다.
- 하지만 QA 판정 기준에서는 advanced 안정성 요구사항 미충족으로 기록해야 한다.

## advanced 검증 목표
- 같은 주문은 멱등 단위로 취급되어 최대 1건만 승인되어야 한다.
- 새 주문 시작 시 멱등성 키가 다시 발급되어야 한다.

## 백엔드 통합 테스트에서 검증할 항목
- advanced: 같은 키와 같은 payload 결과 재사용
- advanced: 다른 키여도 같은 주문이면 같은 결과 재사용
- advanced: 같은 키와 다른 payload 거부
- advanced: 다른 키지만 같은 주문에 다른 payload 거부
- advanced: 선행 요청 실패의 동일 실패 재전달
- baseline: 동일 주문 동시 요청 시 중복 승인 발생

## 실행 순서
1. 검증할 대상을 하나 선택한다.
2. 대상 디렉터리에서 `docker compose up --build -d backend frontend`를 실행한다.
3. 같은 디렉터리에서 `docker compose run --rm backend-test`를 실행한다.
4. `qa/tests`에서 대상에 맞는 Playwright 러너를 실행한다.
5. `qa/tests/test-results/<target>` 아래 결과를 확인한다.

## 산출물 기준
- 대상별 요약 CSV가 남아야 한다.
- 요약 CSV는 대상과 무관하게 같은 열 구조와 같은 `TB-*` 케이스 ID 체계를 유지해야 한다.
- 요약 CSV는 자동화 실행 결과와 QA 판정을 분리해 보여줘야 한다.
- 테스트 케이스별 `tb-*` 디렉터리가 남아야 한다.
- 각 `tb-*` 디렉터리에는 최소 `result.json`과 `video.webm`이 있어야 한다.
- 원본 Playwright 산출물은 `.playwright-artifacts` 아래에 분리되어야 한다.
- 결과 파일은 UTF-8 기준으로 읽을 수 있어야 한다.
- Playwright 산출물은 사용자 경로 검증 결과만 담아야 한다.

## 검토 포인트
- UI의 현재 주문 상태와 `GET /api/orders/current`가 같은가
- UI 결제 이력과 `GET /api/payments?orderId=...`가 같은가
- advanced에서는 중복 승인 경고가 없어야 하는가
- baseline에서는 중복 승인 경고가 보여야 하는가
- 산출물 디렉터리 이름과 공용 테스트 케이스 표가 대응되는가

## 종료 조건
- 대상별 백엔드 테스트가 통과한다.
- 대상별 Playwright 스위트가 통과한다.
- 문서에 적은 기대 결과와 실제 산출물이 모순되지 않는다.
- 단, baseline의 "통과"는 중복 승인 취약점이 기대대로 재현되었다는 의미다.
- baseline의 QA 최종 판정은 advanced 기준 안정성 요구사항을 충족하지 못한 케이스에 대해 `FAIL`이어야 한다.
