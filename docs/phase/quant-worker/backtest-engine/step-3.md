# Step 3: LEAN Template, Data Export, And Docker Runner
Assigned agent: Quant Dev

## Working Directory
.worktrees/quant-worker-backtest-engine

## Files to Read
- CODEX.md
- docs/phase/quant-worker/backtest-engine/spec.md
- docs/phase/quant-worker/backtest-engine/step-2.md
- backend/quant-worker/src/repositories/market_daily_ohlcv_repository.py
- backend/quant-worker/src/repositories/market_weekly_ohlcv_repository.py
- backend/collector-api/src/main/resources/db/migration/V8__create_market_daily_ohlcv.sql
- backend/collector-api/src/main/resources/db/migration/V13__create_market_weekly_ohlcv.sql
- backend/quant-worker/tests/repositories/test_market_daily_ohlcv_repository.py
- backend/quant-worker/tests/repositories/test_market_weekly_ohlcv_repository.py
- backend/quant-worker/src/interfaces/api/app.py

## Open Questions
- None. If exact LEAN local data format details are unavailable offline, implement a documented adapter boundary with tests for deterministic export and mark Docker smoke execution as environment-gated.

## Confirmed Design Choices
- Local DB OHLCV is exported before LEAN execution.
- MVP exports daily bars for KR and US.
- Template is copied into `runs/{runId}` per execution.

## Tasks
1. Write failing tests for OHLCV export:
   - queries `market_daily_ohlcv` by `market`, `symbol`, and date range
   - sorts by `trade_date`
   - rejects missing OHLCV fields
   - writes deterministic run artifact files
   - keeps weekly export extension covered by unit-level seam tests
2. Implement `BacktestDataExporter`.
3. Create a fixed LEAN template directory with reviewed Python files:
   - `main.py`
   - strategy/config loader module
   - no AI-generated executable code
4. Implement template copier/run workspace builder:
   - source template is never mutated
   - run-specific `strategy.json` and `run_config.json` are written under `runs/{runId}/project`
5. Implement Docker/LEAN runner:
   - builds command for `quantconnect/lean:latest`
   - supports timeout
   - captures stdout/stderr to artifacts
   - maps success/failure to domain result
6. Unit-test command construction and failure handling without requiring Docker.
7. If Docker is available, add an optional smoke command/documented check; do not make the whole step blocked solely because Docker is unavailable.

## Acceptance Criteria
- Exporter and workspace builder are deterministic and covered by tests.
- Runner tests cover success, timeout, and non-zero exit handling via mocks/fakes.
- Generated run workspace contains strategy/config/data/results/logs structure.
- No secret or DB credential is written into generated LEAN project files.

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
