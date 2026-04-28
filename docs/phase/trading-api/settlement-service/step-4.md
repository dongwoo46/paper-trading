# Step 4: Code Review
Assigned agent: Code Reviewer

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

## Tasks

Review the following files for correctness, security, and adherence to architecture rules:

1. `BusinessDayCalculator` — logic correctness, edge cases handled
2. `SettlementCommandService` — transaction boundary, lock acquisition order, idempotency key uniqueness, error isolation in batch
3. `SettlementScheduler` — cron expression correctness, timezone, no business logic in scheduler
4. `ExecutionProcessor` (modified) — KIS/LOCAL mode branching correctness, no double-counting risk, Settlement creation conditions

Check against:
- Architecture rules in CLAUDE.md (constructor injection only, no `!!`, `val` preference)
- spec.md Option C decision: LOCAL immediate, KIS deferred
- Account balance consistency: KIS SELL must not call `receiveSellProceeds()` before settlement date
- Idempotency: `idempotencyKey = "settlement-{id}"` prevents duplicate processing

Rate each finding: 🔴 must-fix / 🟡 should-fix / 🟢 suggestion

## Acceptance Criteria

No 🔴 findings. 🟡 findings documented and acknowledged.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <none>
- Blockers: <none | list of 🔴 findings>
---
