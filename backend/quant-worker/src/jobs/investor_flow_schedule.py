"""Independent APScheduler batch for investor flow data collection.

Two batch definitions:
  investor_flow_kospi  — KOSPI  19:00 KST Mon-Fri
  investor_flow_kosdaq — KOSDAQ 19:00 KST Mon-Fri

Activated via INVESTOR_FLOW_SCHEDULE_ENABLED=true (default: false).
Cron overrides: INVESTOR_FLOW_CRON_KOSPI, INVESTOR_FLOW_CRON_KOSDAQ
  Format: "minute hour * * day_of_week"  (5-field cron, fields 0,1,4 used)

Retry policy: reuses compute_retry_delay_seconds from batch_schedule.py.
Slack alerts: reuses notify_slack API from batch_schedule.py adapted for
              InvestorFlowRunResult (which mirrors BatchRunResult fields).
"""
from __future__ import annotations

import logging
import os
import time
from dataclasses import dataclass

from src.jobs.batch_schedule import compute_retry_delay_seconds, notify_slack, BatchRunResult, BatchOutcome

logger = logging.getLogger(__name__)

# APScheduler imports are deferred to start_investor_flow_scheduler() so that
# tests can run without apscheduler installed.
try:
    from apscheduler.schedulers.background import BackgroundScheduler
    from apscheduler.triggers.cron import CronTrigger
except ImportError:
    BackgroundScheduler = None  # type: ignore[assignment,misc]
    CronTrigger = None  # type: ignore[assignment,misc]

_ENV_ENABLED = "INVESTOR_FLOW_SCHEDULE_ENABLED"
_ENV_TIMEZONE = "BATCH_SCHEDULE_TIMEZONE"


def _schedule_enabled() -> bool:
    return os.getenv(_ENV_ENABLED, "false").lower() in ("1", "true", "yes", "on")


def _batch_timezone() -> str:
    return os.getenv(_ENV_TIMEZONE, "Asia/Seoul")


# ---------------------------------------------------------------------------
# Batch definition (reuses BatchDefinition from batch_schedule for consistency)
# ---------------------------------------------------------------------------

@dataclass(frozen=True)
class InvestorFlowBatchDefinition:
    """Static definition of a single investor flow batch job."""
    batch_id: str
    market: str
    timezone: str
    cron_minute: int
    cron_hour: int
    cron_day_of_week: str


def _parse_cron(env_name: str, default_minute: int, default_hour: int, default_dow: str):
    """Parse a 5-field cron string from environment variable."""
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


def build_investor_flow_batch_definitions() -> list[InvestorFlowBatchDefinition]:
    """Return the two canonical investor flow batch definitions."""
    tz = _batch_timezone()

    kospi_min, kospi_hr, kospi_dow = _parse_cron("INVESTOR_FLOW_CRON_KOSPI", 0, 19, "mon-fri")
    kosdaq_min, kosdaq_hr, kosdaq_dow = _parse_cron("INVESTOR_FLOW_CRON_KOSDAQ", 0, 19, "mon-fri")

    return [
        InvestorFlowBatchDefinition(
            batch_id="investor_flow_kospi",
            market="KOSPI",
            timezone=tz,
            cron_minute=kospi_min,
            cron_hour=kospi_hr,
            cron_day_of_week=kospi_dow,
        ),
        InvestorFlowBatchDefinition(
            batch_id="investor_flow_kosdaq",
            market="KOSDAQ",
            timezone=tz,
            cron_minute=kosdaq_min,
            cron_hour=kosdaq_hr,
            cron_day_of_week=kosdaq_dow,
        ),
    ]


# ---------------------------------------------------------------------------
# Batch runner
# ---------------------------------------------------------------------------

