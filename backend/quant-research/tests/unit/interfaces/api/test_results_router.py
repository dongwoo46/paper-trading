"""results_router 단위 테스트 — TestClient + dependency_overrides."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

from datetime import datetime, timezone
from decimal import Decimal
from unittest.mock import MagicMock

import pytest
from fastapi import FastAPI
from fastapi.testclient import TestClient

from chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
from chart_analysis.domain.value_objects import (
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
from interfaces.api.results_router import results_router, get_chart_analysis_repo


def _make_result(symbol: str = "005930") -> ChartAnalysisResult:
    now = datetime(2024, 1, 2, tzinfo=timezone.utc)
    trend = TrendAnalysis(
        direction=Direction.UPTREND,
        strength=Strength.STRONG,
        ma_alignment="MA20>MA60",
        adx=Decimal("25"),
        hh_ll_structure="HH",
    )
    levels = LevelSet(supports=[Decimal("68000")], resistances=[Decimal("72000")])
    trade_plan = TradePlan(
        entry_price=Decimal("70000"),
        stop_loss=Decimal("68000"),
        target_price=Decimal("72000"),
        risk_reward_ratio=Decimal("1.0"),
    )
    volume = VolumeAnalysis(trend="flat", spike_detected=False, avg_ratio=Decimal("1.0"))
    rec = Recommendation(grade=Grade.BUY, confidence=Decimal("0.7"))

    return ChartAnalysisResult(
        symbol=symbol,
        window="1M",
        interval="D",
        snapshot_hash="abc123",
        trend=trend,
        levels=levels,
        trade_plan=trade_plan,
        patterns=[],
        indicator_signals=[],
        volume_analysis=volume,
        recommendation=rec,
        report=None,
        report_source=ReportSource.NONE,
        numeric_computed_at=now,
        llm_computed_at=None,
    )


def _make_test_app(mock_repo) -> FastAPI:
    """결과 라우터만 포함한 최소 FastAPI 앱."""
    test_app = FastAPI()
    test_app.include_router(results_router)
    test_app.dependency_overrides[get_chart_analysis_repo] = lambda: mock_repo
    return test_app


class TestResultsRouter:

    def test_get_results_returns_empty_when_no_data(self):
        """find_by_symbol → [] → 응답 analyses 빈 목록."""
        mock_repo = MagicMock()
        mock_repo.find_by_symbol.return_value = []

        app = _make_test_app(mock_repo)
        client = TestClient(app)

        resp = client.get("/research/results/005930")

        assert resp.status_code == 200
        body = resp.json()
        assert body["symbol"] == "005930"
        assert body["analyses"] == []

    def test_get_results_returns_analyses(self):
        """find_by_symbol → [ChartAnalysisResult] → 응답에 analyses 1개."""
        result = _make_result("005930")
        mock_repo = MagicMock()
        mock_repo.find_by_symbol.return_value = [result]

        app = _make_test_app(mock_repo)
        client = TestClient(app)

        resp = client.get("/research/results/005930")

        assert resp.status_code == 200
        body = resp.json()
        assert body["symbol"] == "005930"
        assert len(body["analyses"]) == 1
        analysis = body["analyses"][0]
        assert analysis["symbol"] == "005930"
        assert analysis["window"] == "1M"

    def test_run_blank_symbol_returns_422(self):
        """POST /research/run/%20 (blank symbol) → 422 반환."""
        mock_repo = MagicMock()
        app = _make_test_app(mock_repo)
        client = TestClient(app)

        resp = client.post("/research/run/%20")

        assert resp.status_code == 422
