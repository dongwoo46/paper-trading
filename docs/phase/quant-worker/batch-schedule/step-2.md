# Step 2: Implement Market and Interval Batch Scheduler
Assigned agent: quant-dev

## Open Questions
- Before executing this step, ask the user to confirm the Step 2 implementation design.
- Confirm the scheduler module boundary: implement a new `src/jobs/batch_schedule.py` module and wire it from `src/interfaces/api/app.py`.
- Confirm the approved KST schedules are still acceptable:
  - `kr_daily`: Mon-Fri 18:30 KST
  - `us_daily`: Tue-Sat 08:30 KST
  - `kr_weekly`: Fri 19:30 KST
  - `us_weekly`: Sat 10:00 KST
- Confirm retry defaults: initial delay 600s, multiplier 2, max attempts 4, max delay 3600s.
- Confirm Slack integration should be environment-configured only and mocked in tests.
- If the user changes any of these choices, update this step document before implementation.

## Confirmed Design Choices
- Use four independent KST schedules: `kr_daily`, `us_daily`, `kr_weekly`, and `us_weekly`.
- KR daily and weekly use `pykrx`; US daily and weekly use `yfinance`.
- Automated runs must not use a single `provider="all"` call.
- Collection is incremental from catalog watermark and idempotent through repository upsert.
- Failures require structured logs and Slack notification.
- Retries use delayed exponential backoff.
- Empty symbol sets, partial data, and no-new-bar outcomes must be classified explicitly.

## Working Directory
.worktrees/batch-schedule

## Files to Read
- CODEX.md
- backend/quant-worker/CODEX.md
- backend/quant-worker/docs/ADR.md
- backend/quant-worker/docs/PRD.md
- docs/phase/quant-worker/batch-schedule/spec.md
- backend/quant-worker/tests/application/test_daily_fetch_service.py
- backend/quant-worker/tests/application/test_weekly_fetch_service.py
- backend/quant-worker/tests/jobs/test_catalog_daily_fetch_job.py
- backend/quant-worker/tests/jobs/test_catalog_weekly_fetch_job.py
- backend/quant-worker/tests/interfaces/test_weekly_api.py
- backend/quant-worker/src/application/daily_fetch_service.py
- backend/quant-worker/src/application/weekly_fetch_service.py
- backend/quant-worker/src/jobs/catalog_daily_fetch_job.py
- backend/quant-worker/src/jobs/catalog_weekly_fetch_job.py
- backend/quant-worker/src/interfaces/api/app.py

## Tasks
1. Write failing tests first for the scheduler design.
2. Add or update tests under `backend/quant-worker/tests/` for:
   - schedule definitions include `kr_daily`, `us_daily`, `kr_weekly`, `us_weekly`
   - `kr_daily` calls `execute(DailyFetchOptions(provider="pykrx", only_default=True))`
   - `us_daily` calls `execute(DailyFetchOptions(provider="yfinance", only_default=True))`
   - `kr_weekly` calls `execute_weekly(WeeklyFetchOptions(provider="pykrx", only_default=True))`
   - `us_weekly` calls `execute_weekly(WeeklyFetchOptions(provider="yfinance", only_default=True))`
   - no automated schedule calls `provider="all"`
   - schedule timezone is `Asia/Seoul`
3. Implement a concrete scheduler module if needed, preferably `backend/quant-worker/src/jobs/batch_schedule.py`, with these public structures or equivalent:
   - `BatchDefinition`
   - `BatchRunContext`
   - `BatchRunResult`
   - `build_default_batch_definitions()`
   - `run_batch(definition: BatchDefinition, attempt: int = 1) -> BatchRunResult`
4. Replace the generic interval-only auto-collect loop in `backend/quant-worker/src/interfaces/api/app.py` with market/interval-aware batch scheduling.
5. Keep manual endpoints unchanged:
   - `collect_daily(request: CollectDailyRequest)`
   - `collect_weekly(request: CollectWeeklyRequest)`
   - `market_weekly(...)`
6. Implement outcome classification without changing collector internals:
   - `success`
   - `noop_empty_universe`
   - `partial_success`
   - `no_new_bar`
   - `stale_provider_window`
   - `failed`
7. Implement delayed exponential backoff:
   - initial delay 600 seconds
   - multiplier 2
   - max attempts 4
   - max delay 3600 seconds
8. Add Slack notification boundary with environment-based configuration:
   - `SLACK_WEBHOOK_URL`
   - `SLACK_NOTIFICATIONS_ENABLED`
   - no hardcoded secrets
   - no secret values in logs
9. Preserve existing incremental collection and upsert behavior:
   - do not bypass `DailyFetchOptions`
   - do not bypass `WeeklyFetchOptions`
   - do not bypass repository upsert methods
10. Update docs only if implementation changes the accepted interface names or commands; do not mark Step 1 complete.

## Acceptance Criteria
- Tests fail before implementation and pass after implementation.
- Four batch definitions exist with approved KST schedules:
  - `kr_daily`: Mon-Fri 18:30 KST
  - `us_daily`: Tue-Sat 08:30 KST
  - `kr_weekly`: Fri 19:30 KST
  - `us_weekly`: Sat 10:00 KST
- The automated scheduler never triggers one shared all-provider collection.
- Retry behavior is idempotent and bounded by max attempts.
- Slack and logs are emitted for failure statuses.
- Empty symbol sets do not crash or retry by default.
- No-new-bar is not treated as hard failure when calendar/provider timing explains it.
- Existing `/collect/daily`, `/collect/weekly`, and `/market/weekly/{symbol}` tests still pass.
- Verification commands pass:

```powershell
cd backend/quant-worker
python -m pytest tests/application/test_daily_fetch_service.py tests/application/test_weekly_fetch_service.py
python -m pytest tests/jobs/test_catalog_daily_fetch_job.py tests/jobs/test_catalog_weekly_fetch_job.py
python -m pytest tests/interfaces/test_weekly_api.py
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
