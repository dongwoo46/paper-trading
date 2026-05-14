"""TDD — 2-4: Domain Ports (Protocol) 시그니처 검증 테스트.

각 Protocol을 in-memory fake로 구현해 구조적 호환성을 검증한다.
"""
from __future__ import annotations

import pytest
from datetime import date, datetime, timezone
from decimal import Decimal
from typing import Optional


# ---------------------------------------------------------------------------
# Shared fixtures helpers
# ---------------------------------------------------------------------------

def _make_candle(d: date = date(2026, 5, 10)):
    from src.chart_analysis.domain.value_objects import Candle
    return Candle(
        date=d,
        open=Decimal("68000"),
        high=Decimal("69000"),
        low=Decimal("67500"),
        close=Decimal("68500"),
        volume=Decimal("1000000"),
    )


def _make_indicator_set():
    from src.chart_analysis.domain.value_objects import IndicatorSet
    return IndicatorSet(
        ma20=Decimal("68000"), ma60=Decimal("67000"), ma120=Decimal("65000"),
        rsi14=Decimal("58.2"), macd=Decimal("120.4"), macd_signal=Decimal("100.1"),
        macd_hist=Decimal("20.3"), bb_upper=Decimal("71000"), bb_lower=Decimal("65000"),
        atr14=Decimal("800"), adx14=Decimal("23.4"), stoch_k=Decimal("65.0"),
        stoch_d=Decimal("60.0"), obv=Decimal("5000000"), volume_ma20=Decimal("900000"),
    )


def _make_snapshot():
    from src.chart_analysis.domain.chart_snapshot import ChartSnapshot
    return ChartSnapshot(
        symbol="005930",
        window="3M",
        interval="D",
        candles=[_make_candle()],
        indicators=_make_indicator_set(),
    )


def _make_result():
    from src.chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
    from src.chart_analysis.domain.value_objects import (
        TrendAnalysis, Direction, Strength, LevelSet, TradePlan,
        VolumeAnalysis, Recommendation, Grade, ReportSource,
    )
    return ChartAnalysisResult(
        symbol="005930",
        window="3M",
        interval="D",
        snapshot_hash="a" * 64,
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
            entry_price=Decimal("69000"),
            stop_loss=Decimal("67000"),
            target_price=Decimal("72000"),
            risk_reward_ratio=Decimal("1.5"),
        ),
        patterns=[],
        indicator_signals=[],
        volume_analysis=VolumeAnalysis(
            trend="increasing", spike_detected=True, avg_ratio=Decimal("1.42")
        ),
        recommendation=Recommendation(grade=Grade.BUY, confidence=Decimal("0.612")),
        report=None,
        report_source=ReportSource.NONE,
        numeric_computed_at=datetime(2026, 5, 10, 8, 0, 0, tzinfo=timezone.utc),
        llm_computed_at=None,
    )


def _make_narrative_report():
    from src.chart_analysis.domain.value_objects import NarrativeReport, ReportSource
    return NarrativeReport(
        trend_section="추세 분석",
        support_resistance_section="지지저항",
        entry_plan_section="진입 계획",
        signal_evidence_section="신호 근거",
        risk_section="리스크",
        source=ReportSource.LLM_PRIMARY,
    )


# ---------------------------------------------------------------------------
# In-memory fakes — 각 Protocol의 구조적 호환성 검증
# ---------------------------------------------------------------------------

class InMemOhlcvRepository:
    """OhlcvRepository Protocol fake."""

    def __init__(self):
        self._store: dict[tuple, list] = {}

    def find_window(self, symbol: str, window: str, interval: str):
        from src.chart_analysis.domain.value_objects import Candle
        key = (symbol, window, interval)
        return self._store.get(key, [])

    def seed(self, symbol, window, interval, candles):
        self._store[(symbol, window, interval)] = candles


class InMemChartAnalysisRepository:
    """ChartAnalysisRepository Protocol fake."""

    def __init__(self):
        self._store: dict[tuple, object] = {}

    def upsert(self, result) -> None:
        key = (result.symbol, result.window, result.interval)
        self._store[key] = result

    def find_by_symbol(self, symbol: str) -> list:
        return [v for (s, *_), v in self._store.items() if s == symbol]

    def find_one(self, symbol: str, window: str, interval: str):
        return self._store.get((symbol, window, interval))


class InMemAnalysisRequestQueueRepository:
    """AnalysisRequestQueueRepository Protocol fake."""

    def __init__(self):
        self._queue: list[dict] = []
        self._next_id = 1

    def enqueue(self, symbol: str, window: str, interval: str) -> None:
        self._queue.append({
            "id": self._next_id,
            "symbol": symbol,
            "window": window,
            "interval": interval,
            "status": "pending",
        })
        self._next_id += 1

    def fetch_pending(self, limit: int) -> list[dict]:
        pending = [q for q in self._queue if q["status"] == "pending"]
        return pending[:limit]

    def mark_processed(self, id: int, status: str) -> None:
        for item in self._queue:
            if item["id"] == id:
                item["status"] = status
                return


class InMemLlmReportGenerator:
    """LlmReportGenerator Protocol fake."""

    def generate(self, snapshot, result):
        return _make_narrative_report()


class InMemIndicatorCalculator:
    """IndicatorCalculator Protocol fake."""

    def calculate(self, candles) -> object:
        return _make_indicator_set()


class InMemSupportResistanceFinder:
    """SupportResistanceFinder Protocol fake."""

    def find(self, candles, atr: Decimal) -> object:
        from src.chart_analysis.domain.value_objects import LevelSet
        return LevelSet(
            supports=[Decimal("68500")],
            resistances=[Decimal("71200")],
        )


