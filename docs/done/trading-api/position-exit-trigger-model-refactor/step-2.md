# Step 2 — Domain Model, Migration, and Repository Contracts

Assigned agent: fullstack-dev

## Working Directory

- Code worktree: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor`
- Service directory: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor\backend\trading-api`
- Orchestration docs root: `C:\Users\dw\Desktop\paper-trading\docs`

## Files to Read

- `CODEX.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/spec.md`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/PositionExitTrigger.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/enums/TriggerType.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/enums/TriggerState.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/enums/TriggerSkipReason.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/infrastructure/persistence/PositionExitTriggerRepository.kt`
- `backend/trading-api/src/main/resources/db/migration/V7__create_position_exit_triggers.sql`
- `backend/trading-api/src/main/resources/db/migration/V11__add_position_exit_trigger_skip_reason.sql`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/order/Order.kt`

## Open Questions

- None. Use the confirmed decisions in `spec.md`.

## Confirmed Design Choices

- One `PositionExitTrigger` row represents one exit condition.
- Development DB policy allows dropping existing combined rows and recreating the schema.
- `exitRatioPercent` default is explicit: `100.0000`.
- `AccountExitTriggerDefault` is out of scope except compile bridging.
- Keep useful WIP pieces only; replace the combined stop-loss/take-profit entity shape.

## Tasks

1. Replace `PositionExitTrigger` fields and domain methods with the single-condition model from `spec.md`.
2. Add `PriceBasisPolicy` enum with `FIXED_PRICE`, `AVG_PRICE_AT_CREATION`, and `FOLLOW_AVG_PRICE`.
3. Keep `TriggerState` as a single trigger state enum: `ARMED`, `TRIGGERED`, `CANCELED`, `FAILED`, `SKIPPED`.
4. Keep or extend `TriggerSkipReason` for lock conflict, no quantity, manual sell conflict, and closed position cases.
5. Define domain method signatures for creation/update/cancel/trigger/fail/skip transitions. Do not keep methods that accept separate stop-loss and take-profit fields.
6. Update Flyway migration files so a development database creates only the new single-condition `position_exit_triggers` schema.
7. Add order audit support for `orderGroupId` if missing in `Order` and the corresponding migration.
8. Update `PositionExitTriggerRepository` contracts for list by position, lock one trigger by id, find ARMED triggers by ticker, and lock ARMED triggers for a group when needed.
9. Remove repository methods that assume one trigger row per position.

## Acceptance Criteria

- The entity no longer has `stopLossPercent`, `takeProfitPercent`, `stopLossState`, `takeProfitState`, `triggeredBy`, or `enabled`.
- The schema supports multiple triggers per position and direct grouping by `positionId + triggerType`.
- Domain transitions reject invalid state changes and stale versions through the existing exception style.
- Migration files are coherent for a fresh development DB.
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
