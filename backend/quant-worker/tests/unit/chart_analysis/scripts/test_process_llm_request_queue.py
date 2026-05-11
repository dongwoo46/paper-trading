"""process_llm_request_queue 스크립트 단위 테스트 (TDD)."""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
from decimal import Decimal
from typing import Optional
from unittest.mock import MagicMock, patch

import pytest

from src.chart_analysis.domain.value_objects import (
    Direction,
    Grade,
    LevelSet,
    NarrativeReport,
    Recommendation,
    ReportSource,
    Strength,
    TradePlan,
    TrendAnalysis,
    VolumeAnalysis,
)
from src.chart_analysis.domain.chart_analysis_result import ChartAnalysisResult


# ---------------------------------------------------------------------------
# 테스트용 Fake
# ---------------------------------------------------------------------------

class FakeQueueItem:
    def __init__(self, id: int, symbol: str, window: str, interval: str) -> None:
        self.id = id
        self.symbol = symbol
        self.window = window
        self.interval = interval
        self.status = "pending"


def _make_trend() -> TrendAnalysis:
    return TrendAnalysis(
        direction=Direction.UPTREND,
        strength=Strength.MEDIUM,
        ma_alignment="MA20>MA60",
        adx=Decimal("28"),
        hh_ll_structure="HH",
    )


def _make_result(symbol="005930", window="3M", interval="D") -> ChartAnalysisResult:
    return ChartAnalysisResult(
        symbol=symbol,
        window=window,
        interval=interval,
        snapshot_hash="abc123",
        trend=_make_trend(),
        levels=LevelSet([Decimal("95")], [Decimal("110")]),
        trade_plan=TradePlan(
            entry_price=Decimal("100"),
            stop_loss=Decimal("95"),
            target_price=Decimal("110"),
            risk_reward_ratio=Decimal("2"),
        ),
        patterns=[],
        indicator_signals=[],
        volume_analysis=VolumeAnalysis("flat", False, Decimal("1.0")),
        recommendation=Recommendation(Grade.BUY, Decimal("0.65")),
        report=None,
        report_source=ReportSource.NONE,
        numeric_computed_at=datetime(2026, 5, 1, 8, 0, 0, tzinfo=timezone.utc),
        llm_computed_at=None,
    )


class FakeQueueRepository:
    def __init__(self, items: list[FakeQueueItem] | None = None) -> None:
        self._items = list(items or [])
        self.mark_calls: list[tuple[int, str]] = []

    def fetch_pending(self, limit: int) -> list[FakeQueueItem]:
        return self._items[:limit]

    def mark_processed(self, id: int, status: str) -> None:
        self.mark_calls.append((id, status))
        for item in self._items:
            if item.id == id:
                item.status = status


class FakeChartAnalysisRepository:
    def __init__(self, result: Optional[ChartAnalysisResult] = None) -> None:
        self._result = result
        self.upserted: list[ChartAnalysisResult] = []

    def find_one(self, symbol: str, window: str, interval: str) -> Optional[ChartAnalysisResult]:
        return self._result

    def upsert(self, result: ChartAnalysisResult) -> None:
        self.upserted.append(result)


class FakeLlmGenerator:
    def __init__(self, should_fail: bool = False) -> None:
        self.should_fail = should_fail
        self.call_count = 0

    def generate(self, snapshot, result) -> NarrativeReport:
        self.call_count += 1
        if self.should_fail:
            raise RuntimeError("LLM 호출 실패 (테스트용)")
        return NarrativeReport(
            trend_section="추세 분석",
            support_resistance_section="지지/저항",
            entry_plan_section="진입 계획",
            signal_evidence_section="신호 근거",
            risk_section="리스크",
            source=ReportSource.LLM_PRIMARY,
        )


# ---------------------------------------------------------------------------
# argparse 테스트
# ---------------------------------------------------------------------------

def test_parse_args_default_limit():
    """--limit 미입력 시 기본값 50 사용."""
    from scripts.process_llm_request_queue import parse_args
    args = parse_args([])
    assert args.limit == 50


