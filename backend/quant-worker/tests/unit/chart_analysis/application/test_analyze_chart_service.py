"""Substep 5-2: AnalyzeChartService 단위 테스트."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

from datetime import datetime, date
from decimal import Decimal
from typing import List, Optional

import pytest

from chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
from chart_analysis.domain.value_objects import (
    CandlePattern,
    Direction,
    Grade,
    IndicatorSignal,
    LevelSet,
    NarrativeReport,
    PatternType,
    Recommendation,
    ReportSource,
    Strength,
    TradePlan,
    TrendAnalysis,
    VolumeAnalysis,
)


# ---------------------------------------------------------------------------
# Fake 저장소
# ---------------------------------------------------------------------------

class FakeChartAnalysisRepository:
    def __init__(self, results: Optional[List[ChartAnalysisResult]] = None) -> None:
        self._results: List[ChartAnalysisResult] = results or []

    def upsert(self, result: ChartAnalysisResult) -> None:
        pass

    def find_by_symbol(self, symbol: str) -> List[ChartAnalysisResult]:
        return [r for r in self._results if r.symbol == symbol]

    def find_one(self, symbol: str, window: str, interval: str) -> Optional[ChartAnalysisResult]:
        for r in self._results:
            if r.symbol == symbol and r.window == window and r.interval == interval:
                return r
        return None


class FakeAnalysisRequestQueueRepository:
    def __init__(self) -> None:
        self._queue: List[dict] = []

    def enqueue(self, symbol: str, window: str, interval: str) -> None:
        for item in self._queue:
            if (item["symbol"] == symbol
                    and item["window"] == window
                    and item["interval"] == interval
                    and item["status"] == "pending"):
                item["requested_count"] += 1
                return
        self._queue.append({
            "symbol": symbol,
            "window": window,
            "interval": interval,
            "status": "pending",
            "requested_count": 1,
        })

    def fetch_pending(self, limit: int) -> List[dict]:
        return [q for q in self._queue if q["status"] == "pending"][:limit]

    def mark_processed(self, id: int, status: str) -> None:
        pass

    def is_pending(self, symbol: str, window: str, interval: str) -> bool:
        """테스트용 헬퍼."""
        return any(
            q["symbol"] == symbol
            and q["window"] == window
            and q["interval"] == interval
            and q["status"] == "pending"
            for q in self._queue
        )


# ---------------------------------------------------------------------------
# 헬퍼
# ---------------------------------------------------------------------------

def _make_result(
    symbol: str = "005930",
    window: str = "1M",
    interval: str = "D",
    report: Optional[NarrativeReport] = None,
    report_source: ReportSource = ReportSource.NONE,
) -> ChartAnalysisResult:
    return ChartAnalysisResult(
        symbol=symbol,
        window=window,
        interval=interval,
        snapshot_hash="abc123",
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
        recommendation=Recommendation(grade=Grade.BUY, confidence=Decimal("0.6")),
        report=report,
        report_source=report_source,
        numeric_computed_at=datetime(2026, 5, 10, 8, 0, 0),
        llm_computed_at=None,
    )


_WINDOWS = [
    ("1M", "D"), ("3M", "D"), ("6M", "D"), ("1Y", "D"),
    ("1Y", "W"), ("2Y", "W"), ("MAX", "W"),
]


class TestAnalyzeChartServiceGetAnalyses:
    """AnalyzeChartService.get_analyses() 단위 테스트."""

    def test_returns_7_windows_in_order(self):
        """7 윈도우 정렬 반환 검증."""
        from chart_analysis.application.analyze_chart_service import AnalyzeChartService

        results = [_make_result(window=w, interval=i) for w, i in _WINDOWS]
        repo = FakeChartAnalysisRepository(results=results)
        queue_repo = FakeAnalysisRequestQueueRepository()

        svc = AnalyzeChartService(
            chart_analysis_repository=repo,
            analysis_request_queue_repository=queue_repo,
        )
        response = svc.get_analyses("005930")

        assert response.symbol == "005930"
        assert len(response.analyses) == 7
        for idx, (w, i) in enumerate(_WINDOWS):
            assert response.analyses[idx].window == w
            assert response.analyses[idx].interval == i

    def test_no_db_rows_returns_empty(self):
        """DB row 0건 → analyses=[]."""
        from chart_analysis.application.analyze_chart_service import AnalyzeChartService

        repo = FakeChartAnalysisRepository(results=[])
        queue_repo = FakeAnalysisRequestQueueRepository()

        svc = AnalyzeChartService(
            chart_analysis_repository=repo,
            analysis_request_queue_repository=queue_repo,
        )
        response = svc.get_analyses("999999")

        assert response.symbol == "999999"
        assert response.analyses == []

    def test_report_status_available_when_report_exists(self):
        """llm_report != null -> report.status='available'."""
        from chart_analysis.application.analyze_chart_service import AnalyzeChartService

        report = NarrativeReport(
            trend_section="추세",
            support_resistance_section="지지저항",
            entry_plan_section="진입",
            signal_evidence_section="신호",
            risk_section="리스크",
            source=ReportSource.LLM_PRIMARY,
        )
        result = _make_result(report=report, report_source=ReportSource.LLM_PRIMARY)
        repo = FakeChartAnalysisRepository(results=[result])
        queue_repo = FakeAnalysisRequestQueueRepository()

        svc = AnalyzeChartService(
            chart_analysis_repository=repo,
            analysis_request_queue_repository=queue_repo,
        )
        response = svc.get_analyses("005930")

        assert response.analyses[0].report.status == "available"

    def test_report_status_pending_when_queued(self):
        """큐에 등록됨 → report.status='pending'."""
        from chart_analysis.application.analyze_chart_service import AnalyzeChartService

        result = _make_result(report=None, report_source=ReportSource.NONE)
        repo = FakeChartAnalysisRepository(results=[result])
        queue_repo = FakeAnalysisRequestQueueRepository()
        queue_repo.enqueue("005930", "1M", "D")

        svc = AnalyzeChartService(
            chart_analysis_repository=repo,
            analysis_request_queue_repository=queue_repo,
        )
        response = svc.get_analyses("005930")

        assert response.analyses[0].report.status == "pending"

    def test_report_status_none_when_no_report_and_not_queued(self):
        """리포트 없음 + 큐 없음 → report.status='none'."""
        from chart_analysis.application.analyze_chart_service import AnalyzeChartService

        result = _make_result(report=None, report_source=ReportSource.NONE)
        repo = FakeChartAnalysisRepository(results=[result])
        queue_repo = FakeAnalysisRequestQueueRepository()

        svc = AnalyzeChartService(
            chart_analysis_repository=repo,
            analysis_request_queue_repository=queue_repo,
        )
        response = svc.get_analyses("005930")

        assert response.analyses[0].report.status == "none"