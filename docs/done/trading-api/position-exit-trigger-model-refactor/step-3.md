# Step 3 — Command, Query, DTO, and Controller Refactor

Assigned agent: fullstack-dev

## Working Directory

- Code worktree: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor`
- Service directory: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor\backend\trading-api`
- Orchestration docs root: `C:\Users\dw\Desktop\paper-trading\docs`

## Files to Read

- `CODEX.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/spec.md`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/controller/PositionExitTriggerController.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/position/PositionExitTriggerDtos.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandService.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerQueryService.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/command/UpsertPositionExitTriggerCommand.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/result/PositionExitTriggerResult.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/result/EffectivePositionExitTriggerResult.kt`
- Files changed in Step 2.

## Open Questions

- None. Use the new API/DTO only; do not preserve the old singular combined DTO shape.

## Confirmed Design Choices

- Replace `PUT /api/positions/{positionId}/exit-trigger` and its old DTOs.
- New API manages individual triggers under plural `/exit-triggers`.
- Direct `triggerPrice` is supported only through `FIXED_PRICE`.
- `triggerPercent` can derive `triggerPrice` through `AVG_PRICE_AT_CREATION` or `FOLLOW_AVG_PRICE`.
- Query results come from position-level trigger rows; account defaults are not merged into the new model.

## Tasks

1. Replace command classes with single-trigger commands: `CreatePositionExitTriggerCommand`, `UpdatePositionExitTriggerCommand`, and `CancelPositionExitTriggerCommand`.
2. Replace result classes with one-trigger and trigger-list results. Include all fields listed in `spec.md`.
3. Refactor `PositionExitTriggerCommandService` to create, update, and cancel one trigger at a time.
4. Compute `triggerPrice` at create/update time when `priceBasisPolicy = AVG_PRICE_AT_CREATION`.
5. For `FOLLOW_AVG_PRICE`, store `triggerPercent` and policy; do not freeze `triggerPrice` as the source of truth.
6. Validate percent, price, and ratio ranges with `BigDecimal` only.
7. Refactor `PositionExitTriggerQueryService` to list triggers for a position and stop returning `EffectivePositionExitTriggerResult` for this endpoint.
8. Replace `PositionExitTriggerDtos.kt` with new request/response DTOs matching `spec.md`.
9. Replace controller methods with `POST`, `GET`, `PATCH`, and `DELETE` under `/api/positions/{positionId}/exit-triggers`.
10. Keep `AccountExitTriggerDefault` code compiling, but do not redesign its model.

## Acceptance Criteria

- No presentation or application class exposes the old combined `stopLossPercent + takeProfitPercent` trigger DTO shape for position triggers.
- Create/update/cancel operations lock the position or trigger where needed and reject closed positions.
- API validation returns local project exceptions/statuses for invalid combinations and stale versions.
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
