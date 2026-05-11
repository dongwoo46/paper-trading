"""Substep 5-4: chart-analysis endpoint 통합 테스트 — POST /chart-analysis/{symbol}."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src'))

from datetime import datetime, date
from decimal import Decimal
from typing import List, Optional

import pytest
import pytest_asyncio
import httpx

from chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
from chart_analysis.domain.value_objects import (
    CandlePattern,
    Direction,
    Grade,
    IndicatorSignal,
    LevelSet,
    PatternType,
    Recommendation,
    ReportSource,
    Strength,
    TradePlan,
    TrendAnalysis,
    VolumeAnalysis,
)


# ---------------------------------------------------------------------------
# 헬퍼
# ---------------------------------------------------------------------------

def _make_result(symbol: str, window: str, interval: str) -> ChartAnalysisResult:
    return ChartAnalysisResult(
        symbol=symbol,
        window=window,
        interval=interval,
        snapshot_hash="hash123",
        trend=TrendAnalysis(
            direction=Direction.UPTREND,
            strength=Strength.MEDIUM,
            ma_alignment="MA20>MA60",
            adx=Decimal("23.4"),
            hh_ll_structure="HH",
        ),
        levels=LevelSet(
            supports=[Decimal("68500")],
            resistances=[Decimal("71200")],
        ),
        trade_plan=TradePlan(
            entry_price=Decimal("69400"),
            stop_loss=Decimal("67100"),
            target_price=Decimal("72500"),
            risk_reward_ratio=Decimal("1.35"),
        ),
        patterns=[CandlePattern(type=PatternType.HAMMER, index=1, date=date(2026, 5, 9))],
        indicator_signals=[
            IndicatorSignal(name="RSI", value=Decimal("58.2"), interpretation="neutral")
        ],
        volume_analysis=VolumeAnalysis(
            trend="increasing", spike_detected=False, avg_ratio=Decimal("1.1")
        ),
        recommendation=Recommendation(grade=Grade.BUY, confidence=Decimal("0.612")),
        report=None,
        report_source=ReportSource.NONE,
        numeric_computed_at=datetime(2026, 5, 10, 8, 0, 0),
        llm_computed_at=None,
    )


_WINDOWS = [
    ("1M", "D"), ("3M", "D"), ("6M", "D"), ("1Y", "D"),
    ("1Y", "W"), ("2Y", "W"), ("MAX", "W"),
]


class FakeChartAnalysisRepository:
    def __init__(self, results: Optional[List[ChartAnalysisResult]] = None) -> None:
        self._results: List[ChartAnalysisResult] = results or []

    def upsert(self, r) -> None:
        pass

    def find_by_symbol(self, symbol: str) -> List[ChartAnalysisResult]:
        return [r for r in self._results if r.symbol == symbol]

    def find_one(self, symbol, window, interval):
        for r in self._results:
            if r.symbol == symbol and r.window == window and r.interval == interval:
                return r
        return None


class FakeQueueRepository:
    def enqueue(self, symbol, window, interval): pass
    def fetch_pending(self, limit): return []
    def mark_processed(self, id, status): pass


# ---------------------------------------------------------------------------
# 테스트용 앱 픽스처
# ---------------------------------------------------------------------------

def _build_test_app(results: Optional[List[ChartAnalysisResult]] = None):
    """테스트용 FastAPI 앱 (의존성 오버라이드 포함)."""
    from fastapi import FastAPI
    from chart_analysis.interfaces.chart_analysis_router import (
        chart_analysis_router,
        get_analyze_chart_service,
    )
    from chart_analysis.application.analyze_chart_service import AnalyzeChartService

    app = FastAPI()
    app.include_router(chart_analysis_router)

    repo = FakeChartAnalysisRepository(results=results)
    queue_repo = FakeQueueRepository()
    svc = AnalyzeChartService(
        chart_analysis_repository=repo,
        analysis_request_queue_repository=queue_repo,
    )

    app.dependency_overrides[get_analyze_chart_service] = lambda: svc
    return app


@pytest.mark.asyncio
class TestChartAnalysisEndpoint:

    async def test_normal_returns_200_with_7_analyses(self):
        """정상: 200 + analyses 배열 7개."""
        results = [_make_result("005930", w, i) for w, i in _WINDOWS]
        app = _build_test_app(results=results)

        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app),
            base_url="http://testserver",
        ) as client:
            response = await client.post("/chart-analysis/005930")

        assert response.status_code == 200
        data = response.json()
        assert data["symbol"] == "005930"
        assert len(data["analyses"]) == 7

    async def test_no_rows_returns_200_empty_analyses(self):
        """DB row 0건: 200 + analyses=[]."""
        app = _build_test_app(results=[])

        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app),
            base_url="http://testserver",
        ) as client:
            response = await client.post("/chart-analysis/999999")

        assert response.status_code == 200
        data = response.json()
        assert data["symbol"] == "999999"
        assert data["analyses"] == []

    async def test_response_has_no_float_values(self):
        """응답 JSON에 float 값 없음 (모두 str)."""
        import json

        results = [_make_result("005930", w, i) for w, i in _WINDOWS]
        app = _build_test_app(results=results)

        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app),
            base_url="http://testserver",
        ) as client:
            response = await client.post("/chart-analysis/005930")

        parsed = response.json()
        _assert_no_float_in_analyses(parsed["analyses"])


def _assert_no_float_in_analyses(analyses: list) -> None:
    for window in analyses:
        summary = window["summary"]
        assert isinstance(summary["confidence"], str), f"confidence is float: {summary['confidence']}"
        levels = window["levels"]
        assert isinstance(levels["entry"], str), f"entry is float: {levels['entry']}"