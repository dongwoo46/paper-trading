"""PrecomputePipelineService 단위 테스트 (TDD — Red 먼저 작성)."""
from __future__ import annotations

import logging
from datetime import date, datetime
from decimal import Decimal
from typing import Optional
from unittest.mock import AsyncMock, MagicMock, call, patch

import pytest

from src.chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
from src.chart_analysis.domain.chart_snapshot import ChartSnapshot
from src.chart_analysis.domain.value_objects import (
    Candle,
    CandlePattern,
    Direction,
    Grade,
    IndicatorSet,
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
# 테스트용 헬퍼 — Candle + IndicatorSet 팩토리
# ---------------------------------------------------------------------------

def _make_candle(i: int) -> Candle:
    p = Decimal(str(100 + i))
    return Candle(
        date=date(2026, 1, i % 28 + 1),
        open=p,
        high=p + Decimal("2"),
        low=p - Decimal("1"),
        close=p + Decimal("1"),
        volume=Decimal("1000"),
    )


def _make_indicators() -> IndicatorSet:
    d = Decimal("50")
    return IndicatorSet(
        ma20=d, ma60=d, ma120=d, rsi14=d, macd=d,
        macd_signal=d, macd_hist=d, bb_upper=d + Decimal("5"),
        bb_lower=d - Decimal("5"), atr14=Decimal("2"),
        adx14=Decimal("30"), stoch_k=d, stoch_d=d,
        obv=Decimal("100000"), volume_ma20=Decimal("1000"),
    )


def _make_trend() -> TrendAnalysis:
    return TrendAnalysis(
        direction=Direction.UPTREND,
        strength=Strength.MEDIUM,
        ma_alignment="MA20>MA60",
        adx=Decimal("30"),
        hh_ll_structure="HH",
    )


def _make_level_set() -> LevelSet:
    return LevelSet(
        supports=[Decimal("95"), Decimal("90")],
        resistances=[Decimal("110"), Decimal("115")],
    )


def _make_trade_plan() -> TradePlan:
    return TradePlan(
        entry_price=Decimal("100"),
        stop_loss=Decimal("95"),
        target_price=Decimal("110"),
        risk_reward_ratio=Decimal("2.0"),
    )


def _make_recommendation(confidence: str = "0.65") -> Recommendation:
    return Recommendation(grade=Grade.BUY, confidence=Decimal(confidence))


def _make_volume_analysis() -> VolumeAnalysis:
    return VolumeAnalysis(trend="increasing", spike_detected=False, avg_ratio=Decimal("1.1"))


def _make_result(
    symbol: str = "005930",
    window: str = "1M",
    interval: str = "D",
    snapshot_hash: str = "aaa",
) -> ChartAnalysisResult:
    return ChartAnalysisResult(
        symbol=symbol,
        window=window,
        interval=interval,
        snapshot_hash=snapshot_hash,
        trend=_make_trend(),
        levels=_make_level_set(),
        trade_plan=_make_trade_plan(),
        patterns=[],
        indicator_signals=[],
        volume_analysis=_make_volume_analysis(),
        recommendation=_make_recommendation(),
        report=None,
        report_source=ReportSource.NONE,
        numeric_computed_at=datetime(2026, 5, 1, 8, 0, 0),
        llm_computed_at=None,
    )


# ---------------------------------------------------------------------------
# Fake 포트 구현
# ---------------------------------------------------------------------------

_WINDOWS = [
    ("1M", "D"), ("3M", "D"), ("6M", "D"), ("1Y", "D"),
    ("1Y", "W"), ("2Y", "W"), ("MAX", "W"),
]


class FakeOhlcvRepository:
    """기본적으로 22개 candle 반환."""

    def __init__(self, candles_by_key: dict | None = None) -> None:
        self._map = candles_by_key or {}

    def find_window(self, symbol: str, window: str, interval: str) -> list[Candle]:
        key = (symbol, window, interval)
        if key in self._map:
            return self._map[key]
        return [_make_candle(i) for i in range(22)]


class FakeChartAnalysisRepository:
    def __init__(self, existing: dict | None = None) -> None:
        self.upserted: list[ChartAnalysisResult] = []
        self._existing: dict = existing or {}

    def upsert(self, result: ChartAnalysisResult) -> None:
        self.upserted.append(result)

    def find_by_symbol(self, symbol: str) -> list[ChartAnalysisResult]:
        return list(self._existing.get(symbol, []))

    def find_one(self, symbol: str, window: str, interval: str) -> Optional[ChartAnalysisResult]:
        for r in self._existing.get(symbol, []):
            if r.window == window and r.interval == interval:
                return r
        return None


class FakeIndicatorCalculator:
    def __init__(self, result: IndicatorSet | None = None) -> None:
        self._result = result or _make_indicators()
        self.call_count = 0

    def calculate(self, candles: list[Candle]) -> IndicatorSet:
        self.call_count += 1
        return self._result


class FakeSupportResistanceFinder:
    def find(self, candles, atr) -> LevelSet:
        return _make_level_set()


class FakePatternDetector:
    def detect(self, candles) -> list[CandlePattern]:
        return []


class FakeTrendClassifier:
    def classify(self, candles, indicators) -> TrendAnalysis:
        return _make_trend()


class FakeConfidenceScorer:
    def __init__(self, score: str = "0.65") -> None:
        self._score = Decimal(score)

    def score(self, trend, patterns, indicator_signals, volume_analysis) -> Decimal:
        return self._score


class FakeLlmReportGenerator:
    def __init__(self) -> None:
        self.call_count = 0

    def generate(self, snapshot, result) -> NarrativeReport:
        self.call_count += 1
        return NarrativeReport(
            trend_section="추세 분석 테스트",
            support_resistance_section="지지/저항 테스트",
            entry_plan_section="진입 계획 테스트",
            signal_evidence_section="신호 근거 테스트",
            risk_section="리스크 테스트",
            source=ReportSource.LLM_PRIMARY,
        )


class FakeSlackNotifier:
    def __init__(self) -> None:
        self.batch_completed_calls: list[tuple] = []

    async def notify_batch_completed(
        self, market: str, success_count: int, failed_count: int
    ) -> None:
        self.batch_completed_calls.append((market, success_count, failed_count))


# ---------------------------------------------------------------------------
# 픽스처
# ---------------------------------------------------------------------------


def _make_service(
    ohlcv_repo: FakeOhlcvRepository | None = None,
    chart_repo: FakeChartAnalysisRepository | None = None,
    indicator_calc: FakeIndicatorCalculator | None = None,
    sr_finder: FakeSupportResistanceFinder | None = None,
    pattern_detector: FakePatternDetector | None = None,
    trend_classifier: FakeTrendClassifier | None = None,
    confidence_scorer: FakeConfidenceScorer | None = None,
    llm_generator: FakeLlmReportGenerator | None = None,
    slack_notifier: FakeSlackNotifier | None = None,
):
    from src.chart_analysis.application.precompute_pipeline_service import (
        PrecomputePipelineService,
    )

    return PrecomputePipelineService(
        ohlcv_repo=ohlcv_repo or FakeOhlcvRepository(),
        chart_analysis_repo=chart_repo or FakeChartAnalysisRepository(),
        indicator_calculator=indicator_calc or FakeIndicatorCalculator(),
        sr_finder=sr_finder or FakeSupportResistanceFinder(),
        pattern_detector=pattern_detector or FakePatternDetector(),
        trend_classifier=trend_classifier or FakeTrendClassifier(),
        confidence_scorer=confidence_scorer or FakeConfidenceScorer(),
        llm_generator=llm_generator or FakeLlmReportGenerator(),
        slack_notifier=slack_notifier or FakeSlackNotifier(),
    )


# ---------------------------------------------------------------------------
# 시나리오 1: snapshot_hash 동일 → 수치/LLM 모두 스킵, numeric_computed_at만 갱신
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_scenario1_same_hash_skips_llm_for_unchanged_window():
    """기존 row와 snapshot_hash가 동일한 윈도우는 LLM 호출을 스킵한다.

    7개 윈도우 모두 기존 row가 있고 hash가 동일하게 설정 →
    is_popular=True 이더라도 LLM 호출 0회 (모두 스킵).
    """
    from src.chart_analysis.domain.chart_snapshot import ChartSnapshot as CS

    # FakeOhlcvRepository가 모든 윈도우에 동일한 candle 집합을 반환하도록 설정
    candles = [_make_candle(i) for i in range(22)]
    indicators = _make_indicators()
    indicator_calc = FakeIndicatorCalculator(result=indicators)

    # 모든 윈도우에 대해 동일 hash를 가진 기존 row 준비
    all_existing = []
    for window, interval in _WINDOWS:
        snap = CS("005930", window, interval, candles, indicators)
        real_hash = snap.compute_hash()
        all_existing.append(_make_result(window=window, interval=interval, snapshot_hash=real_hash))

    chart_repo = FakeChartAnalysisRepository(existing={"005930": all_existing})

    # 모든 윈도우에 동일 candle 반환
    ohlcv_repo = FakeOhlcvRepository()  # 기본 22개 반환

    llm = FakeLlmReportGenerator()
    service = _make_service(
        ohlcv_repo=ohlcv_repo,
        chart_repo=chart_repo,
        indicator_calc=indicator_calc,
        llm_generator=llm,
    )

    result = await service.run_for_symbol("005930", is_popular=True)

    # 모든 hash 동일 → LLM 호출 0회
    assert llm.call_count == 0, (
        f"동일 hash일 때 LLM 호출 금지, 실제 call_count={llm.call_count}"
    )
    # numeric_computed_at 갱신용 upsert는 7회
    assert len(chart_repo.upserted) == 7
    assert result["success"] == 7


# ---------------------------------------------------------------------------
# 시나리오 2: snapshot_hash 다름 + is_popular=True → 수치 + LLM 호출
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_scenario2_different_hash_popular_calls_llm():
    """hash가 다르고 is_popular=True면 수치와 LLM 모두 호출한다."""
    llm = FakeLlmReportGenerator()
    chart_repo = FakeChartAnalysisRepository()

    service = _make_service(chart_repo=chart_repo, llm_generator=llm)

    result = await service.run_for_symbol("005930", is_popular=True)

    # 7 윈도우 모두 새 hash → LLM 7회 호출
    assert llm.call_count == 7, f"is_popular=True면 LLM 7회 호출해야 함, 실제: {llm.call_count}"
    # 7 윈도우 모두 upsert
    assert len(chart_repo.upserted) == 7
    assert result["success"] == 7
    assert result["failed"] == 0


# ---------------------------------------------------------------------------
# 시나리오 3: snapshot_hash 다름 + is_popular=False → 수치만, LLM 스킵
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_scenario3_different_hash_not_popular_skips_llm():
    """hash가 다르고 is_popular=False면 수치만 계산하고 LLM은 호출하지 않는다."""
    llm = FakeLlmReportGenerator()
    chart_repo = FakeChartAnalysisRepository()

    service = _make_service(chart_repo=chart_repo, llm_generator=llm)

    result = await service.run_for_symbol("005930", is_popular=False)

    assert llm.call_count == 0, f"is_popular=False면 LLM 호출 금지, 실제: {llm.call_count}"
    assert len(chart_repo.upserted) == 7
    assert result["success"] == 7


# ---------------------------------------------------------------------------
# 시나리오 4: 일부 윈도우 OHLCV 데이터 부족(candles=[]) → 해당 윈도우 스킵 + 경고 로그
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_scenario4_empty_candles_skips_window(caplog):
    """candles가 빈 윈도우는 스킵되고 경고 로그가 남는다."""
    # MAX/W 윈도우만 빈 데이터
    ohlcv_repo = FakeOhlcvRepository(candles_by_key={("005930", "MAX", "W"): []})
    chart_repo = FakeChartAnalysisRepository()

    service = _make_service(ohlcv_repo=ohlcv_repo, chart_repo=chart_repo)

    with caplog.at_level(logging.WARNING):
        result = await service.run_for_symbol("005930", is_popular=False)

    # MAX/W 스킵 → 6 성공, 1 스킵
    assert result["success"] == 6
    assert result["skipped"] >= 1
    # 경고 로그가 남아야 함
    assert any("candles" in rec.message.lower() or "skip" in rec.message.lower()
               for rec in caplog.records), "빈 candle 경고 로그 누락"


# ---------------------------------------------------------------------------
# 시나리오 5: run_for_market 완료 → slack notify_batch_completed 1회 호출
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_scenario5_run_for_market_calls_slack_once():
    """run_for_market 완료 시 notify_batch_completed가 정확히 1회 호출된다."""
    slack = FakeSlackNotifier()
    service = _make_service(slack_notifier=slack)

    await service.run_for_market("KRX", ["005930", "000660"], popular_set={"005930"})

    assert len(slack.batch_completed_calls) == 1
    market, success, failed = slack.batch_completed_calls[0]
    assert market == "KRX"
    assert success >= 0
    assert failed >= 0


# ---------------------------------------------------------------------------
# 시나리오 6: run_for_market 전체 실패 → Slack 알림 전송 (failed_count > 0)
# ---------------------------------------------------------------------------


@pytest.mark.asyncio
async def test_scenario6_all_failed_slack_with_failed_count():
    """run_for_market에서 모든 종목 실패 시 failed_count > 0으로 Slack 알림 전송."""

    class BrokenIndicatorCalculator:
        def calculate(self, candles):
            raise RuntimeError("DB 연결 실패")

    slack = FakeSlackNotifier()
    service = _make_service(
        indicator_calc=BrokenIndicatorCalculator(),
        slack_notifier=slack,
    )

    await service.run_for_market("KRX", ["005930"], popular_set=set())

    assert len(slack.batch_completed_calls) == 1
    _, _, failed = slack.batch_completed_calls[0]
    assert failed > 0, "전체 실패 시 failed_count > 0 이어야 함"