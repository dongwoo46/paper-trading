# Step 1 — Planner Question Pass and Implementation Audit

## Working Directory

- Code worktree: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor`
- Orchestration docs root: `C:\Users\dw\Desktop\paper-trading\docs`

## Files to Read

- `CODEX.md`
- `docs/TODO.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/index.json`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/domain/entity/position/PositionExitTrigger.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluator.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestrator.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandService.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/application/position/PositionExitTriggerQueryService.kt`
- `backend/trading-api/src/main/kotlin/com/papertrading/api/presentation/dto/position/PositionExitTriggerDtos.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerCommandServiceTest.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerEvaluatorTest.kt`
- `backend/trading-api/src/test/kotlin/com/papertrading/api/application/position/PositionExitTriggerOrchestratorTest.kt`

## Open Questions

- Existing uncommitted work was preserved in commit `66fb6ce`; planner must decide whether to continue from that WIP shape or revise the design before implementation proceeds.
- Confirm migration strategy for existing rows that store both stop-loss and take-profit fields in one row.
- Confirm API compatibility policy for existing trigger request/response DTOs.
- Confirm grouping semantics when multiple triggers of the same `positionId + triggerType` fire in one tick.

## Confirmed Design Choices

- `PositionExitTrigger` should represent one exit condition, not a combined stop-loss/take-profit container.
- Trigger type is `STOP_LOSS` or `TAKE_PROFIT`.
- Trigger state is a single state: `ARMED`, `TRIGGERED`, `CANCELED`, `FAILED`, or `SKIPPED`.
- Monetary values must avoid floating-point types.
- Order execution and trigger processing must remain idempotent and auditable.
- Code work continues in `.worktrees/trading-api-position-exit-trigger-model-refactor`.

## Tasks

1. Audit the WIP implementation in commit `66fb6ce` against the TODO requirements.
2. Produce a structured question list with recommended options only; do not generate final `spec.md` yet.
3. Identify whether any existing code in the WIP commit violates critical financial safety or orchestration constraints.
4. Recommend the final step breakdown needed after user decisions are confirmed.

## Acceptance Criteria

- Planner returns questions and recommended options, not final implementation.
- Planner explicitly accounts for the WIP commit already present in the worktree.
- No Step 2 work starts until user approves planner outputs.

## Agent Return Protocol

Completion Report:
- Status: PASS | FAIL | BLOCKED
- Summary:
- Files modified:
- Test result:
- Blockers:
