# Step 1: Feature Spec and API Spec
Assigned agent: quant-planner

## Working Directory
.worktrees/weekly-ohlcv

## Files to Read
- CODEX.md
- docs/TODO.md
- docs/phase/quant-worker/weekly-ohlcv/index.json

## Tasks
1. Define detailed spec for `quant-worker/weekly-ohlcv` based on TODO requirement.
2. Design weekly OHLCV collection flow using yfinance `interval="1wk"` with Decimal-safe monetary handling.
3. Define schema/model/API requirements:
   - weekly table and SQLAlchemy model
   - migration strategy aligned with existing daily flow
   - `POST /collect/weekly` and `GET /market/weekly/{symbol}` contracts
4. Write `spec.md`.
5. Create `step-2.md` to `step-5.md` with concrete implementation/testing/review/cleanup instructions and explicit file paths.

## Acceptance Criteria
- `spec.md` created.
- `step-2.md` to `step-5.md` created with actionable directives.
- Each step file includes `## Agent Return Protocol`.

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
