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
- backend/quant-worker/src/migrations/V3__create_backtest_runs.sql
- backend/quant-worker/src/backtest/domain.py
- backend/quant-worker/src/backtest/repository.py
- backend/quant-worker/src/backtest/service.py
- backend/quant-worker/src/backtest/api.py
- backend/quant-worker/src/backtest/data_export.py
- backend/quant-worker/src/backtest/workspace.py
- backend/quant-worker/src/backtest/runner.py
- backend/quant-worker/src/backtest/lean_template/main.py
- backend/quant-worker/src/backtest/lean_template/strategy_loader.py
- backend/quant-worker/tests/backtest/test_domain.py
- backend/quant-worker/tests/backtest/test_service.py
- backend/quant-worker/tests/backtest/test_data_export.py
- backend/quant-worker/tests/backtest/test_workspace.py
- backend/quant-worker/tests/backtest/test_runner.py
- backend/quant-worker/tests/interfaces/test_backtest_api.py
- backend/quant-worker/tests/interfaces/test_backtest_migration_sql.py

## Open Questions
- None. The scalar-factor vocabulary, signal timing, position behavior, and versioned KR/US cost profiles are confirmed in `spec.md`. If Docker is unavailable, the pure template behavior and official launcher/config command remain mandatory; only the live Docker smoke is environment-gated.

## Confirmed Design Choices
- Local DB OHLCV is exported before LEAN execution.
- MVP exports daily bars for KR and US.
- Template is copied into `runs/{runId}` per execution.
- The fixed template executes `price`, `returns`, `sma`, `ema`, `wma`, Wilder `rsi`, `roc`, and `momentum`; it must not merely load the strategy JSON.
- Signals use completed bar `T`, and resulting orders are eligible only at the next available market open. The template enforces whole-strategy warm-up, no pyramiding, and exit precedence.
- `KR_DEFAULT_V1` and `US_DEFAULT_V1` resolve exactly as specified and are deterministically snapshotted per run.
- The selected cost profile ID is required run metadata persisted in `backtest_runs`; resolved bps strings exist only in the immutable run artifact snapshot.

## Tasks
1. Write failing execution-validation tests before changing the domain/template:
   - accepts every confirmed category/indicator/param/source/condition shape
   - rejects `macd`, `bollinger`, `atr`, `equal`, `adjusted_close`, factor sources, nonzero lag, non-null stop/take-profit, invalid/mismatched cost profiles, extra/missing params, and invalid category/indicator pairings
   - asserts the stable error codes named in `spec.md`
2. Write failing migration, repository, service, and API profile-persistence tests:
   - unreleased `V3__create_backtest_runs.sql` defines required `cost_profile`
   - migration schema does not add mutable commission, slippage, or tax-rate columns
   - create service persists the selected profile ID and repository reload returns it unchanged
   - queued/reloaded run execution receives the persisted ID rather than deriving a profile from market
   - create and status responses serialize the same `costProfile`; missing, unknown, and market-mismatched input fails with the specified stable code
3. Write failing factor golden-vector tests from hand-calculated daily series:
   - covers `price`, `returns`, `sma`, `ema`, `wma`, Wilder `rsi`, `roc`, and `momentum`
   - fixes exact formula output and first-ready bar for each indicator
   - covers zero-denominator failure for `returns` and `roc`
4. Write failing rule truth-table tests:
   - covers `cross_above`, `cross_below`, `greater_than`, and `less_than`, including equality boundaries
   - covers field, factor, and Decimal-string literal operands and flat `and`/`or`
   - proves cross conditions require two ready current/previous series values
5. Write failing deterministic execution-trace tests without Docker:
   - no entry or exit signal before the whole strategy is warm
   - completed bar `T` can only create an order for the next available open
   - no same-bar close/open fill and no future-bar input in factor or rule evaluation
   - whole-share, no-leverage `position_size_percent` sizing accounts for adverse buy slippage and commission
   - repeated entry while invested does not pyramid or rebalance
   - exit liquidates the full position; when entry and exit are both true, exit wins for invested state and flat state remains flat
   - no next tradable bar means no fill
6. Write failing cost-profile tests without Docker:
   - `KR_DEFAULT_V1`: buy/sell commission `"5"` bps, adverse fill slippage `"10"` bps, sell tax `"18"` bps
   - `US_DEFAULT_V1`: buy/sell commission `"5"` bps, adverse fill slippage `"10"` bps, sell tax `"0"` bps
   - costs apply only to filled quantity and change cash/equity
   - unknown profile and market mismatch return their stable validation codes
   - resolved profile JSON is byte-identical for identical runs
7. Write failing tests for OHLCV export:
   - queries `market_daily_ohlcv` by `market`, `symbol`, and date range
   - sorts by `trade_date`
   - rejects missing OHLCV fields
   - writes deterministic run artifact files
   - keeps weekly export extension covered by unit-level seam tests
8. Update the unreleased V3 migration and the run domain/repository/service/API contract:
   - persist required `cost_profile` ID in DB and carry it through create, reload, and status serialization
   - keep resolved profile numeric strings out of DB columns
   - do not add a new migration for this unreleased schema
9. Implement or revise `BacktestDataExporter` to satisfy the export tests.
10. Create or revise the fixed LEAN template with reviewed Python files:
   - `main.py`
   - strategy/config loader module
   - no AI-generated executable code
   - calculate all confirmed factor formulas and readiness rules
   - evaluate confirmed conditions and flat logical rules
   - apply the confirmed entry/exit state behavior, sizing, next-open timing, and cost formulas
   - do not contain a generic `eval`, `exec`, dynamic import, or generated strategy source path
11. Implement template copier/run workspace builder:
   - source template is never mutated
   - run-specific `strategy.json`, `run_config.json`, and resolved `cost_profile.json` are written under `runs/{runId}/project`
   - strategy and profile snapshots use deterministic key order and Decimal strings
12. Implement or correct the Docker/LEAN runner:
   - generate the project/config inputs required by the official `quantconnect/lean:latest` launcher contract; mounting files and invoking the image with no executable project contract is not sufficient
   - build an explicit, testable launcher command for the fixed algorithm project
   - supports timeout
   - captures stdout/stderr to artifacts
   - maps success/failure to domain result
13. Unit-test official command/config construction and success, timeout, missing-result, and non-zero-exit handling without requiring Docker.
14. If Docker is available, run an optional end-to-end smoke with synthetic data and assert at least one expected next-open trade. Do not block the step solely because Docker is unavailable.

## Acceptance Criteria
- Exporter and workspace builder are deterministic and covered by tests.
- Golden vectors match all eight confirmed factor formulas and readiness bars exactly.
- Rule truth tables pass for every confirmed condition/operator combination and rejected equality boundary.
- Non-Docker execution traces prove warm-up, no-look-ahead, next-open eligibility, whole-share sizing, no pyramiding, and exit precedence.
- KR/US cost calculations and deterministic profile snapshots match the exact confirmed bps strings; costs affect cash/equity and invalid profile selection fails stably.
- Updated unreleased V3 schema persists a required `cost_profile` ID and contains no mutable resolved-rate columns.
- Domain/repository/service DB round-trips preserve the selected profile ID exactly, including after reload for queued execution.
- Create and status APIs both serialize the persisted `costProfile`; missing, unknown, or market-mismatched values fail stably.
- Runner tests prove the official LEAN launcher/config contract plus success, timeout, missing-result, and non-zero-exit handling via mocks/fakes.
- Generated run workspace contains strategy/config/cost-profile/data/results/logs structure.
- A successful template run must show that rules were evaluated; a process exit code of zero from a JSON-loading/no-trade stub does not satisfy acceptance.
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
