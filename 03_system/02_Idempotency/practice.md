# 멱등성(Idempotency) 시스템 구현기: AOP와 Redis를 활용한 결제 중복 방어

## 1. 들어가며: 이론의 청첩장을 실전의 코드로 엮어내기

이전 글(어디에서 멱등성을 보장해야 하는가)에서 우리는 왜 프런트엔드나 DB가 아닌 **API 레이어**에서 멱등성을 통제해야 하는지 구조적 논리를 탐구했다. 더불어 네트워크 재시도를 묶는 `Idempotency-Key`와 결제 대상을 정의하는 `cartId`의 서로 다른 역할을 어떻게 구분해야 하는지도 살펴보았다.

하지만 이론적으로 완벽에 가까운 아키텍처 다이어그램이라 할지라도, 이를 실제 프레임워크와 플랫폼 인프라의 코드로 번역하는 일은 수많은 난제를 수반한다. 멀티 펫(Multi-pet) 서버 환경에서의 동시 접근 제어, 멱등성 로직과 도메인 로직의 결합도 최소화, 그리고 시스템 전체에 걸친 견고한 다중 방어막 구축까지. 

본 문서에서는 이 쉽지 않은 허들들을 넘어서며 실습 프로젝트에서 멱등성을 어떻게 코드 라인 아래로 끌어내려 구현했는지, 그 치열한 엔지니어링 과정을 차분히 복기해 보려 한다.

## 2. AOP 전략: 비즈니스 로직과 인프라 로직의 우아한 격리

API 레이어에서 HTTP 요청이 도달하는 가장 첫 관문에서 요청을 가로채기 위해, 우리는 **Spring AOP(Aspect-Oriented Programming)** 에 주목했다. 

만약 멱등성 검증, 분산 락 획득, 캐시 조회 등의 '인프라적인 로직'을 `PaymentService` 핵심 비즈니스 메서드 내부에 욱여넣는다면 어떨까? 서비스 코드는 기술 인프라 코드에 오염되어 결제 비즈니스의 본질을 알아보기 힘든 스파게티 시스템으로 전락할 것이다. AOP 프로그래밍 모델은 횡단 관심사(Cross-cutting Concerns)인 멱등성을 우아하게 분리하게 해준다.

```java
// PaymentService.java
// 눈에 띄게 간결해진 내부 결제 로직. 
// @Idempotent 애노테이션 하나가 선언적 멱등성을 보장한다.
@Idempotent
public PaymentDto.Response processPayment(PaymentDto.Request request) {
    validate(request);
    
    // 도메인 레벨 상태 검증
    requirePendingCart(request.cartId());
    
    // 외부 부작용 (PG사 승인)
    PgGateway.PgApprovalResult approval = pgGateway.approve(request.cartId(), request.amount());
    PaymentAttemptRecord paymentAttempt = createPaymentAttempt(request, approval, Instant.now());
    cartStore.markPaid(request.cartId(), paymentAttempt.paymentId(), paymentAttempt.pgTransactionId());
    
    return toResponse(paymentAttempt);
}
```

실제 마법은 백그라운드의 어드바이스(`@Around`)에서 벌어진다. `IdempotencyAspect`는 컨트롤러에서 서비스로 진입하는 과정을 막아서고, 캐시 계층을 먼저 들여다본 후 크게 세 가지 우회로를 설정한다.
1. **캐시 미스(초도 요청)**: 아무도 선점하지 않은 요청이므로 락(Lock)을 쥐어주고 핵심 비즈니스 로직으로의 통과를 허락한다.
2. **캐시 히트(재시도된 결과 존재)**: 과거에 정상/비정상으로 처리 완료된 캐시가 있다면, 굳이 무거운 서비스 트리를 태울 것 없이 이전 응답(Response Object)을 발췌해 그대로 반환(Return)해 버린다.
3. **In-Flight(처리 중 상태)**: 누군가가 락을 쥐고 여전히 외부 결제를 진행하고 있는 찰나의 순간이다. 이 후행 요청들은 선행 요청의 결괏값이 나올 때까지 대기(Wait)하거나 충돌(HTTP 409 Conflict) 로 빠르게 롤백 처리된다.

## 3. 분산 락(Distributed Lock)과 Lua Script: 동시성의 카오스를 잠재우다

AOP가 트래픽의 물줄기를 틀어주는 수문장이라면, 그 이면의 상태 정보와 잠금을 안정적으로 관리하는 거대한 저수지는 **Redis**가 도맡는다. 인스턴스가 10개가 떠 있든 100개가 떠 있든, 클러스터링된 Redis 안에서 오직 하나의 트랜잭션만이 무결성을 획득한다.

특히 수 밀리초 차이로 동일한 API가 연달아 호출되는 그 극악무도한 '비동기 따닥' 환경에서는 치밀한 락(Lock) 제어가 필요하다. 

```java
private static final String LOCK_PREFIX = "idempotency:lock:cart:";

private String tryAcquireLock(String cartId) {
    String ownerToken = UUID.randomUUID().toString();
    // SETNX 명령어의 래퍼로, 만약 키가 존재하지 않을 때만 성공(Atomic Transaction)
    Boolean success = redisTemplate.opsForValue().setIfAbsent(
        LOCK_PREFIX + cartId, ownerToken, Duration.ofMinutes(1)
    );
    return Boolean.TRUE.equals(success) ? ownerToken : null;
}
```

그러나 진정한 동시성 관리의 난제는 언제나 '해제 과정(Unlock)'에 숨어있다. 서버가 락을 쥐고 결제를 시도하던 중, Out Of Memory로 갑작스레 재부팅되었다고 상상해보라. 락은 수동으로 해제될 기회를 영원히 박탈당하고 재시도되는 모든 요청은 데드락(Deadlock)의 수렁에 빠질 것이다. 이를 막기 위해 우리는 락에 1분의 타임아웃 레이어를 둘렀다.

