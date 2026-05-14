"""Substep 5-3: GenerateReportService 단위 테스트 — SSE 스트림."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

import asyncio
from datetime import datetime, date
from decimal import Decimal
from typing import AsyncIterator, List, Optional

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
# Helpers / Fakes
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
        snapshot_hash="hash_abc",
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


def _make_report() -> NarrativeReport:
    return NarrativeReport(
        trend_section="추세 분석",
        support_resistance_section="지지저항 분석",
        entry_plan_section="진입 계획",
        signal_evidence_section="신호 근거",
        risk_section="리스크",
        source=ReportSource.LLM_PRIMARY,
    )


class FakeChartAnalysisRepository:
    def __init__(self, result: Optional[ChartAnalysisResult] = None) -> None:
        self._result = result
        self.upsert_called = False
        self.upserted = None

    def upsert(self, result: ChartAnalysisResult) -> None:
        self.upsert_called = True
        self.upserted = result

    def find_by_symbol(self, symbol: str) -> List[ChartAnalysisResult]:
        if self._result and self._result.symbol == symbol:
            return [self._result]
        return []

    def find_one(self, symbol: str, window: str, interval: str):
        if (self._result
                and self._result.symbol == symbol
                and self._result.window == window
                and self._result.interval == interval):
            return self._result
        return None


class FakeLlmReportGenerator:
    def __init__(
        self,
        report: Optional[NarrativeReport] = None,
        raise_exc: Optional[Exception] = None,
    ) -> None:
        self._report = report
        self._raise = raise_exc
        self.call_count = 0

    def generate(self, snapshot, result) -> NarrativeReport:
        self.call_count += 1
        if self._raise is not None:
            raise self._raise
        if self._report is None:
            return _make_report()
        return self._report


class FakeRedisJobStore:
    def __init__(self, lock_result: bool = True) -> None:
        self._lock_result = lock_result
        self._locks: dict = {}
        self._statuses: dict = {}

    def acquire_lock(self, snapshot_hash: str, ttl: int) -> bool:
        if snapshot_hash in self._locks:
            return False
        self._locks[snapshot_hash] = True
        return self._lock_result

    def release_lock(self, snapshot_hash: str) -> None:
        self._locks.pop(snapshot_hash, None)

    def set_status(self, job_id: str, stage: str) -> None:
        self._statuses[job_id] = stage

    def get_status(self, job_id: str) -> Optional[str]:
        return self._statuses.get(job_id)


class FakeSlackWebhookNotifier:
    def __init__(self) -> None:
        self.success_calls: List[dict] = []
        self.failure_calls: List[dict] = []
        self.batch_calls: List[dict] = []

    async def notify_analysis_success(self, symbol: str, window: str, source: str) -> None:
        self.success_calls.append({"symbol": symbol, "window": window, "source": source})

    async def notify_analysis_failure(self, symbol: str, window: str, error: str) -> None:
        self.failure_calls.append({"symbol": symbol, "window": window, "error": error})

    async def notify_batch_completed(self, market: str, success_count: int, failed_count: int) -> None:
        self.batch_calls.append({"market": market, "success": success_count, "failed": failed_count})


def _popular_symbols_fn(symbol: str) -> bool:
    """인기 종목 여부 (005930 = 인기)."""
    return symbol == "005930"


async def _collect_events(gen: AsyncIterator) -> List[dict]:
    events = []
    async for event in gen:
        events.append(event)
    return events


# ---------------------------------------------------------------------------
# 테스트
# ---------------------------------------------------------------------------

@pytest.mark.asyncio
class TestGenerateReportService:

    async def test_scenario1_report_exists_in_db(self):
        """시나리오 1: DB에 LLM 리포트 존재 → status:running → report → end."""
        from chart_analysis.application.generate_report_service import GenerateReportService

        report = _make_report()
        result = _make_result(report=report, report_source=ReportSource.LLM_PRIMARY)

        repo = FakeChartAnalysisRepository(result=result)
        llm_gen = FakeLlmReportGenerator()
        redis_store = FakeRedisJobStore()
        slack = FakeSlackWebhookNotifier()

        svc = GenerateReportService(
            chart_analysis_repository=repo,
            llm_report_generator=llm_gen,
            redis_job_store=redis_store,
            popular_symbols_fn=_popular_symbols_fn,
            slack_notifier=slack,
        )

        events = await _collect_events(svc.stream("005930", "1M", "D"))

        event_types = [e["event"] for e in events]
        assert "status" in event_types
        assert "report" in event_types
        assert "end" in event_types

        # running status
        status_event = next(e for e in events if e["event"] == "status")
        assert status_event["data"]["stage"] == "running"

        # report event
        report_event = next(e for e in events if e["event"] == "report")
        assert "narrative" in report_event["data"]

        # LLM 재호출 없음 (DB 캐시 활용)
        assert llm_gen.call_count == 0

    async def test_scenario2_no_report_popular_calls_llm(self):
        """시나리오 2: 리포트 없음 + 인기 → LLM 호출 → 결과 저장 → 이벤트 push."""
        from chart_analysis.application.generate_report_service import GenerateReportService

        result = _make_result(report=None, report_source=ReportSource.NONE)
        repo = FakeChartAnalysisRepository(result=result)
        llm_gen = FakeLlmReportGenerator()
        redis_store = FakeRedisJobStore()
        slack = FakeSlackWebhookNotifier()

        svc = GenerateReportService(
            chart_analysis_repository=repo,
            llm_report_generator=llm_gen,
            redis_job_store=redis_store,
            popular_symbols_fn=_popular_symbols_fn,
            slack_notifier=slack,
        )

        events = await _collect_events(svc.stream("005930", "1M", "D"))

        assert llm_gen.call_count == 1
        assert repo.upsert_called

        report_event = next(e for e in events if e["event"] == "report")
        assert "narrative" in report_event["data"]

        # Slack success 알림 1회
        assert len(slack.success_calls) == 1
        assert slack.success_calls[0]["symbol"] == "005930"
        assert slack.success_calls[0]["source"] == "llm_primary"

    async def test_scenario3_no_report_unpopular_returns_none(self):
        """시나리오 3: 리포트 없음 + 비인기 → status:none → end."""
        from chart_analysis.application.generate_report_service import GenerateReportService

        result = _make_result(symbol="UNKNOWN", report=None)
        repo = FakeChartAnalysisRepository(result=result)
        llm_gen = FakeLlmReportGenerator()
        redis_store = FakeRedisJobStore()
        slack = FakeSlackWebhookNotifier()

        svc = GenerateReportService(
            chart_analysis_repository=repo,
            llm_report_generator=llm_gen,
            redis_job_store=redis_store,
            popular_symbols_fn=_popular_symbols_fn,
            slack_notifier=slack,
        )

        events = await _collect_events(svc.stream("UNKNOWN", "1M", "D"))

        status_event = next(e for e in events if e["event"] == "status")
        assert status_event["data"]["stage"] == "none"

        end_event = next((e for e in events if e["event"] == "end"), None)
        assert end_event is not None

        assert llm_gen.call_count == 0

    async def test_scenario4_duplicate_request_dedup_by_lock(self):
        """시나리오 4: 동시 동일 snapshot_hash → 락 보유자만 LLM 호출."""
        from chart_analysis.application.generate_report_service import GenerateReportService

        result = _make_result(report=None)
        repo = FakeChartAnalysisRepository(result=result)
        llm_gen = FakeLlmReportGenerator()
        # 이미 락이 걸려있는 store (첫 번째 요청이 락 보유)
        redis_store = FakeRedisJobStore(lock_result=False)
        # acquire_lock이 False를 반환하도록 미리 잠금
        redis_store._locks["hash_abc"] = True
        slack = FakeSlackWebhookNotifier()

        svc = GenerateReportService(
            chart_analysis_repository=repo,
            llm_report_generator=llm_gen,
            redis_job_store=redis_store,
            popular_symbols_fn=_popular_symbols_fn,
            slack_notifier=slack,
        )

        events = await _collect_events(svc.stream("005930", "1M", "D"))

        # 락 실패 → LLM 호출 없음
        assert llm_gen.call_count == 0
        event_types = [e["event"] for e in events]
        assert "end" in event_types

    async def test_scenario5_llm_exception_fallback_and_slack_failure(self):
        """시나리오 5: LLM 예외 → 룰 템플릿 폴백 + slack_notifier.notify_analysis_failure 1회."""
        from chart_analysis.application.generate_report_service import GenerateReportService

        result = _make_result(report=None)
        repo = FakeChartAnalysisRepository(result=result)
        llm_gen = FakeLlmReportGenerator(raise_exc=RuntimeError("LLM timeout"))
        redis_store = FakeRedisJobStore()
        slack = FakeSlackWebhookNotifier()

        svc = GenerateReportService(
            chart_analysis_repository=repo,
            llm_report_generator=llm_gen,
            redis_job_store=redis_store,
            popular_symbols_fn=_popular_symbols_fn,
            slack_notifier=slack,
        )

        events = await _collect_events(svc.stream("005930", "1M", "D"))

        # 폴백 report 이벤트 존재
        report_event = next((e for e in events if e["event"] == "report"), None)
        assert report_event is not None
        assert report_event["data"]["source"] == "rule_template"

        # Slack failure 알림 1회
        assert len(slack.failure_calls) == 1
        assert slack.failure_calls[0]["symbol"] == "005930"
        assert "LLM timeout" in slack.failure_calls[0]["error"]

        # success 알림은 없음
        assert len(slack.success_calls) == 0

    async def test_scenario6_llm_success_slack_notify(self):
        """시나리오 6: LLM 성공 → notify_analysis_success(source='llm_primary') 1회."""
        from chart_analysis.application.generate_report_service import GenerateReportService

        result = _make_result(report=None)
        repo = FakeChartAnalysisRepository(result=result)
        llm_gen = FakeLlmReportGenerator()
        redis_store = FakeRedisJobStore()
        slack = FakeSlackWebhookNotifier()

        svc = GenerateReportService(
            chart_analysis_repository=repo,
            llm_report_generator=llm_gen,
            redis_job_store=redis_store,
            popular_symbols_fn=_popular_symbols_fn,
            slack_notifier=slack,
        )

        await _collect_events(svc.stream("005930", "1M", "D"))

        assert len(slack.success_calls) == 1
        call = slack.success_calls[0]
        assert call["symbol"] == "005930"
        assert call["window"] == "1M"
        assert call["source"] == "llm_primary"
        assert len(slack.failure_calls) == 0