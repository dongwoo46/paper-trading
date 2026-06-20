# Step 7: Cleanup Summary And PR Prep
Assigned agent: Orchestrator

## Working Directory
.worktrees/quant-worker-backtest-engine

## Files to Read
- CODEX.md
- docs/phase/quant-worker/backtest-engine/spec.md
- docs/phase/quant-worker/backtest-engine/index.json
- docs/phase/quant-worker/backtest-engine/step-2.md
- docs/phase/quant-worker/backtest-engine/step-3.md
- docs/phase/quant-worker/backtest-engine/step-4.md
- docs/phase/quant-worker/backtest-engine/step-5.md
- docs/phase/quant-worker/backtest-engine/step-6.md
- docs/TODO.md

## Open Questions
- PR creation requires user confirmation.

## Confirmed Design Choices
- Cleanup runs in manual mode.

## Tasks
1. Run final targeted verification for touched quant-worker files if not already current.
2. Write `backtest-engine-summary.md`.
3. Update `docs/TODO.md` to mark `backtest-engine` complete only after implementation/review pass.
4. Move phase docs to `docs/done/quant-worker/backtest-engine/` only after user-approved finalization.
5. Prepare PR summary and checklist.
6. Do not remove the worktree until PR creation or explicit user-approved cleanup.

## Acceptance Criteria
- Summary includes implemented API, data flow, tests, known limitations, and follow-up TODOs.
- Root orchestration state is updated immediately.
- PR is not created without user confirmation.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences>
- Files modified: <paths relative to worktree root>
- Test result: <commands run and result>
- Blockers: <none | description>
---
