"""시장별 차트 분석 파이프라인 트리거 함수.

APScheduler EVENT_JOB_EXECUTED 리스너에 의해 수집 잡 완료 후 체인으로 호출.

함수:
    run_krx_chart_analysis_pipeline()   — 국장 일봉 분석
    run_us_chart_analysis_pipeline()    — 미장 일봉 분석
    run_weekly_chart_analysis_pipeline() — 전체 주봉 분석 (1Y-W, 2Y-W, MAX-W)
"""
from __future__ import annotations

import logging
from typing import Any

logger = logging.getLogger(__name__)


# ---------------------------------------------------------------------------
# 내부 헬퍼 — DI 팩토리 (테스트에서 패치)
# ---------------------------------------------------------------------------

def _get_pipeline_service() -> Any:
    """PrecomputePipelineService 인스턴스를 환경변수 기반으로 생성.

    quant-research는 자체 계산기를 직접 인스턴스화한다 (ResearchClient 경유 금지).
    """
    from src.chart_analysis.application.precompute_pipeline_service import (
        PrecomputePipelineService,
    )
    from src.chart_analysis.infrastructure.indicator_calculator import (
        PandasTaIndicatorCalculator,
    )
    from src.chart_analysis.infrastructure.trend_classifier import (
        MaAdxTrendClassifier,
    )
    from src.chart_analysis.infrastructure.pattern_detector import (
        RuleBasedPatternDetector,
    )
    from src.chart_analysis.infrastructure.support_resistance_finder import (
        ScipyPeakSupportResistanceFinder,
    )
    from src.chart_analysis.infrastructure.confidence_scorer import (
        WeightedRuleConfidenceScorer,
    )
    from src.chart_analysis.infrastructure.ohlcv_repository import PostgresOhlcvRepository
    from src.chart_analysis.infrastructure.chart_analysis_repository import (
        PostgresChartAnalysisRepository,
    )
    from src.chart_analysis.infrastructure.analysis_request_queue_repository import (
        PostgresAnalysisRequestQueueRepository,
    )
    from src.infrastructure.db import load_db_config_from_env, connect

    db = load_db_config_from_env()
    connect_fn = lambda: connect(db)  # noqa: E731

    return PrecomputePipelineService(
        ohlcv_repo=PostgresOhlcvRepository(connect_fn),
        chart_analysis_repo=PostgresChartAnalysisRepository(connect_fn),
        indicator_calculator=PandasTaIndicatorCalculator(),
        sr_finder=ScipyPeakSupportResistanceFinder(),
        pattern_detector=RuleBasedPatternDetector(),
        trend_classifier=MaAdxTrendClassifier(),
        confidence_scorer=WeightedRuleConfidenceScorer(),
        llm_generator=_get_null_llm_generator(),
        slack_notifier=_get_slack_notifier(),
        queue_repo=PostgresAnalysisRequestQueueRepository(connect_fn),
    )


def _get_null_llm_generator() -> Any:
    """quant-research는 LLM 리포트를 생성하지 않는다 (quant-ai 역할). Null 객체 반환."""
    class _NullLlmGenerator:
        def generate(self, snapshot: object, result: object) -> Any:
            raise NotImplementedError("LLM report generation is not supported in quant-research")

    return _NullLlmGenerator()


def _get_slack_notifier() -> Any:
    """Slack 알림 — 환경변수 SLACK_WEBHOOK_URL 기반."""
    import os

    class _NullSlackNotifier:
        async def notify_batch_completed(self, market: str, success: int, failed: int) -> None:
            logger.info(
                "batch_completed market=%s success=%d failed=%d",
                market, success, failed,
            )

    class _SlackWebhookNotifier:
        def __init__(self, webhook_url: str) -> None:
            self._url = webhook_url

        async def notify_batch_completed(self, market: str, success: int, failed: int) -> None:
            import urllib.request
            import json
            payload = json.dumps({
                "text": f"[quant-research] {market} 차트 분석 완료 — 성공: {success}, 실패: {failed}"
            }).encode()
            try:
                req = urllib.request.Request(self._url, data=payload, headers={"Content-Type": "application/json"})
                urllib.request.urlopen(req, timeout=5)
            except Exception as exc:  # noqa: BLE001
                logger.warning("slack_notifier:failed error=%s", str(exc))

    webhook_url = os.getenv("SLACK_WEBHOOK_URL", "")
    if webhook_url:
        return _SlackWebhookNotifier(webhook_url)
    return _NullSlackNotifier()


def _get_krx_symbols() -> list[str]:
    """pykrx 카탈로그에서 enabled 국장 종목 목록 반환."""
    from src.infrastructure.db import load_db_config_from_env, connect

    db = load_db_config_from_env()
    with connect(db) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT symbol FROM pykrx_symbol_catalog WHERE enabled = TRUE ORDER BY id ASC")
            return [row[0] for row in cur.fetchall()]


def _get_us_symbols() -> list[str]:
    """yfinance 카탈로그에서 enabled 미장 종목 목록 반환."""
    from src.infrastructure.db import load_db_config_from_env, connect

    db = load_db_config_from_env()
    with connect(db) as conn:
        with conn.cursor() as cur:
            cur.execute("SELECT ticker FROM yfinance_symbol_catalog WHERE enabled = TRUE ORDER BY id ASC")
            return [row[0] for row in cur.fetchall()]


def _get_all_symbols() -> list[str]:
    """국장 + 미장 전체 종목 반환."""
    return _get_krx_symbols() + _get_us_symbols()


def _get_popular_set() -> set[str]:
    """popular_symbols 테이블에서 is_popular 종목 코드 집합 반환.

    테이블 미존재 시 빈 set 반환 (graceful degradation).
    """
    try:
        from src.infrastructure.db import load_db_config_from_env, connect

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
    service = _get_pipeline_service()
    symbols = _get_all_symbols()
    popular = _get_popular_set()
    result = await service.run_for_market("WEEKLY", symbols, popular_set=popular)
    logger.info(
        "market_pipeline_triggers:weekly_pipeline_done success=%d failed=%d",
        result["success"], result["failed"],
    )
    return result
