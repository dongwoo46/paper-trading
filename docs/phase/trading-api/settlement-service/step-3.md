# Step 3: Testing and QA Verification
Assigned agent: Test Engineer

## Working Directory
`.worktrees/trading-api-settlement-service`

## Files to Read
- `CLAUDE.md`
- `docs/ADR.md`
- `docs/phase/trading-api/settlement-service/spec.md`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/util/BusinessDayCalculator.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/settlement/SettlementCommandService.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/scheduler/SettlementScheduler.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/ExecutionProcessor.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/domain/util/BusinessDayCalculatorTest.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/settlement/SettlementCommandServiceTest.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/order/ExecutionProcessorTest.kt`

## Tasks

Run all tests and verify the following:

1. Run the full test suite for trading-api:
```bash
cd .worktrees/trading-api-settlement-service/backend/trading-api
./gradlew test
```

2. Verify test coverage for each new file:
   - `BusinessDayCalculatorTest` — weekend skip cases, edge cases
   - `SettlementCommandServiceTest` — batch processing, single reprocess, failure isolation
   - `ExecutionProcessorTest` — LOCAL immediate settlement, KIS T+2 deferred settlement

3. Verify spec.md behaviour matches implementation:
   - LOCAL SELL → `account.receiveSellProceeds()` called immediately, no PendingSettlement created
   - KIS SELL → `account.receiveSellProceeds()` NOT called, PendingSettlement created with correct settlementDate
   - `SettlementCommandService.processSettlements()` → completes PENDING records, writes AccountLedger(SETTLEMENT)
   - Batch failure isolation: one failed record does not stop the rest

4. If any test fails, report the failure details. Do not fix implementation — report as FAIL with details.

## Acceptance Criteria

```bash
cd .worktrees/trading-api-settlement-service/backend/trading-api && ./gradlew test
```

All tests pass. No compilation errors.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <list of paths>
- Test result: <passed N/N | failed N — list failing cases>
- Blockers: <none | description>
---
