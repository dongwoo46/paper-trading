"""시장별 차트 분석 파이프라인 트리거 함수.

APScheduler EVENT_JOB_EXECUTED 리스너에 의해 수집 잡 완료 후 체인으로 호출.

함수:
    run_krx_chart_analysis_pipeline()   — 국장 일봉 분석
    run_us_chart_analysis_pipeline()    — 미장 일봉 분석
    run_weekly_chart_analysis_pipeline() — 전체 주봉 분석 (1Y-W, 2Y-W, MAX-W)
"""
from __future__ import annotations

import logging
import os
from typing import Any

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# 내부 헬퍼 — DI 팩토리 (테스트에서 패치)
# ---------------------------------------------------------------------------

def _get_pipeline_service() -> Any:
    """PrecomputePipelineService 인스턴스를 환경변수 기반으로 생성."""
    from src.chart_analysis.application.precompute_pipeline_service import (
        PrecomputePipelineService,
    )
    from src.chart_analysis.infrastructure.indicator_calculator import (
        PandasTaIndicatorCalculator,
    )
    from src.chart_analysis.infrastructure.support_resistance_finder import (
        ScipyPeaksSupportResistanceFinder,
    )
    from src.chart_analysis.infrastructure.pattern_detector import (
        RuleBasedPatternDetector,
    )
    from src.chart_analysis.infrastructure.trend_classifier import (
        MaTrendClassifier,
    )
    from src.chart_analysis.infrastructure.confidence_scorer import (
        RuleWeightedConfidenceScorer,
    )
    from src.chart_analysis.infrastructure.langchain_ollama_report_generator import (
        LangChainOllamaReportGenerator,
    )
    from src.chart_analysis.infrastructure.slack_notifier import SlackWebhookNotifier
    from src.chart_analysis.infrastructure.ohlcv_repository import PostgresOhlcvRepository
    from src.chart_analysis.infrastructure.chart_analysis_repository import (
        PostgresChartAnalysisRepository,
    )
    from src.application.daily_fetch_service import load_db_config_from_env
    from src.catalog.postgres_symbol_catalog import connect

    db = load_db_config_from_env()
    connect_fn = lambda: connect(db)  # noqa: E731

    return PrecomputePipelineService(
        ohlcv_repo=PostgresOhlcvRepository(connect_fn),
        chart_analysis_repo=PostgresChartAnalysisRepository(connect_fn),
        indicator_calculator=PandasTaIndicatorCalculator(),
        sr_finder=ScipyPeaksSupportResistanceFinder(),
        pattern_detector=RuleBasedPatternDetector(),
        trend_classifier=MaTrendClassifier(),
        confidence_scorer=RuleWeightedConfidenceScorer(),
        llm_generator=LangChainOllamaReportGenerator(),
        slack_notifier=SlackWebhookNotifier(),
    )


def _get_weekly_pipeline_service() -> Any:
    """주봉 전용 PrecomputePipelineService (윈도우 필터는 서비스 수준에서 처리 불가 → WEEKLY 마켓 사용)."""
    return _get_pipeline_service()


def _get_krx_symbols() -> list[str]:
    """pykrx 카탈로그에서 enabled 국장 종목 목록 반환."""
    from src.catalog.postgres_symbol_catalog import PostgresSymbolCatalogRepository
    from src.application.daily_fetch_service import load_db_config_from_env
    from src.catalog.postgres_symbol_catalog import connect

    db = load_db_config_from_env()
    repo = PostgresSymbolCatalogRepository(db)
    symbols = repo.list_symbols("pykrx")
    return [s.symbol for s in symbols]


def _get_us_symbols() -> list[str]:
    """yfinance 카탈로그에서 enabled 미장 종목 목록 반환."""
    from src.catalog.postgres_symbol_catalog import PostgresSymbolCatalogRepository
    from src.application.daily_fetch_service import load_db_config_from_env

    db = load_db_config_from_env()
    repo = PostgresSymbolCatalogRepository(db)
    symbols = repo.list_symbols("yfinance")
    return [s.symbol for s in symbols]


def _get_all_symbols() -> list[str]:
    """국장 + 미장 전체 종목 반환."""
    return _get_krx_symbols() + _get_us_symbols()


def _get_popular_set() -> set[str]:
    """popular_symbols 테이블에서 is_popular 종목 코드 집합 반환.

    테이블 미존재 시 빈 set 반환 (graceful degradation).
    """
    try:
        from src.application.daily_fetch_service import load_db_config_from_env
        from src.catalog.postgres_symbol_catalog import connect

        db = load_db_config_from_env()
        with connect(db) as conn:
            with conn.cursor() as cur:
                cur.execute("SELECT symbol FROM popular_symbols")
                rows = cur.fetchall()
        return {row[0] for row in rows}
    except Exception as exc:  # noqa: BLE001
        logger.warning("market_pipeline_triggers:get_popular_set_failed error=%s", str(exc))
        return set()


# ---------------------------------------------------------------------------
# Public 트리거 함수
# ---------------------------------------------------------------------------

async def run_krx_chart_analysis_pipeline() -> dict:
    """국장(KRX) 일봉 차트 분석 파이프라인 실행.

    pykrx 수집 잡 완료 이벤트(EVENT_JOB_EXECUTED) 후 체인 호출.
    """
    logger.info("market_pipeline_triggers:krx_pipeline_start")
    service = _get_pipeline_service()
    symbols = _get_krx_symbols()
    popular = _get_popular_set()
    result = await service.run_for_market("KRX", symbols, popular_set=popular)
    logger.info(
        "market_pipeline_triggers:krx_pipeline_done success=%d failed=%d",
        result["success"], result["failed"],
    )
    return result


async def run_us_chart_analysis_pipeline() -> dict:
    """미장(US) 일봉 차트 분석 파이프라인 실행.

    yfinance 수집 잡 완료 이벤트(EVENT_JOB_EXECUTED) 후 체인 호출.
    """
    logger.info("market_pipeline_triggers:us_pipeline_start")
    service = _get_pipeline_service()
    symbols = _get_us_symbols()
    popular = _get_popular_set()
    result = await service.run_for_market("US", symbols, popular_set=popular)
    logger.info(
        "market_pipeline_triggers:us_pipeline_done success=%d failed=%d",
        result["success"], result["failed"],
    )
    return result


async def run_weekly_chart_analysis_pipeline() -> dict:
    """주봉(1Y-W, 2Y-W, MAX-W) 차트 분석 파이프라인 실행.

    주봉 수집 잡 완료 이벤트(EVENT_JOB_EXECUTED) 후 체인 호출.
    전체 종목(국장+미장)의 주봉 윈도우만 처리한다.
    """
    logger.info("market_pipeline_triggers:weekly_pipeline_start")
    service = _get_weekly_pipeline_service()
    symbols = _get_all_symbols()
    popular = _get_popular_set()
    result = await service.run_for_market("WEEKLY", symbols, popular_set=popular)
    logger.info(
        "market_pipeline_triggers:weekly_pipeline_done success=%d failed=%d",
        result["success"], result["failed"],
    )
    return result