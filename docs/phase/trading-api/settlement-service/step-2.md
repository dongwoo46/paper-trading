# Step 2: 구현 (TDD)
담당 에이전트: Full Stack Developer

## Working Directory
`.worktrees/trading-api-settlement-service`

---

## Files to Read

- `CLAUDE.md`
- `docs/ADR.md`
- `docs/phase/trading-api/settlement-service/spec.md`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/PendingSettlement.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Settlement.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/SettlementExecution.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/Account.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/model/AccountLedger.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/enums/TransactionType.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/enums/TradingMode.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PendingSettlementRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/SettlementExecutionRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/AccountRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/LocalMatchingScheduler.kt`

---

## Tasks (TDD: Red → Green 순서)

### 규칙
- 각 작업: 테스트 먼저(Red) → 구현(Green) → 컴파일 확인
- `val` 우선, `!!` 금지, 생성자 주입만 사용
- `BigDecimal` 연산 시 반드시 scale 지정 (`setScale(4, RoundingMode.HALF_UP)`)
- `require*` 로 입력 불변식 조기 검증
- spec.md의 설계 결정(Option C)을 반드시 준수

---

### 작업 1: BusinessDayCalculator (신규)

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/util/BusinessDayCalculator.kt`

- `object BusinessDayCalculator`
- `fun addBusinessDays(date: LocalDate, days: Int): LocalDate`
- 토·일 스킵. `days < 0` 시 `IllegalArgumentException`
- 공휴일 미지원 (MVP) — 추후 확장 포인트 주석 명시

**테스트**: `backend/trading-api/src/test/kotlin/com/papertrading/api/domain/util/BusinessDayCalculatorTest.kt`
- 월요일 +2 = 수요일
- 금요일 +2 = 다음주 화요일
- 목요일 +2 = 다음주 월요일
- 0일 추가 = 동일 날짜
- 음수 전달 시 예외

---

### 작업 2: SettlementCommandService (신규)

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementCommandService.kt`

**시그니처**:
```
class SettlementCommandService(
    pendingSettlementRepository,
    accountRepository,
    accountLedgerRepository,
)

fun processSettlements(targetDate: LocalDate): Int
fun processSettlement(pendingSettlementId: Long)
```

**processSettlements 로직** (spec.md 참조):
- `settlementDate <= targetDate AND status = PENDING` 인 건 조회
- 각 건에 `processOne()` — 실패 시 warn 로그 + 계속 진행
- 성공 건수 반환

**processOne 로직**:
- `accountRepository.findByIdWithLock()` — 비관적 락
- `account.receiveSellProceeds(amount)`
- `pendingSettlement.complete()`
- `AccountLedger(SETTLEMENT)` 저장, `idempotencyKey = "settlement-{id}"`

**테스트**: `backend/trading-api/src/test/kotlin/com/papertrading/api/application/settlement/SettlementCommandServiceTest.kt`
- PENDING 정산 처리 → 예수금 증가, status = COMPLETED
- 대상 없음 → 0 반환
- 계좌 미존재 → 예외 전파 (processSettlement 단건)
- ID 미존재 → NoSuchElementException
- 배치 중 단건 실패 → 나머지 계속 처리

---

### 작업 3: SettlementScheduler (신규)

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/SettlementScheduler.kt`

- `@Scheduled(cron = "0 30 16 * * MON-FRI", zone = "Asia/Seoul")`
- `LocalDate.now(ZoneId.of("Asia/Seoul"))` 기준 날짜
- 처리 건수 로그

---

### 작업 4: ExecutionProcessor 수정

**파일**: `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt`

**생성자에 추가**:
- `PendingSettlementRepository`
- `SettlementRepository`
- `SettlementExecutionRepository`

**SELL 분기 수정** (spec.md Option C 기준):
- `TradingMode.LOCAL` → 기존 동작 유지 (`account.receiveSellProceeds()` 즉시 호출)
- `KIS_PAPER` / `KIS_LIVE` → `receiveSellProceeds()` 제거, `PendingSettlement` 생성
  - `settlementDate = BusinessDayCalculator.addBusinessDays(LocalDate.now(KST), 2)`
  - `amount = fillPrice * fillQty - fee`

**FILLED + SELL 시 추가**:
- `Settlement` 생성 (`realizedPnl = grossProceeds - fee`, `tax = 0`)
- `SettlementExecution` 생성 (Settlement ↔ Execution 연결)

**테스트**: `backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt`
- LOCAL SELL → 즉시 입금, PendingSettlement 생성 안됨
- KIS_LIVE SELL → 즉시 입금 없음, PendingSettlement 생성됨

---

## Acceptance Criteria

```bash
cd .worktrees/trading-api-settlement-service/backend/trading-api
./gradlew compileKotlin
./gradlew compileTestKotlin
```

---

## Agent Return Protocol

When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
