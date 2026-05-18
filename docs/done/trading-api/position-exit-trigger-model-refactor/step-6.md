# Step 6 — Review Pass

Assigned agent: reviewer

## Working Directory

- Code worktree: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor`
- Service directory: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor\backend\trading-api`
- Orchestration docs root: `C:\Users\dw\Desktop\paper-trading\docs`

## Files to Read

- `CODEX.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/spec.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/step-2.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/step-3.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/step-4.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/step-5.md`
- All production and test files modified in Steps 2 through 5.

## Open Questions

- None. Review against the confirmed design in `spec.md`.

## Confirmed Design Choices

- Review priority is financial correctness, state consistency, idempotency, concurrency, and API/model drift.
- No compatibility layer for the old API should remain.
- `AccountExitTriggerDefault` must not be silently refactored beyond compile bridging.

## Tasks

1. Review schema and entity alignment, including Flyway order and development DB reset behavior.
2. Check that one trigger row cannot represent both stop loss and take profit.
3. Check all state transitions and invalid transition guards.
4. Check price policy behavior and `BigDecimal` precision/rounding.
5. Check grouped order idempotency, `orderGroupId`, and trigger id/version audit inputs.
6. Check pending sell quantity subtraction and quantity cap behavior.
7. Check pessimistic lock use and concurrent quote/manual sell conflict handling.
8. Check API/DTO removal of old combined fields.
9. Check tests cover the highest-risk paths from `spec.md`.
10. Return findings first, ordered by severity, with file and line references.

## Acceptance Criteria

- Review identifies blocking correctness issues or explicitly states that no blocking issues were found.
- Any recommended changes are scoped to this phase.
- Verification command if changes are made after review:

```bash
cd backend/trading-api && ./gradlew test --tests "*PositionExitTrigger*"
```

## Agent Return Protocol

Completion Report:
- Status: PASS | FAIL | BLOCKED
- Summary:
- Files modified:
- Test result:
- Blockers:
