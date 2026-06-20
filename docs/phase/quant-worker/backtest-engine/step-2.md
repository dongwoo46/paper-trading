# Step 2: Backtest Domain, DSL, Persistence, And Run API Skeleton
Assigned agent: Quant Dev

## Working Directory
.worktrees/quant-worker-backtest-engine

## Files to Read
- CODEX.md
- docs/phase/quant-worker/backtest-engine/spec.md
- docs/phase/quant-worker/backtest-engine/step-1.md
- backend/quant-worker/CODEX.md
- backend/quant-worker/requirements.txt
- backend/quant-worker/src/interfaces/api/app.py
- backend/quant-worker/src/migrations/V1__create_investor_flow_tables.sql
- backend/quant-worker/src/migrations/V2__create_chart_analysis_tables.sql
- backend/quant-worker/tests/interfaces/test_investor_flow_api.py
- backend/quant-worker/tests/interfaces/test_weekly_api.py
- backend/quant-worker/src/catalog/postgres_symbol_catalog.py

## Open Questions
- None. Use the confirmed decisions in `spec.md`.

## Confirmed Design Choices
- LEAN is the only execution engine.
- Strategy input is validated JSON DSL, not generated Python.
- API is asynchronous run-based.
- DB stores run metadata and summary only.

## Tasks
1. Write failing tests first for DSL validation:
   - accepts executable `price` and `technical` factors
   - rejects unknown categories/operators/indicators/markets
   - recognizes but rejects planned unsupported categories (`flow`, `fundamental`, `macro`, `news_sentiment`, `disclosure`, `paper_factor`, `event`) with stable error codes
   - rejects look-ahead-prone/future references
2. Implement `src/backtest/domain` or equivalent:
   - run status enum
   - market/currency policy
   - strategy DSL Pydantic models
   - run request/summary/result value objects
3. Add Python migration for `backtest_runs` using `CREATE TABLE IF NOT EXISTS`.
4. Implement repository/service layer for creating runs, updating status, storing summary, and loading run metadata.
5. Add FastAPI routes:
   - `POST /backtest-runs`
   - `GET /backtest-runs/{runId}`
   - basic `GET /backtest-runs/{runId}/result`
   - basic `GET /backtest-runs/{runId}/logs`
6. For this step, execution may remain a queued/skeleton service that records `PENDING`; actual LEAN export/execution belongs to later steps.
7. Keep Decimal/string serialization for money and metrics.

## Acceptance Criteria
- Tests prove DSL validation and API request/lookup behavior.
- `backtest_runs` migration is idempotent.
- API returns stable 400/404/422-style errors for invalid input and unknown runs.
- No Docker/LEAN execution is required yet.
- No detailed trades/equity DB tables are added.

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
