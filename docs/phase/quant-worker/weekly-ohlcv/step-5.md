# Step 5: Summary and PR Preparation
Assigned agent: orchestrator

## Working Directory
.worktrees/weekly-ohlcv

## Files to Read
- docs/phase/quant-worker/weekly-ohlcv/spec.md
- docs/phase/quant-worker/weekly-ohlcv/index.json
- docs/phase/quant-worker/weekly-ohlcv/step-2.md
- docs/phase/quant-worker/weekly-ohlcv/step-3.md
- docs/phase/quant-worker/weekly-ohlcv/step-4.md

## Tasks
1. Confirm all prior steps are completed and index status is updated.
2. Write final feature summary document:
- `docs/done/quant-worker/weekly-ohlcv/weekly-ohlcv-summary.md`
- Include:
- implemented scope
- schema/API contract decisions
- test and compile evidence
- review findings and resolutions
3. Update orchestration state:
- mark phase completed in `docs/phase/quant-worker/weekly-ohlcv/index.json`
- update `docs/TODO.md` weekly-ohlcv item to `[x]` with done date and PR number placeholder if needed
4. Prepare PR details:
- title and description with migration note and API contract changes
- explicit rollout/backfill notes if required

## Acceptance Criteria
- Summary doc created under `docs/done/...`.
- TODO and phase status updated consistently.
- PR-ready change summary available.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
