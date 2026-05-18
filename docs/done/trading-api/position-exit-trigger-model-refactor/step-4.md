# Step 4 — Evaluator, Orchestrator, Grouped Order, and Quantity Cap

Assigned agent: fullstack-dev

## Working Directory

- Code worktree: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor`
- Service directory: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor\backend\trading-api`
- Orchestration docs root: `C:\Users\dw\Desktop\paper-trading\docs`

## Files to Read

- `CODEX.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/spec.md`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluator.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/result/TriggerDecision.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/order/OrderCommandService.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/OrderRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionRepository.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/Position.kt`
- Files changed in Steps 2 and 3.

## Open Questions

- None. Use `positionId + triggerType` grouping and pending-sell quantity cap.

## Confirmed Design Choices

- Evaluator returns fired trigger decisions and only varies comparison direction by trigger type.
- Simultaneous fired triggers are grouped by `positionId + triggerType`.
- One grouped order is created per group.
- Grouped order uses `orderGroupId`; individual trigger audit/idempotency uses `triggerId + version`.
- Automatic quantity is capped by current position quantity minus pending sell orders before applying summed exit ratio.
- State transitions are `ARMED -> TRIGGERED`, `FAILED`, or `SKIPPED` for order/processing failure and conflicts.

## Tasks

1. Refactor `TriggerDecision` to carry trigger id, trigger version, position id, trigger type, effective trigger price, quote price, quote time, and exit ratio percent.
2. Refactor `PositionExitTriggerEvaluator` to evaluate a single ARMED trigger against a position and quote.
3. Implement effective trigger price handling for `FIXED_PRICE`, `AVG_PRICE_AT_CREATION`, and `FOLLOW_AVG_PRICE`.
4. Refactor `PositionExitTriggerOrchestrator.onQuote(ticker, price, quoteAt)` to load candidate ARMED triggers for the ticker, lock affected position/trigger rows, evaluate, group, and process groups.
5. Preserve useful WIP locking behavior, but do not rely on the old one-row-per-position repository method.
6. Implement skip behavior for lock conflict, no orderable quantity, pending/manual sell conflict, and closed position.
7. Add or update repository support to calculate pending sell quantity for the account/ticker/position context using pending or partial sell orders.
8. Compute automatic order quantity from current position quantity minus pending sell quantity, then apply capped summed `exitRatioPercent`.
9. Update `OrderCommandService` with a grouped auto-exit order method signature that accepts ticker/market/account/quantity/orderGroupId and trigger audit inputs without depending on the old combined trigger entity.
10. Ensure duplicate grouped order creation treats existing order by deterministic idempotency key or `orderGroupId` as success.
11. Publish order creation failure notification on final failure and mark affected triggers `FAILED`.

## Acceptance Criteria

- One quote tick can fire multiple triggers and create exactly one order per `positionId + triggerType` group.
- No order quantity exceeds current position quantity minus pending sell orders.
- Lock/no quantity/manual sell conflicts produce `SKIPPED` with `skipReason`.
- Order/processing failure produces `FAILED`.
- Duplicate processing is idempotent and does not create duplicate grouped orders.
- Verification command:

```bash
cd backend/trading-api && ./gradlew compileKotlin
```

## Agent Return Protocol

Completion Report:
- Status: PASS | FAIL | BLOCKED
- Summary:
- Files modified:
- Test result:
- Blockers:
