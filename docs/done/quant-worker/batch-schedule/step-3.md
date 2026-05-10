# Step 3: Testing and QA Verification
Assigned agent: test-engineer

## Open Questions
- Before executing this step, ask the user to confirm the Step 3 QA design.
- Confirm tests must not call real Slack, `pykrx`, or `yfinance`; all external/provider behavior should use mocks or disabled configuration.
- Confirm full quant-worker test suite should be attempted after feature-scoped tests.
- Confirm that equivalent behavior is acceptable if Step 2 uses different public names than `BatchDefinition`, `BatchRunContext`, `BatchRunResult`, `build_default_batch_definitions()`, or `run_batch(...)`.
- If the user wants stricter naming or broader integration tests, update this step document before QA.

## Confirmed Design Choices
- Verify the decision-first design from `spec.md` rather than re-opening schedule design.
- The schedule model is split by market and interval.
- Provider mapping is fixed:
  - KR daily -> `pykrx`
  - US daily -> `yfinance`
  - KR weekly -> `pykrx`
  - US weekly -> `yfinance`
- Retry is delayed exponential backoff.
- Failures must produce both Slack notification calls and structured logs.
- Empty symbol set is a successful no-op warning, not a crash.
- Partial data persists successes and retries/report failures.
- Calendar/provider-explained no-new-bar is distinct from hard failure.

## Working Directory
.worktrees/batch-schedule

## Files to Read
- CODEX.md
- backend/quant-worker/CODEX.md
- docs/phase/quant-worker/batch-schedule/spec.md
- docs/phase/quant-worker/batch-schedule/step-2.md
- backend/quant-worker/tests/application/test_daily_fetch_service.py
- backend/quant-worker/tests/application/test_weekly_fetch_service.py
- backend/quant-worker/tests/jobs/test_catalog_daily_fetch_job.py
- backend/quant-worker/tests/jobs/test_catalog_weekly_fetch_job.py
- backend/quant-worker/tests/interfaces/test_weekly_api.py
- backend/quant-worker/tests/repositories/test_market_weekly_ohlcv_repository.py
- backend/quant-worker/tests/collectors/test_yfinance_weekly_collector.py
- backend/quant-worker/tests/collectors/test_pykrx_weekly_collector.py
- backend/quant-worker/src/application/daily_fetch_service.py
- backend/quant-worker/src/application/weekly_fetch_service.py
- backend/quant-worker/src/jobs/catalog_daily_fetch_job.py
- backend/quant-worker/src/jobs/catalog_weekly_fetch_job.py
- backend/quant-worker/src/interfaces/api/app.py
- backend/quant-worker/src/jobs/batch_schedule.py

## Tasks
1. Review Step 2 changes before running tests.
2. Add missing tests only if an acceptance criterion from `spec.md` is not covered.
3. Verify scheduler definitions:
   - all four batch IDs exist
   - KST timezone is used
   - cron or schedule expressions match the spec
   - no scheduled run uses `provider="all"`
4. Verify retry behavior:
   - delayed retry is scheduled after a recoverable failure
   - delay doubles per attempt
   - max attempts stop retries
   - final failure emits Slack notification and log
5. Verify production policies:
   - empty symbol set returns `noop_empty_universe`
   - partial success keeps successful rows and retries failed symbols or safely reruns the batch
   - no-new-bar does not advance watermark and does not become hard failure when expected
6. Verify API regressions:
   - `POST /collect/daily` still accepts valid requests and rejects invalid date windows
   - `POST /collect/weekly` still accepts valid requests and rejects invalid date windows
   - `GET /market/weekly/{symbol}` still validates blank symbols, date windows, and limit bounds
7. Verify collector/repository regressions:
   - `YFinanceWeeklyCollector` still uses `interval="1wk"`
   - `PykrxWeeklyCollector` still uses `freq="w"`
   - `MarketWeeklyOhlcvRepository` still uses `ON CONFLICT (source, symbol, trade_date) DO UPDATE`
8. Run the verification commands and summarize pass/fail output.

## Acceptance Criteria
- All feature-scoped tests pass.
- Full quant-worker test suite passes or any unrelated failures are clearly identified with evidence.
- No test performs real Slack calls.
- No test performs real provider network calls unless explicitly marked and skipped by default.
- Logs include useful scheduler metadata without secrets.
- The QA report identifies any residual risk around calendars, provider lag, or deployment configuration.
- Verification commands:

```powershell
cd backend/quant-worker
python -m pytest tests/application/test_daily_fetch_service.py tests/application/test_weekly_fetch_service.py
python -m pytest tests/jobs/test_catalog_daily_fetch_job.py tests/jobs/test_catalog_weekly_fetch_job.py
python -m pytest tests/interfaces/test_weekly_api.py
python -m pytest tests/repositories/test_market_weekly_ohlcv_repository.py
python -m pytest tests/collectors/test_yfinance_weekly_collector.py tests/collectors/test_pykrx_weekly_collector.py
python -m pytest tests
python -m py_compile src/interfaces/api/app.py
```

## Agent Return Protocol
When you finish, output a completion report in EXACTLY this format so the Orchestrator can evaluate:

---
## Completion Report
- Status: PASS | FAIL | BLOCKED
- Summary: <one or two sentences of what was done>
- Files modified: <list of paths relative to main repo root>
- Blockers: <none | description>
---
