"""APScheduler — 차트 분석 배치 스케줄러.

4개 잡:
  chart_analysis_krx     — KRX 수집 잡 완료(EVENT_JOB_EXECUTED) 체인
  chart_analysis_us      — yfinance 수집 잡 완료 체인
  chart_analysis_weekly  — 주봉 수집 잡 완료 체인
  popular_symbols_refresh — CronTrigger: 월요일 04:00 KST

EVENT_JOB_EXECUTED 리스너: 수집 잡 완료 시 → 차트 분석 잡 트리거.
"""
from __future__ import annotations

import asyncio
import logging
import os

logger = logging.getLogger(__name__)

# 수집 잡 batch_id → 차트 분석 트리거 매핑
_COLLECTION_JOB_TO_ANALYSIS: dict[str, str] = {
    "kr_daily": "chart_analysis_krx",
    "us_daily": "chart_analysis_us",
    "kr_weekly": "chart_analysis_weekly",
    "us_weekly": "chart_analysis_weekly",
}

_TIMEZONE = "Asia/Seoul"


def _run_async(coro) -> None:
    """동기 컨텍스트(APScheduler 스레드)에서 async 함수를 실행."""
    try:
        loop = asyncio.get_event_loop()
        if loop.is_running():
            import concurrent.futures
            future = asyncio.run_coroutine_threadsafe(coro, loop)
            future.result(timeout=3600)
        else:
            asyncio.run(coro)
    except Exception as exc:  # noqa: BLE001
        logger.warning("chart_analysis_schedule:async_run_error error=%s", str(exc))


def _run_krx_analysis() -> None:
    from src.chart_analysis.application.market_pipeline_triggers import (
        run_krx_chart_analysis_pipeline,
    )
    _run_async(run_krx_chart_analysis_pipeline())


def _run_us_analysis() -> None:
    from src.chart_analysis.application.market_pipeline_triggers import (
        run_us_chart_analysis_pipeline,
    )
    _run_async(run_us_chart_analysis_pipeline())


def _run_weekly_analysis() -> None:
    from src.chart_analysis.application.market_pipeline_triggers import (
        run_weekly_chart_analysis_pipeline,
    )
    _run_async(run_weekly_chart_analysis_pipeline())


def _run_popular_refresh() -> None:
    """인기 종목 갱신 잡 (주 1회 CronTrigger)."""
    try:
        from src.chart_analysis.application.popular_symbols_refresh_service import (
            PopularSymbolsRefreshService,
        )
        from src.chart_analysis.infrastructure.popular_symbols_repository import (
            PostgresPopularSymbolsRepository,
        )
        from src.chart_analysis.infrastructure.symbol_metrics_repository import (
            PostgresSymbolMetricsRepository,
        )
        from src.application.daily_fetch_service import load_db_config_from_env
        from src.catalog.postgres_symbol_catalog import connect

        db = load_db_config_from_env()
        connect_fn = lambda: connect(db)  # noqa: E731

        service = PopularSymbolsRefreshService(
            symbol_metrics_repository=PostgresSymbolMetricsRepository(connect_fn),
            popular_symbols_repository=PostgresPopularSymbolsRepository(connect_fn),
        )
        inserted = service.refresh()
        logger.info("popular_symbols_refresh:done inserted=%d", inserted)
    except Exception as exc:  # noqa: BLE001
        logger.warning("popular_symbols_refresh:error error=%s", str(exc))


def _on_job_executed(event) -> None:
    """EVENT_JOB_EXECUTED 리스너 — 수집 잡 완료 시 차트 분석 잡 체인.

    수집 잡 batch_id와 매핑된 분석 잡만 트리거.
    """
    job_id: str = event.job_id or ""
    if job_id not in _COLLECTION_JOB_TO_ANALYSIS:
        return

    analysis_job_id = _COLLECTION_JOB_TO_ANALYSIS[job_id]
    logger.info(
        "chart_analysis_schedule:chained source_job=%s analysis_job=%s",
        job_id, analysis_job_id,
    )

    dispatch = {
        "chart_analysis_krx": _run_krx_analysis,
        "chart_analysis_us": _run_us_analysis,
        "chart_analysis_weekly": _run_weekly_analysis,
    }
    fn = dispatch.get(analysis_job_id)
    if fn is not None:
        import threading
        t = threading.Thread(target=fn, daemon=True, name=f"chain:{analysis_job_id}")
        t.start()


def start_chart_analysis_scheduler() -> object:
    """차트 분석 스케줄러 시작. 스케줄러 인스턴스 반환."""
    try:
        from apscheduler.schedulers.background import BackgroundScheduler
        from apscheduler.triggers.cron import CronTrigger
        from apscheduler.events import EVENT_JOB_EXECUTED
    except ImportError:
        logger.error(
            "chart_analysis_schedule:apscheduler_not_installed — "
            "install apscheduler to enable chart analysis scheduling"
        )
        return None

    tz = os.getenv("BATCH_SCHEDULE_TIMEZONE", _TIMEZONE)
    scheduler = BackgroundScheduler(timezone=tz)

    # --- 잡 1: KRX 차트 분석 (수동 체인용 — 직접 cron 없음, listener로 체인)
    scheduler.add_job(
        _run_krx_analysis,
        id="chart_analysis_krx",
        name="chart_analysis:krx",
        # 직접 cron 미설정 — listener 체인으로 실행
        trigger=CronTrigger(hour=23, minute=59, day_of_week="mon-fri", timezone=tz),
        max_instances=1,
        replace_existing=True,
    )

    # --- 잡 2: US 차트 분석
    scheduler.add_job(
        _run_us_analysis,
        id="chart_analysis_us",
        name="chart_analysis:us",
        trigger=CronTrigger(hour=23, minute=59, day_of_week="tue-sat", timezone=tz),
        max_instances=1,
        replace_existing=True,
    )

    # --- 잡 3: 주봉 차트 분석
    scheduler.add_job(
        _run_weekly_analysis,
        id="chart_analysis_weekly",
        name="chart_analysis:weekly",
        trigger=CronTrigger(hour=23, minute=59, day_of_week="sat", timezone=tz),
        max_instances=1,
        replace_existing=True,
    )

    # --- 잡 4: 인기 종목 갱신 (월요일 04:00 KST)
    scheduler.add_job(
        _run_popular_refresh,
        id="popular_symbols_refresh",
        name="popular_symbols:refresh",
        trigger=CronTrigger(hour=4, minute=0, day_of_week="mon", timezone=tz),
        max_instances=1,
        replace_existing=True,
    )

    # EVENT_JOB_EXECUTED 리스너 등록 (수집 잡 → 분석 잡 체인)
    scheduler.add_listener(_on_job_executed, EVENT_JOB_EXECUTED)

    scheduler.start()
    logger.info("chart_analysis_schedule:started timezone=%s", tz)
    return scheduler


def stop_chart_analysis_scheduler(scheduler: object | None) -> None:
    """스케줄러 정상 종료."""
    if scheduler is None:
        return
    try:
        scheduler.shutdown(wait=False)
        logger.info("chart_analysis_schedule:stopped")
    except Exception as exc:  # noqa: BLE001
        logger.warning("chart_analysis_schedule:shutdown_error error=%s", str(exc))