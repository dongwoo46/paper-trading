# Quant Worker Batch Schedule Spec

## Scope
Automate quant-worker OHLCV collection for daily and weekly candles with separate schedules by market and interval.

This phase is scheduling, retry, notification, and orchestration work only. It must not change collection semantics beyond routing the existing daily and weekly pipelines through a market/interval-aware scheduler.

## Confirmed Design Choices
- Do not use one shared 17:00 KST schedule for all data.
- Split schedules by market and candle interval because collection is incremental and provider update timing differs.
- KR daily candles use `pykrx`.
- US daily candles use `yfinance`.
- KR weekly candles use `pykrx`.
- US weekly candles use `yfinance`.
- Use delayed retry with exponential backoff.
- Send both Slack notifications and structured logs for failures.
- Use incremental collection from the last stored date/bar and idempotent upsert.
- Treat no-new-bar differently from hard failure when calendar/provider timing explains it.
- Scheduler timing must match realistic provider update availability, not exact market close time.

## Planner Assumptions
- The scheduler remains inside `backend/quant-worker/src/interfaces/api/app.py` unless implementation discovers a cleaner local module extraction such as `src/jobs/batch_schedule.py`.
- Existing HTTP endpoints remain available:
  - `POST /collect/daily`
  - `POST /collect/weekly`
  - `GET /market/weekly/{symbol}`
- Existing service entry points remain the execution boundary:
  - `src.application.daily_fetch_service.execute(DailyFetchOptions(...))`
  - `src.application.weekly_fetch_service.execute(WeeklyFetchOptions(...))`
- Existing catalog watermarks are the source for incremental windows:
  - `PostgresSymbolCatalogRepository.list_symbols(provider=..., only_default=...)`
  - `PostgresSymbolCatalogRepository.update_collection_status(...)`
- Existing repositories keep idempotency through upsert:
  - `MarketDailyOhlcvRepository.upsert_daily_rows(...)`
  - `MarketWeeklyOhlcvRepository.upsert_weekly_rows(...)`
- Slack delivery uses environment configuration. Secrets must stay in environment variables and must not be logged.
- Calendar-aware behavior can start with deterministic weekday/time guards and provider no-data classification. A dedicated exchange-calendar library can be added later only if needed.

## Batch Definitions
Use four independent batch definitions.

| Batch ID | Market | Interval | Provider | Default KST Schedule | API Equivalent |
| --- | --- | --- | --- | --- | --- |
| `kr_daily` | KR | `1d` | `pykrx` | Mon-Fri 18:30 KST | `DailyFetchOptions(provider="pykrx", only_default=true)` |
| `us_daily` | US | `1d` | `yfinance` | Tue-Sat 08:30 KST | `DailyFetchOptions(provider="yfinance", only_default=true)` |
| `kr_weekly` | KR | `1wk` | `pykrx` | Fri 19:30 KST | `WeeklyFetchOptions(provider="pykrx", only_default=true)` |
| `us_weekly` | US | `1wk` | `yfinance` | Sat 10:00 KST | `WeeklyFetchOptions(provider="yfinance", only_default=true)` |

Do not schedule all providers through `provider="all"` for normal automated runs. `provider="all"` remains valid for manual HTTP collection.

## Timing Rationale
Exact market-close timing is unsafe because provider bars are not guaranteed to be finalized at the exchange close.

KR daily uses 18:30 KST to allow post-close OHLCV publication and provider lag after the regular KRX session. KR weekly uses Friday 19:30 KST because the weekly candle depends on the final daily bar of the Korean trading week.

US daily runs Tue-Sat 08:30 KST because the regular US close lands early morning KST and yfinance EOD availability can lag. US weekly runs Saturday 10:00 KST to allow additional buffer after the Friday US close and yfinance weekly aggregation.

If a market is closed or a provider has not yet exposed a finalized bar, the run should be classified as `no_new_bar` or `stale_provider_window`, not as a hard failure.

## Processing Flow
For each enabled batch definition:

1. Scheduler determines whether the batch is due in KST.
2. Scheduler creates a run context:
   - `batch_id`
   - `market`
   - `interval`
   - `provider`
   - `scheduled_for_kst`
   - `attempt`
   - `max_attempts`
3. Scheduler calls the matching service:
   - `kr_daily` -> `execute(DailyFetchOptions(provider="pykrx", only_default=True))`
   - `us_daily` -> `execute(DailyFetchOptions(provider="yfinance", only_default=True))`
   - `kr_weekly` -> `execute_weekly(WeeklyFetchOptions(provider="pykrx", only_default=True))`
   - `us_weekly` -> `execute_weekly(WeeklyFetchOptions(provider="yfinance", only_default=True))`
4. Service loads symbols using provider-specific catalog filtering.
5. Job computes each symbol's effective start from `fetched_until_date + 1 day`, bounded by the requested window.
6. Collector fetches provider data for the effective window.
7. Repository writes rows using idempotent upsert.
8. Service updates catalog watermark only for successful rows with a non-null `fetched_until_date`.
9. Scheduler classifies the run outcome and decides whether to retry, notify, or mark success.

## Outcome Classification
Use explicit statuses in logs and scheduler result objects.

