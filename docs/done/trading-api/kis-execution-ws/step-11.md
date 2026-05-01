# Step 11: Summary and PR Preparation
Assigned agent: cleanup

## Working Directory
Main repo root: `C:/Users/dw/Desktop/paper-trading`

Implementation worktree: `.worktrees/trading-api-kis-execution-ws`

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/TODO.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/index.json`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/spec.md`
- All completion reports in `index.json`.

## Tasks
Finalize the phase only after Step 10 final full-suite gate passes.

Required orchestration actions:
- Verify `index.json` has implementation, QA, review, rework, final review, and final full-suite gate completed.
- Write done summary under `docs/done/trading-api/kis-execution-ws/kis-execution-ws-summary.md`.
- Mark the TODO item complete only after final verification and PR creation:
  - `KIS 체결통보 WebSocket 연동 | project: trading-api | phase: kis-execution-ws | priority: P1`
- Commit worktree changes with a Korean commit message if needed.
- Prepare PR description with risk notes and test results.
- Keep orchestration docs under the main repo root `docs/`.

## Acceptance Criteria
- Done summary exists and references final verification.
- `docs/TODO.md` is updated only if the feature is actually complete.
- `docs/phase/trading-api/kis-execution-ws/index.json` accurately reflects final state until moved to `docs/done`.
- PR description includes risk notes and test results.
- No unrelated code or docs changes are included.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was finalized>
- Files modified: <list of paths relative to main repo root>
- Test result: <not run | passed N/N | failed N - list failing cases>
- Blockers: <none | description>
---