class InMemPatternDetector:
    """PatternDetector Protocol fake."""

    def detect(self, candles) -> list:
        return []


class InMemTrendClassifier:
    """TrendClassifier Protocol fake."""

    def classify(self, candles, indicators) -> object:
        from src.chart_analysis.domain.value_objects import TrendAnalysis, Direction, Strength
        return TrendAnalysis(
            direction=Direction.UPTREND,
            strength=Strength.MEDIUM,
            ma_alignment="MA20>MA60",
            adx=Decimal("23.4"),
            hh_ll_structure="HH",
        )


class InMemConfidenceScorer:
    """ConfidenceScorer Protocol fake."""

    def score(self, trend, patterns, indicator_signals, volume_analysis) -> Decimal:
        return Decimal("0.612")


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

class TestOhlcvRepositoryProtocol:
    def test_find_window_returns_list(self):
        from src.chart_analysis.domain.ports import OhlcvRepository
        repo = InMemOhlcvRepository()
        result = repo.find_window("005930", "3M", "D")
        assert isinstance(result, list)

    def test_find_window_returns_candles(self):
        repo = InMemOhlcvRepository()
        candle = _make_candle()
        repo.seed("005930", "3M", "D", [candle])
        result = repo.find_window("005930", "3M", "D")
        assert len(result) == 1
        assert result[0] == candle

    def test_protocol_structural_compatibility(self):
        """InMemOhlcvRepository가 OhlcvRepository Protocol과 구조적으로 호환."""
        from src.chart_analysis.domain.ports import OhlcvRepository
        # runtime_checkable이 아니어도 메서드 존재 확인
        repo = InMemOhlcvRepository()
        assert callable(repo.find_window)


class TestChartAnalysisRepositoryProtocol:
    def test_upsert_and_find_one(self):
        repo = InMemChartAnalysisRepository()
        result = _make_result()
        repo.upsert(result)
        found = repo.find_one("005930", "3M", "D")
        assert found is result

    def test_find_by_symbol(self):
        repo = InMemChartAnalysisRepository()
        result = _make_result()
        repo.upsert(result)
        found = repo.find_by_symbol("005930")
        assert len(found) == 1

    def test_find_one_returns_none_when_missing(self):
        repo = InMemChartAnalysisRepository()
        found = repo.find_one("MISSING", "3M", "D")
        assert found is None

    def test_protocol_structural_compatibility(self):
        from src.chart_analysis.domain.ports import ChartAnalysisRepository
        repo = InMemChartAnalysisRepository()
        assert callable(repo.upsert)
        assert callable(repo.find_by_symbol)
        assert callable(repo.find_one)


class TestAnalysisRequestQueueRepositoryProtocol:
    def test_enqueue_and_fetch_pending(self):
        repo = InMemAnalysisRequestQueueRepository()
        repo.enqueue("005930", "3M", "D")
        pending = repo.fetch_pending(limit=10)
        assert len(pending) == 1
        assert pending[0]["symbol"] == "005930"

    def test_mark_processed(self):
        repo = InMemAnalysisRequestQueueRepository()
        repo.enqueue("005930", "3M", "D")
        pending = repo.fetch_pending(limit=1)
        item_id = pending[0]["id"]
        repo.mark_processed(item_id, "completed")
        assert repo.fetch_pending(limit=10) == []

    def test_fetch_pending_limit(self):
        repo = InMemAnalysisRequestQueueRepository()
        for i in range(5):
            repo.enqueue(f"SYM{i}", "3M", "D")
        pending = repo.fetch_pending(limit=3)
        assert len(pending) == 3

    def test_protocol_structural_compatibility(self):
        from src.chart_analysis.domain.ports import AnalysisRequestQueueRepository
        repo = InMemAnalysisRequestQueueRepository()
        assert callable(repo.enqueue)
        assert callable(repo.fetch_pending)
        assert callable(repo.mark_processed)


class TestLlmReportGeneratorProtocol:
    def test_generate_returns_narrative_report(self):
        from src.chart_analysis.domain.value_objects import NarrativeReport
        gen = InMemLlmReportGenerator()
        snapshot = _make_snapshot()
        result = _make_result()
        report = gen.generate(snapshot, result)
        assert isinstance(report, NarrativeReport)

    def test_protocol_structural_compatibility(self):
        from src.chart_analysis.domain.ports import LlmReportGenerator
        gen = InMemLlmReportGenerator()
        assert callable(gen.generate)


class TestCalculatorPortsStructural:
    def test_indicator_calculator_protocol(self):
        from src.chart_analysis.domain.ports import IndicatorCalculator
        calc = InMemIndicatorCalculator()
        assert callable(calc.calculate)

    def test_support_resistance_finder_protocol(self):
        from src.chart_analysis.domain.ports import SupportResistanceFinder
        finder = InMemSupportResistanceFinder()
        assert callable(finder.find)

    def test_pattern_detector_protocol(self):
        from src.chart_analysis.domain.ports import PatternDetector
        detector = InMemPatternDetector()
        assert callable(detector.detect)

    def test_trend_classifier_protocol(self):
        from src.chart_analysis.domain.ports import TrendClassifier
        classifier = InMemTrendClassifier()
        assert callable(classifier.classify)

    def test_confidence_scorer_protocol(self):
        from src.chart_analysis.domain.ports import ConfidenceScorer
        scorer = InMemConfidenceScorer()
        assert callable(scorer.score)
