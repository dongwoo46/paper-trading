"""KST-aware market/interval-split batch scheduler for OHLCV collection.

Four independent batch definitions:
  kr_daily  — KR 1d  pykrx    Mon-Fri 18:30 KST
  us_daily  — US 1d  yfinance Tue-Sat 08:30 KST
  kr_weekly — KR 1wk pykrx    Fri    19:30 KST
  us_weekly — US 1wk yfinance Sat    10:00 KST

Outcome classification: success | noop_empty_universe | partial_success |
                         no_new_bar | stale_provider_window | failed

Retry: delayed exponential backoff — initial 600s, multiplier 2,
       max 4 attempts, max delay 3600s.

Slack: SLACK_WEBHOOK_URL + SLACK_NOTIFICATIONS_ENABLED env vars.
       Secrets are never logged or embedded in payloads.
"""
from __future__ import annotations

import json
import logging
import os
import time
import urllib.request
from dataclasses import dataclass, field
from datetime import datetime
from enum import Enum
from typing import Literal

from src.application.daily_fetch_service import DailyFetchOptions
from src.application.daily_fetch_service import execute
from src.application.weekly_fetch_service import WeeklyFetchOptions
from src.application.weekly_fetch_service import execute as execute_weekly

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Configuration helpers
# ---------------------------------------------------------------------------

_ENV_INITIAL_DELAY = "BATCH_RETRY_INITIAL_DELAY_SECONDS"
_ENV_MULTIPLIER = "BATCH_RETRY_MULTIPLIER"
_ENV_MAX_ATTEMPTS = "BATCH_RETRY_MAX_ATTEMPTS"
_ENV_MAX_DELAY = "BATCH_RETRY_MAX_DELAY_SECONDS"
_ENV_ONLY_DEFAULT = "BATCH_ONLY_DEFAULT"
_ENV_ENABLED = "BATCH_SCHEDULE_ENABLED"
_ENV_TIMEZONE = "BATCH_SCHEDULE_TIMEZONE"

_DEFAULT_INITIAL_DELAY = 600
_DEFAULT_MULTIPLIER = 2
_DEFAULT_MAX_ATTEMPTS = 4
_DEFAULT_MAX_DELAY = 3600


def _retry_initial_delay() -> int:
    return int(os.getenv(_ENV_INITIAL_DELAY, str(_DEFAULT_INITIAL_DELAY)))


def _retry_multiplier() -> int:
    return int(os.getenv(_ENV_MULTIPLIER, str(_DEFAULT_MULTIPLIER)))


def _retry_max_attempts() -> int:
    return int(os.getenv(_ENV_MAX_ATTEMPTS, str(_DEFAULT_MAX_ATTEMPTS)))


def _retry_max_delay() -> int:
    return int(os.getenv(_ENV_MAX_DELAY, str(_DEFAULT_MAX_DELAY)))


def _batch_only_default() -> bool:
    return os.getenv(_ENV_ONLY_DEFAULT, "true").lower() in ("1", "true", "yes", "on")


def _batch_timezone() -> str:
    return os.getenv(_ENV_TIMEZONE, "Asia/Seoul")


def _batch_enabled() -> bool:
    return os.getenv(_ENV_ENABLED, "false").lower() in ("1", "true", "yes", "on")


# ---------------------------------------------------------------------------
# Domain types
# ---------------------------------------------------------------------------


class BatchOutcome(str, Enum):
    SUCCESS = "success"
    NOOP_EMPTY_UNIVERSE = "noop_empty_universe"
    PARTIAL_SUCCESS = "partial_success"
    NO_NEW_BAR = "no_new_bar"
    STALE_PROVIDER_WINDOW = "stale_provider_window"
    FAILED = "failed"


@dataclass(frozen=True)
class BatchDefinition:
    """Static definition of a single scheduled batch job."""

    batch_id: str
    market: str
    interval: Literal["1d", "1wk"]
    provider: Literal["pykrx", "yfinance"]
    timezone: str
    cron_minute: int
    cron_hour: int
    cron_day_of_week: str  # APScheduler day-of-week expression e.g. "mon-fri"


@dataclass(frozen=True)
class BatchRunContext:
    """Ephemeral context created at run time."""

    batch_id: str
    market: str
    interval: str
    provider: str
    scheduled_for_kst: datetime
    attempt: int
    max_attempts: int


@dataclass(frozen=True)
class BatchRunResult:
    """Result record emitted after each batch run attempt."""

    batch_id: str
    market: str
    interval: str
    provider: str
    attempt: int
    status: BatchOutcome
    symbols: int
    success_symbols: int
    failed_symbols: int
    total_rows_inserted: int
    elapsed_ms: int
    error: str | None
    should_retry: bool


# ---------------------------------------------------------------------------
# Retry delay calculation
# ---------------------------------------------------------------------------


def compute_retry_delay_seconds(
    attempt: int,
    initial: int | None = None,
    multiplier: int | None = None,
    max_delay: int | None = None,
) -> int:
    """Return the retry delay in seconds for a given attempt number (1-indexed).

    delay = min(initial * multiplier^(attempt-1), max_delay)
    """
    _initial = initial if initial is not None else _retry_initial_delay()
    _mult = multiplier if multiplier is not None else _retry_multiplier()
    _max = max_delay if max_delay is not None else _retry_max_delay()
    raw = _initial * (_mult ** (attempt - 1))
    return min(raw, _max)


# ---------------------------------------------------------------------------
# Outcome classification
# ---------------------------------------------------------------------------

_RETRYABLE_OUTCOMES = {
    BatchOutcome.PARTIAL_SUCCESS,
    BatchOutcome.STALE_PROVIDER_WINDOW,  # reserved — staleness check not yet implemented
    BatchOutcome.FAILED,
}
_SLACK_OUTCOMES = {
    BatchOutcome.FAILED,
    BatchOutcome.PARTIAL_SUCCESS,
    BatchOutcome.STALE_PROVIDER_WINDOW,  # reserved — staleness check not yet implemented
}


def _classify_outcome(
    symbols: int,
    success_symbols: int,
    failed_symbols: int,
    total_rows_inserted: int,
) -> BatchOutcome:
    if symbols == 0:
        return BatchOutcome.NOOP_EMPTY_UNIVERSE
    if failed_symbols > 0 and success_symbols > 0:
        return BatchOutcome.PARTIAL_SUCCESS
    if failed_symbols > 0 and success_symbols == 0:
        return BatchOutcome.FAILED
    # All symbols succeeded
    if total_rows_inserted == 0:
        return BatchOutcome.NO_NEW_BAR
    return BatchOutcome.SUCCESS


# ---------------------------------------------------------------------------
# Batch definitions
# ---------------------------------------------------------------------------


