# Step 7 — Cleanup and Phase Handoff

Assigned agent: cleanup

## Working Directory

- Code worktree: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor`
- Service directory: `C:\Users\dw\Desktop\paper-trading\.worktrees\trading-api-position-exit-trigger-model-refactor\backend\trading-api`
- Orchestration docs root: `C:\Users\dw\Desktop\paper-trading\docs`

## Files to Read

- `CODEX.md`
- `docs/TODO.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/index.json`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/spec.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/step-2.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/step-3.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/step-4.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/step-5.md`
- `docs/phase/trading-api/position-exit-trigger-model-refactor/step-6.md`
- Final git diff for the worktree.

## Open Questions

- None. Do not update `docs/state.md`; the orchestrator owns it.

## Confirmed Design Choices

- Cleanup finalizes docs and handoff only after implementation, test, and review steps pass.
- Commit messages must be Korean if a commit is requested by the orchestrator/user.
- Root `docs/` is the only orchestration state source.

## Tasks

1. Confirm all prior step completion reports are PASS or have accepted follow-up resolution.
2. Run final verification commands and summarize outputs.
3. Check modified files for unresolved TODO/FIXME introduced by this phase.
4. Update this phase `index.json` to mark completed steps according to actual outcomes.
5. If the phase is complete, prepare the done summary under `docs/done/trading-api/position-exit-trigger-model-refactor/` only when the orchestrator instructs cleanup to finish the phase.
6. Do not update `docs/state.md` in this phase handoff unless the orchestrator explicitly instructs it.
7. Return concise handoff with files changed, verification results, residual risks, and blockers.

## Acceptance Criteria

- Final verification passes or failures are clearly reported.
- Phase docs accurately reflect actual step statuses.
- No root state update is made by this planner-created cleanup step.
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
