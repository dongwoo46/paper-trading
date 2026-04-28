# Settlement Service — 기능 명세

## 개요

주식 매도 체결 후 실제 결제 대금이 T+2 영업일에 계좌에 입금되는 규정을 시스템에 반영한다.
현재 `ExecutionProcessor`는 SELL 체결 즉시 `account.receiveSellProceeds()`를 호출하여 즉시 입금 처리한다.
이 기능은 그 흐름을 `PendingSettlement` 생성 → D+2 스케줄러 정산 완료 방식으로 교체한다.

---

## 설계 결정: Option C 채택

### 비교

| 옵션 | 설명 | 장점 | 단점 |
|------|------|------|------|
| **A** | ExecutionProcessor SELL 즉시 입금 제거 → PendingSettlement 생성 | 정확한 T+2 표현 | LOCAL 모드 UX 저하, 기존 테스트 다수 수정 필요 |
| **B** | 기존 즉시 입금 유지 + PendingSettlement 병렬 생성 | 코드 변경 최소 | 잔액 이중 계산 (BAD) |
| **C** | LOCAL 모드 즉시 입금 유지, KIS 모드(KIS_PAPER·KIS_LIVE)만 T+2 적용 | 모의투자 UX 유지, 실거래 정확성 | TradingMode 분기 필요 |

### 결정: Option C

**근거:**
1. LOCAL 모드는 모의투자(시뮬레이션) 환경이므로 즉시 자금 반영이 학습·테스트 UX에 적합하다.
2. KIS_PAPER·KIS_LIVE는 실제 증권사(한국투자증권) 결제 주기를 시뮬레이션해야 하므로 T+2가 필수다.
3. Option A는 기존 LOCAL 모드 체결 테스트 전체(ExecutionProcessorTest)를 대규모 수정해야 하며, 이는 회귀 위험을 높인다.
4. Option B는 이중 계산으로 절대 불가.

**구현 원칙:** `ExecutionProcessor.fill()` 내 SELL 분기에서 `account.tradingMode`로 KIS 여부를 판단한다.

---

## 아키텍처 계층

```
interfaces (controller)
    ↓
application (SettlementCommandService)
    ↓
domain (PendingSettlement, Settlement, Account)
    ↑
infrastructure (SettlementScheduler, Repositories)
```

---

## 도메인 모델 (기존 — 수정 없음)

이미 구현된 엔티티:
- `PendingSettlement` — PENDING → COMPLETED 상태 머신
- `Settlement` — FILLED 주문 1건당 1건, 실현손익·수수료·세금 확정
- `SettlementExecution` — Settlement ↔ Execution N:M 조인

---

## TransactionType 추가

`SETTLEMENT` 타입은 이미 `TransactionType` enum에 존재한다. 추가 불필요.

---

## 신규 구현 목록

### 1. SettlementCommandService (신규)

**패키지**: `com.papertrading.api.application.settlement`

**책임:**
- 만기 도래 PENDING 정산을 배치 처리한다.
- 단건 재처리를 지원한다.
- 한 트랜잭션 내에서 `account.receiveSellProceeds()` → `pendingSettlement.complete()` → `AccountLedger(SETTLEMENT)` 기록을 원자적으로 수행한다.

**메서드 시그니처:**
```kotlin
@Service
class SettlementCommandService(
    private val pendingSettlementRepository: PendingSettlementRepository,
    private val accountRepository: AccountRepository,
    private val accountLedgerRepository: AccountLedgerRepository,
) {
    @Transactional
    fun processSettlements(targetDate: LocalDate): Int

    @Transactional
    fun processSettlement(pendingSettlementId: Long)
}
```

**processSettlements 흐름:**
1. `pendingSettlementRepository.findBySettlementDateLessThanEqualAndStatus(targetDate, PENDING)` 조회
2. 각 PendingSettlement에 대해 `processOne(pendingSettlement)` 호출
3. 성공 건수 반환

**processOne 흐름:**
1. `accountRepository.findByIdWithLock(accountId)` — 비관적 락 획득
2. `account.receiveSellProceeds(pendingSettlement.amount)`
3. `pendingSettlement.complete()`
4. `accountLedgerRepository.save(AccountLedger(SETTLEMENT, amount, balanceAfter, idempotencyKey="settlement-${id}"))`

**idempotencyKey**: `"settlement-${pendingSettlement.id}"` — 중복 처리 방지

---

### 2. SettlementScheduler (신규)

**패키지**: `com.papertrading.api.infrastructure.scheduler`

**파일**: `SettlementScheduler.kt`

```kotlin
@Component
class SettlementScheduler(
    private val settlementCommandService: SettlementCommandService,
) {
    @Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Seoul")
    fun runDailySettlement() { ... }
}
```

- 매 영업일 KST 16:30 실행 (장 마감 후)
- `settlementCommandService.processSettlements(LocalDate.now(ZoneId.of("Asia/Seoul")))` 호출
- 처리 결과(건수)를 로그로 남긴다

---

### 3. ExecutionProcessor 수정 (기존 파일)

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt`

**변경 내용 — SELL 분기:**

KIS 모드(`KIS_PAPER`, `KIS_LIVE`)의 경우:
- `account.receiveSellProceeds()` 호출 제거
- `AccountLedger(SELL_EXECUTE)` 기록 유지 (체결 기록은 남긴다)
- `PendingSettlement` 생성 (settlementDate = 체결일 + 2 영업일)
- `Settlement` + `SettlementExecution` 레코드 생성 (FILLED 시)

LOCAL 모드의 경우:
- 기존 동작 유지 (`account.receiveSellProceeds()` 즉시 호출)

**영업일 계산**: `BusinessDayCalculator` 유틸 클래스 신규 작성 (공휴일 미고려, 단순 주말 스킵)

**추가 의존성:**
```kotlin
class ExecutionProcessor(
    // 기존 의존성 유지
    private val pendingSettlementRepository: PendingSettlementRepository,
    private val settlementRepository: SettlementRepository,
    private val settlementExecutionRepository: SettlementExecutionRepository,
)
```

---

### 4. BusinessDayCalculator (신규 유틸)

**패키지**: `com.papertrading.api.domain.util`

```kotlin
object BusinessDayCalculator {
    fun addBusinessDays(date: LocalDate, days: Int): LocalDate
}
```

- 토요일(SATURDAY), 일요일(SUNDAY)를 스킵하여 N 영업일 후 날짜 계산
- 공휴일은 MVP에서 미지원 (추후 확장 포인트 주석 명시)

---

## T+2 정산 흐름 (KIS 모드)

```
SELL 체결 (ExecutionProcessor.fill)
    ├─ position.applySell(fillQty)
    ├─ AccountLedger(SELL_EXECUTE) 기록 ← 체결 사실 기록 유지
    ├─ PendingSettlement 생성 (status=PENDING, settlementDate=T+2)
    └─ (FILLED 시) Settlement + SettlementExecution 생성

D+2 영업일 16:30 (SettlementScheduler)
    └─ SettlementCommandService.processSettlements(today)
           ├─ account.receiveSellProceeds(amount)  ← 실제 예수금 입금
           ├─ pendingSettlement.complete()
           └─ AccountLedger(SETTLEMENT) 기록
```

---

## 영업일 계산 정책

- 주말(토·일) 스킵, 공휴일은 MVP에서 미고려
- KST 기준 날짜 사용 (`ZoneId.of("Asia/Seoul")`)
- 스케줄러는 `LocalDate.now(ZoneId.of("Asia/Seoul"))` 사용

---

## 에러 처리

- `processSettlements` 내 단건 처리 실패 시: `runCatching` + warn 로그, 다음 건 계속 처리
- 단건 재처리(`processSettlement`)는 예외 전파 (호출자 책임)
- 이미 COMPLETED인 PendingSettlement에 `complete()` 재호출 시: `IllegalStateException` 도메인 예외 발생 → ControllerAdvice 처리

---

## 테스트 전략

| 레이어 | 테스트 대상 | 방법 |
|--------|------------|------|
| 도메인 | `BusinessDayCalculator` | 단위 테스트 (주말 스킵 케이스) |
| 애플리케이션 | `SettlementCommandService` | MockK 단위 테스트 |
| 애플리케이션 | `ExecutionProcessor` (KIS 분기) | MockK 단위 테스트 |
| 인프라 | `SettlementScheduler` | MockK + `@Scheduled` cron 표현식 검증 |

---

## 파일 생성/수정 요약

| 구분 | 파일 | 유형 |
|------|------|------|
| 신규 | `domain/util/BusinessDayCalculator.kt` | 신규 |
| 신규 | `application/settlement/SettlementCommandService.kt` | 신규 |
| 신규 | `infrastructure/scheduler/SettlementScheduler.kt` | 신규 |
| 수정 | `application/order/ExecutionProcessor.kt` | 수정 (KIS T+2 분기) |
| 신규 (테스트) | `domain/util/BusinessDayCalculatorTest.kt` | 신규 |
| 신규 (테스트) | `application/settlement/SettlementCommandServiceTest.kt` | 신규 |
| 수정 (테스트) | `application/order/ExecutionProcessorTest.kt` | 신규 (KIS 분기 케이스 추가) |
