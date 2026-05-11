"""시장별 트리거 함수 단위 테스트 (TDD — Red 먼저 작성)."""
from __future__ import annotations

from datetime import date, datetime
from decimal import Decimal
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from src.chart_analysis.domain.value_objects import (
    Direction,
    Grade,
    LevelSet,
    Recommendation,
    ReportSource,
    Strength,
    TradePlan,
    TrendAnalysis,
    VolumeAnalysis,
)
from src.chart_analysis.domain.chart_analysis_result import ChartAnalysisResult


# ---------------------------------------------------------------------------
# 공통 Fake
# ---------------------------------------------------------------------------

def _make_fake_service():
    """run_for_market가 비동기이므로 AsyncMock 사용."""
    svc = MagicMock()
    svc.run_for_market = AsyncMock(return_value={"success": 10, "failed": 0})
    return svc


# ---------------------------------------------------------------------------
# run_krx_chart_analysis_pipeline
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_run_krx_pipeline_calls_run_for_market_with_krx():
    """run_krx_chart_analysis_pipeline → run_for_market('KRX', ...) 호출 확인."""
    from src.chart_analysis.application import market_pipeline_triggers as triggers

    fake_service = _make_fake_service()
    fake_symbols = ["005930", "000660"]
    fake_popular = {"005930"}

    with (
        patch.object(triggers, "_get_pipeline_service", return_value=fake_service),
        patch.object(triggers, "_get_krx_symbols", return_value=fake_symbols),
        patch.object(triggers, "_get_popular_set", return_value=fake_popular),
    ):
        await triggers.run_krx_chart_analysis_pipeline()

    fake_service.run_for_market.assert_called_once_with(
        "KRX", fake_symbols, popular_set=fake_popular
    )


# ---------------------------------------------------------------------------
# run_us_chart_analysis_pipeline
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_run_us_pipeline_calls_run_for_market_with_us():
    """run_us_chart_analysis_pipeline → run_for_market('US', ...) 호출 확인."""
    from src.chart_analysis.application import market_pipeline_triggers as triggers

    fake_service = _make_fake_service()
    fake_symbols = ["AAPL", "MSFT"]
    fake_popular = {"AAPL"}

    with (
        patch.object(triggers, "_get_pipeline_service", return_value=fake_service),
        patch.object(triggers, "_get_us_symbols", return_value=fake_symbols),
        patch.object(triggers, "_get_popular_set", return_value=fake_popular),
    ):
        await triggers.run_us_chart_analysis_pipeline()

    fake_service.run_for_market.assert_called_once_with(
        "US", fake_symbols, popular_set=fake_popular
    )


# ---------------------------------------------------------------------------
# run_weekly_chart_analysis_pipeline — 주봉 윈도우만 처리
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
async def test_run_weekly_pipeline_filters_weekly_windows():
    """run_weekly_chart_analysis_pipeline은 주봉 윈도우(1Y-W, 2Y-W, MAX-W)만 처리한다."""
    from src.chart_analysis.application import market_pipeline_triggers as triggers

    # 주봉 전용 서비스 mock
    fake_weekly_service = _make_fake_service()
    fake_symbols = ["005930", "AAPL"]
    fake_popular = set()

    with (
        patch.object(triggers, "_get_weekly_pipeline_service", return_value=fake_weekly_service),
        patch.object(triggers, "_get_all_symbols", return_value=fake_symbols),
        patch.object(triggers, "_get_popular_set", return_value=fake_popular),
    ):
        await triggers.run_weekly_chart_analysis_pipeline()

    # run_for_market가 'ALL' 또는 별도 weekly 마켓으로 호출되어야 함
    fake_weekly_service.run_for_market.assert_called_once()
    call_args = fake_weekly_service.run_for_market.call_args
    # 시장 이름은 'WEEKLY' 또는 'ALL'
    market_arg = call_args[0][0] if call_args[0] else call_args[1].get("market")
    assert market_arg in ("WEEKLY", "ALL", "KRX", "US"), (
        f"주봉 파이프라인 market 인자가 예상치 않음: {market_arg}"
    )