def build_default_batch_definitions() -> list[BatchDefinition]:
    """Return the four canonical batch definitions with default KST cron schedules.

    Schedules can be overridden via env vars:
      BATCH_KR_DAILY_CRON, BATCH_US_DAILY_CRON,
      BATCH_KR_WEEKLY_CRON, BATCH_US_WEEKLY_CRON
    """
    tz = _batch_timezone()

    def _parse_cron(env_name: str, default_minute: int, default_hour: int, default_dow: str):
        raw = os.getenv(env_name, "").strip()
        if not raw:
            return default_minute, default_hour, default_dow
        parts = raw.split()
        if len(parts) >= 5:
            try:
                return int(parts[0]), int(parts[1]), parts[4]
            except (ValueError, IndexError):
                pass
        return default_minute, default_hour, default_dow

    kr_daily_min, kr_daily_hr, kr_daily_dow = _parse_cron("BATCH_KR_DAILY_CRON", 30, 18, "mon-fri")
    us_daily_min, us_daily_hr, us_daily_dow = _parse_cron("BATCH_US_DAILY_CRON", 30, 8, "tue-sat")
    kr_weekly_min, kr_weekly_hr, kr_weekly_dow = _parse_cron("BATCH_KR_WEEKLY_CRON", 30, 19, "fri")
    us_weekly_min, us_weekly_hr, us_weekly_dow = _parse_cron("BATCH_US_WEEKLY_CRON", 0, 10, "sat")

    return [
        BatchDefinition(
            batch_id="kr_daily",
            market="KR",
            interval="1d",
            provider="pykrx",
            timezone=tz,
            cron_minute=kr_daily_min,
            cron_hour=kr_daily_hr,
            cron_day_of_week=kr_daily_dow,
        ),
        BatchDefinition(
            batch_id="us_daily",
            market="US",
            interval="1d",
            provider="yfinance",
            timezone=tz,
            cron_minute=us_daily_min,
            cron_hour=us_daily_hr,
            cron_day_of_week=us_daily_dow,
        ),
        BatchDefinition(
            batch_id="kr_weekly",
            market="KR",
            interval="1wk",
            provider="pykrx",
            timezone=tz,
            cron_minute=kr_weekly_min,
            cron_hour=kr_weekly_hr,
            cron_day_of_week=kr_weekly_dow,
        ),
        BatchDefinition(
            batch_id="us_weekly",
            market="US",
            interval="1wk",
            provider="yfinance",
            timezone=tz,
            cron_minute=us_weekly_min,
            cron_hour=us_weekly_hr,
            cron_day_of_week=us_weekly_dow,
        ),
    ]


# ---------------------------------------------------------------------------
# Batch runner
# ---------------------------------------------------------------------------


def run_batch(definition: BatchDefinition, attempt: int = 1) -> BatchRunResult:
    """Execute a single batch run and return a structured result.

    Does NOT schedule retries — the scheduler loop is responsible for that.
    Never raises; exceptions are caught and classified as FAILED.
    """
    max_attempts = _retry_max_attempts()
    only_default = _batch_only_default()
    started_at = time.perf_counter()

    try:
        if definition.interval == "1d":
            service_result = execute(
                DailyFetchOptions(
                    provider=definition.provider,
                    only_default=only_default,
                )
            )
        else:
            service_result = execute_weekly(
                WeeklyFetchOptions(
                    provider=definition.provider,
                    only_default=only_default,
                )
            )

        elapsed_ms = int((time.perf_counter() - started_at) * 1000)

        symbols: int = service_result.get("symbols", 0)
        success_symbols: int = service_result.get("success_symbols", 0)
        failed_symbols: int = service_result.get("failed_symbols", 0)
        total_rows_inserted: int = service_result.get("total_rows_inserted", 0)

        status = _classify_outcome(symbols, success_symbols, failed_symbols, total_rows_inserted)
        should_retry = status in _RETRYABLE_OUTCOMES and attempt < max_attempts

        result = BatchRunResult(
            batch_id=definition.batch_id,
            market=definition.market,
            interval=definition.interval,
            provider=definition.provider,
            attempt=attempt,
            status=status,
            symbols=symbols,
            success_symbols=success_symbols,
            failed_symbols=failed_symbols,
            total_rows_inserted=total_rows_inserted,
            elapsed_ms=elapsed_ms,
            error=None,
            should_retry=should_retry,
        )

    except Exception as exc:  # noqa: BLE001
        elapsed_ms = int((time.perf_counter() - started_at) * 1000)
        status = BatchOutcome.FAILED
        should_retry = attempt < max_attempts

        result = BatchRunResult(
            batch_id=definition.batch_id,
            market=definition.market,
            interval=definition.interval,
            provider=definition.provider,
            attempt=attempt,
            status=status,
            symbols=0,
            success_symbols=0,
            failed_symbols=0,
            total_rows_inserted=0,
            elapsed_ms=elapsed_ms,
            error=str(exc),
            should_retry=should_retry,
        )

    _log_structured(result)
    return result


# ---------------------------------------------------------------------------
# Structured logging
# ---------------------------------------------------------------------------


