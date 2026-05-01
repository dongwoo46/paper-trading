# Step 5: Summary and PR Preparation
Assigned agent: orchestrator

## Working Directory
Main repo root: `C:/Users/dw/Desktop/paper-trading`

Implementation worktree: `.worktrees/trading-api-kis-execution-ws`

## Files to Read
- `C:/Users/dw/Desktop/paper-trading/CODEX.md`
- `C:/Users/dw/Desktop/paper-trading/docs/TODO.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/index.json`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/spec.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/step-2.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/step-3.md`
- `C:/Users/dw/Desktop/paper-trading/docs/phase/trading-api/kis-execution-ws/step-4.md`
- Completion reports from Steps 2 through 4.

## Tasks
Finalize the phase only after implementation, tests, and review are complete.

Required orchestration actions:
- Verify `index.json` has all steps completed or accurately records blockers.
- Summarize the implemented KIS execution WebSocket flow:
  - paper/live channels.
  - polling replacement.
  - idempotent fill processing.
  - SSE notification path.
  - KIS error mapping.
  - token refresh/WebSocket re-auth behavior.
- Run or confirm final verification results:
```bash
cd backend/trading-api && ./gradlew test compileKotlin
```
- Write done summary under `docs/done/trading-api/kis-execution-ws/kis-execution-ws-summary.md`.
- Mark the TODO item complete only after final verification and PR creation:
  - `KIS 체결통보 WebSocket 연동 | project: trading-api | phase: kis-execution-ws | priority: P1`
- Create the PR from `feature/trading-api-kis-execution-ws` using a Korean commit message if committing is required.
- Keep orchestration docs under the main repo root `docs/`; do not use worktree docs as state.

## Acceptance Criteria
- Done summary exists and references final verification.
- `docs/TODO.md` is updated only if the feature is actually complete.
- `docs/phase/trading-api/kis-execution-ws/index.json` accurately reflects final state.
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
