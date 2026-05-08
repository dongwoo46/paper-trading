# Step 5: Finalization, Docs Update, and PR Preparation
Assigned agent: orchestrator

## Working Directory
.worktrees/trading-api-position-trigger-exit

## Files to Read
- C:/Users/dw/Desktop/paper-trading/CODEX.md
- C:/Users/dw/Desktop/paper-trading/docs/state.md
- C:/Users/dw/Desktop/paper-trading/docs/TODO.md
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/index.json
- C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/position-trigger-exit/spec.md

## Objective
Close out phase after implementation, testing, and review are complete.

## Finalization Directives
1. Confirm step-2 to step-4 completion reports are PASS.
2. Update phase status in:
- `docs/phase/trading-api/position-trigger-exit/index.json`
- `docs/state.md`

3. If phase completed:
- mark TODO item as done in `docs/TODO.md` with date and PR number.
- write summary doc:
  - `docs/done/trading-api/position-trigger-exit/position-trigger-exit-summary.md`

4. Prepare PR details:
- key changes by layer (migration/domain/app/api/test).
- risk notes and rollback notes.
- verification command outputs summary.

## Deliverables
- Updated orchestration docs.
- Final summary document.
- PR-ready change summary.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
