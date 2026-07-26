# 버그 리포트 템플릿

## 요약
- 짧은 결함 제목

## 환경
- 브랜치 / 커밋:
- 대상 버전:
- Docker 서비스:
- 브라우저 / Playwright 프로젝트:
- 날짜 / 시간대:

## 사전 조건
- 활성 주문 ID:
- Idempotency key:
- Payload:
- 시나리오 전에 `POST /api/orders/next`를 사용했는지:

## 재현 절차
1. ...
2. ...
3. ...

## 기대 결과
- ...

## 실제 결과
- ...

## 영향도
- 중복 승인 위험
- UI / 백엔드 상태 불일치
- 오류 전파 이상
- QA 진행 차단

## 증적
- 요약 CSV 경로:
- 가독용 산출물 디렉터리:
- 스크린샷 경로:
- 동영상 경로:
- 백엔드 응답 본문:
- `GET /api/orders/current` 결과:
- `GET /api/orders/{orderId}` 결과:
- `GET /api/payments?orderId={orderId}` 결과:

## 메모
- 동시성 타이밍 관련 정보
- 항상 재현되는지, 간헐 재현인지