def test_parse_args_custom_limit():
    """--limit 100 파싱 확인."""
    from scripts.process_llm_request_queue import parse_args
    args = parse_args(["--limit", "100"])
    assert args.limit == 100


# ---------------------------------------------------------------------------
# 상태 전이 테스트
# ---------------------------------------------------------------------------

def test_pending_to_completed_on_success():
    """LLM 성공 시 processing → completed 순서로 갱신, llm_report 업데이트."""
    from scripts.process_llm_request_queue import process_queue

    item = FakeQueueItem(id=1, symbol="005930", window="3M", interval="D")
    queue_repo = FakeQueueRepository([item])
    result = _make_result()
    chart_repo = FakeChartAnalysisRepository(result)
    llm = FakeLlmGenerator(should_fail=False)

    process_queue(
        limit=10,
        queue_repo=queue_repo,
        chart_repo=chart_repo,
        llm_generator=llm,
    )

    assert llm.call_count == 1
    # processing 선점 후 completed 순서로 2회 호출
    assert len(queue_repo.mark_calls) == 2
    assert queue_repo.mark_calls[0] == (1, "processing")
    assert queue_repo.mark_calls[1] == (1, "completed")
    # chart_analysis_result.llm_report 업데이트 확인
    assert len(chart_repo.upserted) == 1
    upserted = chart_repo.upserted[0]
    assert upserted.report is not None


def test_pending_to_failed_on_llm_error():
    """LLM 실패 시 processing → failed 순서로 갱신."""
    from scripts.process_llm_request_queue import process_queue

    item = FakeQueueItem(id=2, symbol="000660", window="1M", interval="D")
    queue_repo = FakeQueueRepository([item])
    result = _make_result(symbol="000660", window="1M")
    chart_repo = FakeChartAnalysisRepository(result)
    llm = FakeLlmGenerator(should_fail=True)

    process_queue(
        limit=10,
        queue_repo=queue_repo,
        chart_repo=chart_repo,
        llm_generator=llm,
    )

    assert llm.call_count == 1
    # processing 선점 후 failed 순서로 2회 호출
    assert len(queue_repo.mark_calls) == 2
    assert queue_repo.mark_calls[0] == (2, "processing")
    assert queue_repo.mark_calls[1] == (2, "failed")
    # llm 실패 시 upsert 되지 않음
    assert len(chart_repo.upserted) == 0


def test_limit_controls_batch_size():
    """--limit N은 최대 N개 pending row만 처리한다."""
    from scripts.process_llm_request_queue import process_queue

    items = [FakeQueueItem(id=i, symbol=f"SYM{i}", window="1M", interval="D") for i in range(5)]
    queue_repo = FakeQueueRepository(items)
    llm = FakeLlmGenerator(should_fail=False)

    def find_one(symbol, window, interval):
        return _make_result(symbol=symbol, window=window, interval=interval)

    chart_repo = FakeChartAnalysisRepository()
    chart_repo.find_one = find_one

    process_queue(limit=3, queue_repo=queue_repo, chart_repo=chart_repo, llm_generator=llm)

    assert llm.call_count == 3, f"limit=3이면 LLM 3회 호출, 실제: {llm.call_count}"


def test_chart_analysis_not_found_marks_failed():
    """chart_analysis_result row가 없으면 processing 선점 후 failed로 표시한다."""
    from scripts.process_llm_request_queue import process_queue

    item = FakeQueueItem(id=9, symbol="MISSING", window="1M", interval="D")
    queue_repo = FakeQueueRepository([item])
    chart_repo = FakeChartAnalysisRepository(result=None)  # None 반환
    llm = FakeLlmGenerator(should_fail=False)

    process_queue(limit=10, queue_repo=queue_repo, chart_repo=chart_repo, llm_generator=llm)

    assert llm.call_count == 0
    # processing 선점 후 failed 순서로 2회 호출
    assert len(queue_repo.mark_calls) == 2
    assert queue_repo.mark_calls[0] == (9, "processing")
    assert queue_repo.mark_calls[1] == (9, "failed")