def _log_structured(result: BatchRunResult) -> None:
    log_data = {
        "batch_id": result.batch_id,
        "market": result.market,
        "interval": result.interval,
        "provider": result.provider,
        "attempt": result.attempt,
        "status": result.status.value,
        "symbols": result.symbols,
        "success_symbols": result.success_symbols,
        "failed_symbols": result.failed_symbols,
        "total_rows_inserted": result.total_rows_inserted,
        "elapsed_ms": result.elapsed_ms,
    }
    if result.error:
        log_data["error"] = result.error

    if result.status in (BatchOutcome.FAILED, BatchOutcome.PARTIAL_SUCCESS, BatchOutcome.STALE_PROVIDER_WINDOW):
        logger.warning("batch_run %s", json.dumps(log_data))
    elif result.status == BatchOutcome.NOOP_EMPTY_UNIVERSE:
        logger.warning("batch_run %s", json.dumps(log_data))
    else:
        logger.info("batch_run %s", json.dumps(log_data))


# ---------------------------------------------------------------------------
# Slack notification
# ---------------------------------------------------------------------------


def notify_slack(result: BatchRunResult) -> None:
    """Send a Slack notification if configured and outcome warrants it.

    Only sends for: failed, partial_success, stale_provider_window,
    noop_empty_universe (when enabled).
    Never logs secrets. Payload does not include the webhook URL.
    """
    enabled = os.getenv("SLACK_NOTIFICATIONS_ENABLED", "false").lower() in ("1", "true", "yes", "on")
    if not enabled:
        return

    webhook_url = os.getenv("SLACK_WEBHOOK_URL", "")
    if not webhook_url:
        return

    # Determine whether to notify
    notify_outcomes = {
        BatchOutcome.FAILED,
        BatchOutcome.PARTIAL_SUCCESS,
        BatchOutcome.STALE_PROVIDER_WINDOW,
        BatchOutcome.NOOP_EMPTY_UNIVERSE,
    }
    if result.status not in notify_outcomes:
        return

    _send_slack_message(webhook_url, result)