def _run_investor_flow_batch(definition: InvestorFlowBatchDefinition, attempt: int = 1) -> BatchRunResult:
    """Execute a single investor flow batch run and return a BatchRunResult.

    Never raises; exceptions are classified as FAILED.
    """
    from src.application.investor_flow_fetch_service import InvestorFlowFetchService
    from src.collectors.investor_flow_collector import InvestorFlowCollector
    from src.repositories.investor_flow_repository import InvestorFlowRepository
    from src.application.daily_fetch_service import load_db_config_from_env
    from src.catalog.postgres_symbol_catalog import connect

    max_attempts = int(os.getenv("BATCH_RETRY_MAX_ATTEMPTS", "4"))
    started_at = time.perf_counter()

    try:
        db_config = load_db_config_from_env()
        collector = InvestorFlowCollector()
        repo = InvestorFlowRepository()
        def conn_factory() -> object:
            return connect(db_config)
        svc = InvestorFlowFetchService(collector, repo, conn_factory)

        result = svc.execute(None, definition.market)
        elapsed_ms = int((time.perf_counter() - started_at) * 1000)

        total_rows = (
            result.get("investor_flow", 0)
            + result.get("short_selling", 0)
            + result.get("program_trading", 0)
            + result.get("foreign_holding", 0)
        )
        errors = result.get("errors", [])
        failed = len(errors)
        success = 4 - failed  # 4 datasets

        if failed > 0 and success > 0:
            status = BatchOutcome.PARTIAL_SUCCESS
        elif failed > 0 and success == 0:
            status = BatchOutcome.FAILED
        elif total_rows == 0:
            status = BatchOutcome.NO_NEW_BAR
        else:
            status = BatchOutcome.SUCCESS

        should_retry = status in {BatchOutcome.PARTIAL_SUCCESS, BatchOutcome.FAILED} and attempt < max_attempts

        return BatchRunResult(
            batch_id=definition.batch_id,
            market=definition.market,
            interval="1d",
            provider="pykrx",
            attempt=attempt,
            status=status,
            symbols=4,
            success_symbols=success,
            failed_symbols=failed,
            total_rows_inserted=total_rows,
            elapsed_ms=elapsed_ms,
            error="; ".join(e["error"] for e in errors) if errors else None,
            should_retry=should_retry,
        )

    except Exception as exc:  # noqa: BLE001
        elapsed_ms = int((time.perf_counter() - started_at) * 1000)
        should_retry = attempt < max_attempts
        return BatchRunResult(
            batch_id=definition.batch_id,
            market=definition.market,
            interval="1d",
            provider="pykrx",
            attempt=attempt,
            status=BatchOutcome.FAILED,
            symbols=0,
            success_symbols=0,
            failed_symbols=0,
            total_rows_inserted=0,
            elapsed_ms=elapsed_ms,
            error=str(exc),
            should_retry=should_retry,
        )


def _run_with_retry(definition: InvestorFlowBatchDefinition) -> None:
    """Execute a batch with exponential backoff retry. Runs in APScheduler thread."""
    attempt = 1
    while True:
        result = _run_investor_flow_batch(definition, attempt=attempt)
        notify_slack(result)

        if not result.should_retry:
            break

        delay = compute_retry_delay_seconds(attempt=attempt)
        logger.info(
            "investor_flow_batch:retry_scheduled batch_id=%s attempt=%d next_in_seconds=%d",
            definition.batch_id,
            attempt + 1,
            delay,
        )
        time.sleep(delay)
        attempt += 1


# ---------------------------------------------------------------------------
# APScheduler lifecycle helpers (called by app.py lifespan)
# ---------------------------------------------------------------------------

def start_investor_flow_scheduler() -> object | None:
    """Create, configure, and start the investor flow BackgroundScheduler.

    Returns the scheduler instance, or None if disabled.
    """
    if not _schedule_enabled():
        logger.info("investor_flow_scheduler:disabled INVESTOR_FLOW_SCHEDULE_ENABLED=false")
        return None

    if BackgroundScheduler is None:
        logger.error("investor_flow_scheduler:apscheduler_not_installed")
        return None

    tz = _batch_timezone()
    scheduler = BackgroundScheduler(timezone=tz)

    for defn in build_investor_flow_batch_definitions():
        trigger = CronTrigger(
            minute=defn.cron_minute,
            hour=defn.cron_hour,
            day_of_week=defn.cron_day_of_week,
            timezone=tz,
        )
        scheduler.add_job(
            _run_with_retry,
            trigger=trigger,
            args=[defn],
            id=defn.batch_id,
            name=f"investor_flow:{defn.batch_id}",
            max_instances=1,
            replace_existing=True,
        )
        logger.info(
            "investor_flow_scheduler:registered batch_id=%s cron=%s:%s dow=%s tz=%s",
            defn.batch_id,
            defn.cron_hour,
            defn.cron_minute,
            defn.cron_day_of_week,
            tz,
        )

    scheduler.start()
    logger.info("investor_flow_scheduler:started timezone=%s", tz)
    return scheduler


def stop_investor_flow_scheduler(scheduler: object | None) -> None:
    """Gracefully shut down the investor flow scheduler."""
    if scheduler is None:
        return
    try:
        scheduler.shutdown(wait=False)
        logger.info("investor_flow_scheduler:stopped")
    except Exception as exc:  # noqa: BLE001
        logger.warning("investor_flow_scheduler:shutdown_error error=%s", str(exc))