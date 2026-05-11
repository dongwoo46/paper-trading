"""Substep 5-5: SSE endpoint 통합 테스트 — POST /chart-analysis/{symbol}/report."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src'))

from datetime import datetime, date
from decimal import Decimal
from typing import AsyncIterator, List, Optional

import pytest
import httpx

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
        snapshot_hash="hash_sse",
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


def _make_narrative() -> NarrativeReport:
    return NarrativeReport(
        trend_section="추세 분석",
        support_resistance_section="지지저항",
        entry_plan_section="진입 계획",
        signal_evidence_section="신호 근거",
        risk_section="리스크",
        source=ReportSource.LLM_PRIMARY,
    )


class FakeChartAnalysisRepository:
    def __init__(self, result: Optional[ChartAnalysisResult] = None) -> None:
        self._result = result

    def upsert(self, r): self._result = r
    def find_by_symbol(self, symbol): return [self._result] if self._result else []
    def find_one(self, symbol, window, interval):
        if (self._result
                and self._result.symbol == symbol
                and self._result.window == window
                and self._result.interval == interval):
            return self._result
        return None


class FakeLlmGenerator:
    def __init__(self, report=None, raise_exc=None):
        self._report = report
        self._raise = raise_exc

    def generate(self, snapshot, result):
        if self._raise:
            raise self._raise
        return self._report or _make_narrative()


class FakeRedisStore:
    def __init__(self):
        self._locks = {}
        self._statuses = {}

    def acquire_lock(self, key, ttl): return self._locks.setdefault(key, True) and True
    def release_lock(self, key): self._locks.pop(key, None)
    def set_status(self, job_id, stage): self._statuses[job_id] = stage
    def get_status(self, job_id): return self._statuses.get(job_id)


class FakeSlack:
    async def notify_analysis_success(self, symbol, window, source): pass
    async def notify_analysis_failure(self, symbol, window, error): pass
    async def notify_batch_completed(self, market, success_count, failed_count): pass


def _parse_sse_body(body: str) -> List[dict]:
    """SSE body를 이벤트 목록으로 파싱."""
    import json
    events = []
    current_event = {}
    for line in body.splitlines():
        line = line.strip()
        if line.startswith("event:"):
            current_event["event"] = line[6:].strip()
        elif line.startswith("data:"):
            try:
                current_event["data"] = json.loads(line[5:].strip())
            except Exception:
                current_event["data"] = line[5:].strip()
        elif line == "" and current_event:
            events.append(current_event)
            current_event = {}
    if current_event:
        events.append(current_event)
    return events


def _build_sse_app(result: Optional[ChartAnalysisResult] = None, popular: bool = True):
    from fastapi import FastAPI
    from chart_analysis.interfaces.chart_analysis_router import (
        chart_analysis_router,
        get_generate_report_service,
    )
    from chart_analysis.application.generate_report_service import GenerateReportService

    app = FastAPI()
    app.include_router(chart_analysis_router)

    repo = FakeChartAnalysisRepository(result=result)
    llm = FakeLlmGenerator()
    redis = FakeRedisStore()
    slack = FakeSlack()

    svc = GenerateReportService(
        chart_analysis_repository=repo,
        llm_report_generator=llm,
        redis_job_store=redis,
        popular_symbols_fn=lambda s: popular,
        slack_notifier=slack,
    )
    app.dependency_overrides[get_generate_report_service] = lambda: svc
    return app


@pytest.mark.asyncio
class TestChartAnalysisSseEndpoint:

    async def test_report_exists_sse_order(self):
        """SSE 이벤트 순서: status -> report -> end (리포트 존재)."""
        narrative = _make_narrative()
        result = _make_result(report=narrative, report_source=ReportSource.LLM_PRIMARY)
        app = _build_sse_app(result=result)

        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app),
            base_url="http://testserver",
        ) as client:
            response = await client.post("/chart-analysis/005930/report")

        assert response.status_code == 200
        assert "text/event-stream" in response.headers.get("content-type", "")

        events = _parse_sse_body(response.text)
        event_types = [e.get("event") for e in events]
        assert "status" in event_types
        assert "report" in event_types
        assert "end" in event_types

        # 순서 검증
        status_idx = event_types.index("status")
        report_idx = event_types.index("report")
        end_idx = event_types.index("end")
        assert status_idx < report_idx < end_idx

    async def test_no_report_returns_status_none(self):
        """리포트 부재 + 비인기 → status:none -> end."""
        result = _make_result(report=None, report_source=ReportSource.NONE)
        app = _build_sse_app(result=result, popular=False)

        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app),
            base_url="http://testserver",
        ) as client:
            response = await client.post("/chart-analysis/005930/report")

        events = _parse_sse_body(response.text)
        status_event = next(e for e in events if e.get("event") == "status")
        assert status_event["data"]["stage"] == "none"

        end_event = next((e for e in events if e.get("event") == "end"), None)
        assert end_event is not None