def _send_slack_message(webhook_url: str, result: BatchRunResult) -> None:
    """Build and send a Slack message. Does not include secrets in the payload."""
    retry_info = ""
    if result.should_retry:
        delay = compute_retry_delay_seconds(attempt=result.attempt)
        retry_info = f" | next retry in {delay}s"
    elif result.status in _RETRYABLE_OUTCOMES:
        retry_info = " | retries exhausted"

    text_parts = [
        f":rotating_light: *batch_run* `{result.batch_id}` — `{result.status.value}`",
        f"market={result.market} interval={result.interval} provider={result.provider}",
        f"attempt={result.attempt} symbols={result.symbols} "
        f"success={result.success_symbols} failed={result.failed_symbols} "
        f"rows={result.total_rows_inserted} elapsed={result.elapsed_ms}ms{retry_info}",
    ]
    if result.error:
        # Truncate error messages to avoid leaking stack traces
        safe_error = result.error[:200]
        text_parts.append(f"error: {safe_error}")

    payload = json.dumps({"text": "\n".join(text_parts)}).encode("utf-8")

    req = urllib.request.Request(
        webhook_url,
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            pass
    except Exception as exc:  # noqa: BLE001
        # Never let Slack failure crash the batch runner
        logger.warning("notify_slack:error batch_id=%s error=%s", result.batch_id, str(exc))


# ---------------------------------------------------------------------------
# APScheduler lifecycle helpers (imported by app.py)
# ---------------------------------------------------------------------------


def start_batch_scheduler() -> object | None:
    """Create, configure, and start the APScheduler BackgroundScheduler.

    Returns the scheduler instance so app.py can stop it on shutdown.
    Returns None if BATCH_SCHEDULE_ENABLED is false.
    """
    if not _batch_enabled():
        logger.info("batch_scheduler:disabled BATCH_SCHEDULE_ENABLED=false")
        return None

    try:
        from apscheduler.schedulers.background import BackgroundScheduler
        from apscheduler.triggers.cron import CronTrigger
    except ImportError:
        logger.error("batch_scheduler:apscheduler_not_installed — install apscheduler to enable batch scheduling")
        return None

    tz = _batch_timezone()
    scheduler = BackgroundScheduler(timezone=tz)

    for defn in build_default_batch_definitions():
        trigger = CronTrigger(
            minute=defn.cron_minute,
            hour=defn.cron_hour,
            day_of_week=defn.cron_day_of_week,
            timezone=tz,
        )
        scheduler.add_job(
            _run_batch_with_retry,
            trigger=trigger,
            args=[defn],
            id=defn.batch_id,
            name=f"batch:{defn.batch_id}",
            max_instances=1,
            replace_existing=True,
        )
        logger.info(
            "batch_scheduler:registered batch_id=%s cron=%s:%s dow=%s tz=%s",
            defn.batch_id,
            defn.cron_hour,
            defn.cron_minute,
            defn.cron_day_of_week,
            tz,
        )

    scheduler.start()
    logger.info("batch_scheduler:started timezone=%s", tz)
    return scheduler


def stop_batch_scheduler(scheduler: object | None) -> None:
    """Gracefully shut down the scheduler."""
    if scheduler is None:
        return
    try:
        scheduler.shutdown(wait=False)
        logger.info("batch_scheduler:stopped")
    except Exception as exc:  # noqa: BLE001
        logger.warning("batch_scheduler:shutdown_error error=%s", str(exc))


def _run_batch_with_retry(definition: BatchDefinition) -> None:
    """Execute a batch and retry with exponential backoff if needed.

    This function runs inside the APScheduler thread pool.
    """
    import threading

    attempt = 1
    while True:
        result = run_batch(definition, attempt=attempt)
        notify_slack(result)

        if not result.should_retry:
            if attempt > 1 and result.status in _RETRYABLE_OUTCOMES:
                # Final retry exhausted — send exhaustion notification
                exhaustion = BatchRunResult(
                    batch_id=result.batch_id,
                    market=result.market,
                    interval=result.interval,
                    provider=result.provider,
                    attempt=attempt,
                    status=result.status,
                    symbols=result.symbols,
                    success_symbols=result.success_symbols,
                    failed_symbols=result.failed_symbols,
                    total_rows_inserted=result.total_rows_inserted,
                    elapsed_ms=result.elapsed_ms,
                    error=result.error,
                    should_retry=False,
                )
                _send_slack_exhaustion(exhaustion)
            break

        delay = compute_retry_delay_seconds(attempt=attempt)
        logger.info(
            "batch_run:retry_scheduled batch_id=%s attempt=%d next_in_seconds=%d",
            definition.batch_id,
            attempt + 1,
            delay,
        )
        # Sleep in the scheduler thread (blocking is acceptable here)
        time.sleep(delay)
        attempt += 1


def _send_slack_exhaustion(result: BatchRunResult) -> None:
    """Send a final retry-exhaustion Slack alert."""
    enabled = os.getenv("SLACK_NOTIFICATIONS_ENABLED", "false").lower() in ("1", "true", "yes", "on")
    webhook_url = os.getenv("SLACK_WEBHOOK_URL", "")
    if not enabled or not webhook_url:
        return

    text = (
        f":sos: *batch_run exhausted* `{result.batch_id}` — `{result.status.value}`\n"
        f"market={result.market} interval={result.interval} provider={result.provider}\n"
        f"Final attempt={result.attempt} symbols={result.symbols} failed={result.failed_symbols}"
    )
    payload = json.dumps({"text": text}).encode("utf-8")
    req = urllib.request.Request(
        webhook_url,
        data=payload,
        headers={"Content-Type": "application/json"},
        method="POST",
    )
    try:
        with urllib.request.urlopen(req, timeout=5) as resp:
            pass
    except Exception as exc:  # noqa: BLE001
        logger.warning("notify_slack:exhaustion_error batch_id=%s error=%s", result.batch_id, str(exc))