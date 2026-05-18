# Step 5 — Test Coverage and Verification

Assigned agent: test-engineer

## Working Directory

- Code worktree: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor`
- Service directory: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor\backend\trading-api`
- Orchestration docs root: `C:\Users\dw\Desktop\paper-trading\docs`

## Files to Read

- `CODEX.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/spec.md`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandServiceTest.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluatorTest.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt`
- Production files changed in Steps 2, 3, and 4.

## Open Questions

- None. Tests must cover the confirmed single-condition model.

## Confirmed Design Choices

- Keep useful WIP tests only where they still validate locks, listener/orchestrator behavior, skip reasons, and idempotency.
- Rewrite tests that assume combined stop-loss/take-profit rows.
- Test-first is required for business logic changes where practical; this step must close any coverage gaps left by implementation steps.

## Tasks

1. Rewrite command service tests for creating single STOP_LOSS/TAKE_PROFIT triggers, default `exitRatioPercent`, direct and computed trigger prices, dynamic policy, invalid inputs, stale version rejection, and closed position rejection.
2. Rewrite evaluator tests for STOP_LOSS and TAKE_PROFIT comparison directions, equality boundaries, non-firing boundaries, and `FOLLOW_AVG_PRICE` recomputation.
3. Rewrite orchestrator tests for grouped order creation, separate groups by trigger type, exit ratio cap, pending sell subtraction, no available quantity, duplicate quote/idempotency, order failure, and skip reasons.
4. Add controller/DTO tests if this project has existing controller test patterns for similar APIs.
5. Remove or update tests that assert the old combined DTO/entity shape.
6. Run focused tests, then compile.

## Acceptance Criteria

- Tests fail against the old combined trigger model and pass against the new single-condition implementation.
- All confirmed state transitions and grouping rules are covered.
- No test uses floating-point values for money, prices, quantities, or percents.
- Verification commands:

```bash
cd backend/trading-api && ./gradlew test --tests "*PositionExitTrigger*"
cd backend/trading-api && ./gradlew compileKotlin
```

## Agent Return Protocol

Completion Report:
- Status: PASS | FAIL | BLOCKED
- Summary:
- Files modified:
- Test result:
- Blockers:
