# Step 4: Code Review
Assigned agent: code-reviewer

## Working Directory
.worktrees/weekly-ohlcv

## Files to Read
- docs/phase/quant-worker/weekly-ohlcv/spec.md
- docs/phase/quant-worker/weekly-ohlcv/step-2.md
- docs/phase/quant-worker/weekly-ohlcv/step-3.md
- All files changed in Step 2 and Step 3

## Tasks
1. Review for correctness and regressions in:
- weekly yfinance collection logic (`interval="1wk"`)
- date-window and limit validation
- query ordering and response schema stability
2. Review financial safety:
- Decimal/BigDecimal usage only for monetary/volume fields
- no float-based persistence logic
3. Review persistence and migration safety:
- unique key and indexes suitable for `GET /market/weekly/{symbol}` access pattern
- upsert conflict target and update set correctness
4. Review architecture consistency:
- weekly flow mirrors existing daily layering (collector/job/service/repository/controller)
- no unrelated refactor or scope creep
5. Review test quality:
- meaningful Red→Green coverage for collector/repository/job/service/API
- key failure modes covered

## Acceptance Criteria
- No critical/high-severity defects remain.
- Review findings documented with file paths and concrete fixes.
- If no findings, explicitly state zero findings and residual risks (if any).

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