| Status | Meaning | Retry | Slack |
| --- | --- | --- | --- |
| `success` | All requested symbols succeeded or were already current. | No | No |
| `noop_empty_universe` | Catalog returned zero symbols for the batch. | No | Warning log; Slack warning only when the batch is enabled and production expects symbols |
| `partial_success` | At least one symbol succeeded and at least one symbol failed. | Retry failed symbols or rerun idempotently | Yes |
| `no_new_bar` | Provider returned no rows but this is explainable by calendar, holiday, weekend, or provider timing. | No immediate hard retry; optional delayed check if within freshness window | Log info/warning, no failure Slack unless stale beyond SLA |
| `stale_provider_window` | Expected bar is still missing after the provider buffer and retry window. | Yes until max attempts; then stop | Yes |
| `failed` | Service, DB, provider, validation, or unexpected exception failed the run. | Yes until max attempts | Yes |

## Production Policies
Empty symbol set:
Return a successful no-op result with status `noop_empty_universe`. This is not a hard collection failure because an empty catalog can be intentional in non-production or filtered runs. In production, log a warning with `batch_id`, `provider`, and `only_default`; send Slack warning if the batch is enabled and the provider universe is expected to be non-empty.

Partial data:
Persist all successful symbols and do not roll them back because upsert is idempotent and per-symbol failures should not block good data. Mark the batch `partial_success`, log failed symbols, notify Slack, and retry with delayed exponential backoff. If symbol-scoped retry is not implemented in the first pass, rerun the same provider/interval batch because upsert makes the rerun safe.

Stale/no-new-bar:
Classify no-new-bar as non-failure when market calendar or provider timing explains it. Do not advance the catalog watermark when no rows are returned. If the bar is still missing after the configured buffer and retry attempts, classify as `stale_provider_window`, notify Slack, and keep the watermark unchanged.

## Retry Policy
Use delayed exponential backoff for idempotent batch runs.

- Initial retry delay: 10 minutes.
- Backoff multiplier: 2.
- Max attempts per scheduled run: 4 total attempts, including the initial attempt.
- Max delay cap: 60 minutes.
- Retry only statuses that can recover: `partial_success`, `stale_provider_window`, `failed`.
- Do not retry `success` or `noop_empty_universe`.
- Do not retry `no_new_bar` unless the scheduler determines that the provider freshness window is still open and a delayed check is useful.

## Notification Rules
Structured logs are mandatory for every scheduled batch run:

- `batch_id`
- `market`
- `interval`
- `provider`
- `attempt`
- `status`
- `symbols`
- `success_symbols`
- `failed_symbols`
- `total_rows_inserted`
- `elapsed_ms`
- `error`, when present

Slack notification is mandatory for:

- `failed`
- `partial_success`
- `stale_provider_window`
- production `noop_empty_universe` warning
- final retry exhaustion

Slack messages must exclude secrets and full stack traces. Include batch metadata, summary counts, failed symbols if bounded, and the next retry delay or final status.

## Configuration
Use environment variables or an equivalent typed config wrapper. Recommended names:

- `BATCH_SCHEDULE_ENABLED`
- `BATCH_SCHEDULE_TIMEZONE=Asia/Seoul`
- `BATCH_KR_DAILY_CRON=30 18 * * 1-5`
- `BATCH_US_DAILY_CRON=30 8 * * 2-6`
- `BATCH_KR_WEEKLY_CRON=30 19 * * 5`
- `BATCH_US_WEEKLY_CRON=0 10 * * 6`
- `BATCH_ONLY_DEFAULT=true`
- `BATCH_RETRY_INITIAL_DELAY_SECONDS=600`
- `BATCH_RETRY_MULTIPLIER=2`
- `BATCH_RETRY_MAX_ATTEMPTS=4`
- `BATCH_RETRY_MAX_DELAY_SECONDS=3600`
- `SLACK_WEBHOOK_URL`
- `SLACK_NOTIFICATIONS_ENABLED`

The implementation may keep the existing `AUTO_COLLECT_*` variables temporarily only for backward compatibility, but new batch behavior must be driven by market/interval-specific configuration.

## Verification Scope
Implementation must add tests before behavior changes.

Minimum test coverage:

- Schedule definitions contain exactly `kr_daily`, `us_daily`, `kr_weekly`, and `us_weekly`.
- Each batch maps to the correct provider and service options.
- Scheduler does not use one shared `provider="all"` automated run.
- Retry policy uses delayed exponential backoff and stops at max attempts.
- Empty symbol set becomes `noop_empty_universe`.
- Partial success triggers retry and Slack/log notification.
- No-new-bar stays distinct from hard failure.
- `POST /collect/daily` and `POST /collect/weekly` continue to work.
- Existing weekly collector/repository/API tests continue to pass.

Recommended verification commands:

```powershell
cd backend/quant-worker
python -m pytest tests/application/test_daily_fetch_service.py tests/application/test_weekly_fetch_service.py
python -m pytest tests/jobs/test_catalog_daily_fetch_job.py tests/jobs/test_catalog_weekly_fetch_job.py
python -m pytest tests/interfaces/test_weekly_api.py
python -m pytest tests
python -m py_compile src/interfaces/api/app.py
```
