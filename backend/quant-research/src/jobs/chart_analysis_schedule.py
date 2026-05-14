"""APScheduler — 차트 분석 배치 스케줄러.

4개 잡:
  chart_analysis_krx     — CronTrigger: 평일 23:59 KST
  chart_analysis_us      — CronTrigger: 화~토 23:59 KST
  chart_analysis_weekly  — CronTrigger: 토요일 23:59 KST
  popular_symbols_refresh — CronTrigger: 월요일 04:00 KST
"""
from __future__ import annotations

import asyncio
import logging
import os

logger = logging.getLogger(__name__)

_TIMEZONE = "Asia/Seoul"


def _run_async(coro) -> None:
    """동기 컨텍스트(APScheduler 스레드)에서 async 함수를 실행."""
    try:
        loop = asyncio.get_event_loop()
        if loop.is_running():
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
        from src.infrastructure.db import load_db_config_from_env, connect

        db = load_db_config_from_env()
        connect_fn = lambda: connect(db)  # noqa: E731

        # popular_symbols_repository / symbol_metrics_repository는
        # 별도 인프라 어댑터로 구현 예정. 현재는 graceful skip.
        logger.info("popular_symbols_refresh:skipped (repositories not yet implemented)")
    except Exception as exc:  # noqa: BLE001
        logger.warning("popular_symbols_refresh:error error=%s", str(exc))


def start_chart_analysis_scheduler() -> object:
    """차트 분석 스케줄러 시작. 스케줄러 인스턴스 반환."""
    try:
        from apscheduler.schedulers.background import BackgroundScheduler
        from apscheduler.triggers.cron import CronTrigger
    except ImportError:
        logger.error(
            "chart_analysis_schedule:apscheduler_not_installed — "
            "install apscheduler to enable chart analysis scheduling"
        )
        return None

    tz = os.getenv("BATCH_SCHEDULE_TIMEZONE", _TIMEZONE)
    scheduler = BackgroundScheduler(timezone=tz)

    # --- 잡 1: KRX 차트 분석
    scheduler.add_job(
        _run_krx_analysis,
        id="chart_analysis_krx",
        name="chart_analysis:krx",
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