더불어 락 해제 시점에는 자신이 발급받았던 `ownerToken`을 대조하는 검증을 거치게 했다. 내가 발급받지 않은 락을 다른 자격 없는 노드가 해제하지 못하도록, 원자적 실행을 완전히 보장해주는 **Redis Lua Script**를 이용해 강결합된 Unlock 로직을 완성시켰다. 

## 4. 심층 방어 체계(Defense in Depth): 견고한 안전장치의 앙상블

우리는 API 레이어라는 주력 함선 엔진에 AOP와 Redis라는 든든한 날개를 달았다. 그러나 100% 무결한 시스템이란 존재하지 않으므로, 단일 실패 지점(SPOF)에 기대지 않고 시스템 전반의 겹겹 방어 구조, 이른바 심층 방어(Defense in Depth)를 세밀하게 직조했다.

### 4.1. 프런트엔드 (UX): 최초 파도의 분산
당연하게도 가장 첫 지점, 클라이언트 사이드에서는 동일한 장바구니에 대해 단일 `Idempotency-Key` 상태를 리액트 라이프사이클 안에서 유지한다. 사용자 액션이 시작되는 즉시 `submitting` 상태로 전환을 걸어 UI 컴포넌트 전체를 얼려버린다(Disable). 네트워크 해킹 방지보단 선량한 사용자의 반복 클릭에 따른 불필요한 트래픽 오버헤드를 서버 도달 전에 제거해주는 비용 최적화의 첫걸음이다. 

### 4.2. 도메인 (Service): 비즈니스 룰 방어선
만시지탄이지만 Redis 캐시 클러스터가 전부 내려앉아 멱등성 우회가 실패했다고 가정하자. 요청이 비즈니스 레이어에 스며들었을 때도 안도할 방어막이 쳐져 있다.
```java
private void requirePendingCart(String cartId) {
    CartRecord cart = cartStore.get(cartId);
    if (!"PENDING".equals(cart.status())) {
        throw new CartNotPendingException(cartId); // 이미 결제된 카트는 Out!
    }
}
```
이미 승인을 받고 `PAID` 상태 전이가 완료된 장바구니라면, 멱등성 여부와 무관하게 시스템 도메인 차원의 근간 룰에 튕겨 나간다. 이로써 기술 식별자와 비즈니스 식별자의 이중 크로스 체크가 견고해진다.

### 4.3. 저장소 (DB): 최후의 무결성 보루
외부 PG사 호출이 기적적으로 두 번 성공해버렸다는 끔찍한 절망 속에 이르더라도 결제 이력 저장소는 마지막 등대를 밝힌다. 
```java
// 내부 구현상 ConcurrentMap.putIfAbsent (실 환경에선 DB의 Unique Index 제약)
PaymentAttemptRecord existing = recordsByCartId.putIfAbsent(record.cartId(), record);
if (existing != null) {
    throw new DuplicatePaymentAttemptException(record.cartId());
}
```
데이터베이스의 물리적 구조 혹은 동시성 컬렉션 자체의 고유 제약 속성을 활용해, 결제 로그 테이블에 단 1회만 트랜잭션이 커밋되도록 최후통첩을 꽂아 넣는다.

## 5. 우리의 검증: Baseline 비교 진단

지금까지 설계한 견고한 아키텍처가 실제로 아름답게 동작하는지 어떻게 검증할 수 있을까? 이를 위해 결제 비즈니스 로직은 동일하되 인프라 제어만 생략된 `00_baseline` 코드와 치밀한 설계가 탑재된 `01_advanced` 코드를 마주 세워 자동화 비교(QA Testing)를 실행했다.

- **00_baseline (통제되지 않은 파도)**: Playwright를 통한 E2E 테스트가 '따닥' 시나리오를 주입하자마자, 어김없이 컨트롤러가 동시에 비즈니스 레이어를 두들겼고 무방비한 PG 목(Mock) 호출이 2스택으로 쌓여버리는 치명적인 동시성 결함이 고스란히 노출되었다.
- **01_advanced (멱등성 구조의 완성)**: 동시다발적인 부하(Integration Test)와 E2E 매크로 테스트에서도 시스템은 꿈쩍하지 않았다. 첫 번째 요청이 락을 선점하는 순간 파생된 나머지 트래픽 찌꺼기들은 캐싱된 최초의 성공 응답만을 예쁘게 포장해서 돌아갈 뿐이었다. PG사로 나가는 네트워크 패킷은 오직 단 1회, 정확하게 제어되었다.

## 6. 결론: "견고하다는 것"의 증명

실무의 분산 아키텍처 환경에서는 비정상적인 입력이 예외가 아니라 상수다. 본 실습 결제 멱등성 시스템 구현은 단순히 기술 블로깅의 튜토리얼을 얹는 과정이 아니었다. "기필코 네트워크는 끊어지고, 사용자의 행동은 지극히 불확실하다"는 불신 위에서 프런트엔드부터 API 게이트웨이, 도메인 애플리케이션, 그리고 데이터베이스 최하단까지 일관된 무결성 철학을 뿌리내리는 지독한 설계의 여정이었다.

비즈니스와 무관한 동시성 제어 요소를 AOP의 배후로 내쫓고, Redis라는 외부 캐시와 Lua 스크립트로 잠금을 세밀하게 쥐고 풀면서 완성한 이 모델. 비로소 "두 번 결제되지 않는" 진정으로 굳건하고 안정적인 시스템을 코드 위에 물리적으로 증명해냈다.
