"""PrecomputePipelineService 단위 테스트."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

import asyncio
from datetime import date, datetime, timedelta, timezone
from decimal import Decimal
from unittest.mock import AsyncMock, MagicMock, patch

import pytest

from chart_analysis.application.precompute_pipeline_service import (
    PrecomputePipelineService,
    _build_trade_plan,
)
from chart_analysis.domain.value_objects import (
    Candle,
    CandlePattern,
    Direction,
    Grade,
    IndicatorSet,
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
from chart_analysis.domain.chart_analysis_result import ChartAnalysisResult


def _make_candle(close: str = "70000", idx: int = 0) -> Candle:
    return Candle(
        date=date(2024, 1, 1) + timedelta(days=idx),
        open=Decimal("69000"),
        high=Decimal("71000"),
        low=Decimal("68000"),
        close=Decimal(close),
        volume=Decimal("1000000"),
    )


def _make_candles(n: int = 10) -> list[Candle]:
    return [_make_candle(str(70000 + i * 100), i) for i in range(n)]


def _make_indicator_set() -> IndicatorSet:
    d = Decimal("1")
    return IndicatorSet(
        ma20=d, ma60=d, ma120=d, rsi14=Decimal("50"),
        macd=d, macd_signal=d, macd_hist=d,
        bb_upper=d, bb_lower=d, atr14=d, adx14=Decimal("25"),
        stoch_k=d, stoch_d=d, obv=d, volume_ma20=Decimal("500000"),
    )


def _make_trend() -> TrendAnalysis:
    return TrendAnalysis(
        direction=Direction.UPTREND,
        strength=Strength.STRONG,
        ma_alignment="MA20>MA60",
        adx=Decimal("25"),
        hh_ll_structure="HH",
    )


def _make_result(symbol: str = "005930", hash_val: str = "hash_old") -> ChartAnalysisResult:
    now = datetime(2024, 1, 2, tzinfo=timezone.utc)
    trend = _make_trend()
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
        snapshot_hash=hash_val,
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


def _make_service(
    ohlcv_candles=None,
    existing_result=None,
) -> tuple[PrecomputePipelineService, dict]:
    """의존성 모두 mock한 서비스 인스턴스와 mock 딕셔너리 반환."""
    mocks = {}

    ohlcv_repo = MagicMock()
    ohlcv_repo.find_window.return_value = ohlcv_candles if ohlcv_candles is not None else []
    mocks["ohlcv_repo"] = ohlcv_repo

    chart_repo = MagicMock()
    chart_repo.find_one.return_value = existing_result
    mocks["chart_repo"] = chart_repo

    indicator_calc = MagicMock()
    indicator_calc.calculate.return_value = _make_indicator_set()
    mocks["indicator_calc"] = indicator_calc

    sr_finder = MagicMock()
    sr_finder.find.return_value = LevelSet(
        supports=[Decimal("68000")], resistances=[Decimal("72000")]
    )
    mocks["sr_finder"] = sr_finder

    pattern_detector = MagicMock()
    pattern_detector.detect.return_value = []
    mocks["pattern_detector"] = pattern_detector

    trend_classifier = MagicMock()
    trend_classifier.classify.return_value = _make_trend()
    mocks["trend_classifier"] = trend_classifier

    confidence_scorer = MagicMock()
    confidence_scorer.score.return_value = Recommendation(
        grade=Grade.BUY, confidence=Decimal("0.7")
    )
    mocks["confidence_scorer"] = confidence_scorer

    llm_generator = MagicMock()
    mocks["llm_generator"] = llm_generator

    slack_notifier = MagicMock()
    slack_notifier.notify_batch_completed = AsyncMock()
    mocks["slack_notifier"] = slack_notifier

    queue_repo = MagicMock()
    mocks["queue_repo"] = queue_repo

    svc = PrecomputePipelineService(
        ohlcv_repo=ohlcv_repo,
        chart_analysis_repo=chart_repo,
        indicator_calculator=indicator_calc,
        sr_finder=sr_finder,
        pattern_detector=pattern_detector,
        trend_classifier=trend_classifier,
        confidence_scorer=confidence_scorer,
        llm_generator=llm_generator,
        slack_notifier=slack_notifier,
        queue_repo=queue_repo,
    )
    return svc, mocks


class TestPrecomputePipelineService:

    def test_run_for_symbol_skips_when_too_few_candles(self):
        """ohlcv_repo가 빈 목록 반환 → skipped=1 (7 윈도우 모두 스킵)."""
        svc, mocks = _make_service(ohlcv_candles=[])

        result = asyncio.get_event_loop().run_until_complete(
            svc.run_for_symbol("005930", is_popular=False)
        )

        assert result["skipped"] == 7
        assert result["success"] == 0

    def test_run_for_symbol_enqueues_popular(self):
        """is_popular=True, 해시 변경 시 queue_repo.enqueue 호출 확인."""
        candles = _make_candles(240)
        # existing result with different hash → triggers recompute
        existing = _make_result(hash_val="old_hash")
        svc, mocks = _make_service(ohlcv_candles=candles, existing_result=existing)

        # patch ChartSnapshot.compute_hash so it returns a different hash
        with patch(
            "chart_analysis.application.precompute_pipeline_service.ChartSnapshot"
        ) as MockSnapshot:
            mock_snap = MagicMock()
            mock_snap.compute_hash.return_value = "new_hash_different"
            MockSnapshot.return_value = mock_snap

            result = asyncio.get_event_loop().run_until_complete(
                svc.run_for_symbol("005930", is_popular=True)
            )

        # All 7 windows processed successfully → enqueue called 7 times
        assert mocks["queue_repo"].enqueue.called
        assert result["success"] == 7

    def test_run_for_symbol_no_enqueue_when_not_popular(self):
        """is_popular=False → queue_repo.enqueue 미호출."""
        candles = _make_candles(240)
        existing = _make_result(hash_val="old_hash")
        svc, mocks = _make_service(ohlcv_candles=candles, existing_result=existing)

        with patch(
            "chart_analysis.application.precompute_pipeline_service.ChartSnapshot"
        ) as MockSnapshot:
            mock_snap = MagicMock()
            mock_snap.compute_hash.return_value = "new_hash_different"
            MockSnapshot.return_value = mock_snap

            asyncio.get_event_loop().run_until_complete(
                svc.run_for_symbol("005930", is_popular=False)
            )

        mocks["queue_repo"].enqueue.assert_not_called()

    def test_build_trade_plan_uses_nearest_valid_levels_on_correct_side(self):
        """trade plan은 현재가 아래 nearest support와 현재가 위 nearest resistance를 선택한다."""
        levels = LevelSet(
            supports=[Decimal("101"), Decimal("97"), Decimal("94")],
            resistances=[Decimal("99"), Decimal("103"), Decimal("110")],
        )

        plan = _build_trade_plan(levels, Decimal("100"))

        assert plan.stop_loss == Decimal("97")
        assert plan.target_price == Decimal("103")
        assert isinstance(plan.stop_loss, Decimal)
        assert isinstance(plan.target_price, Decimal)

    def test_pipeline_passes_window_and_price_context_to_sr_and_confidence(self):
        """pipeline은 finder와 confidence scorer에 window/current-price context를 전달한다."""
        candles = _make_candles(60)
        existing = _make_result(hash_val="old_hash")
        svc, mocks = _make_service(ohlcv_candles=candles, existing_result=existing)

        with patch(
            "chart_analysis.application.precompute_pipeline_service.ChartSnapshot"
        ) as MockSnapshot:
            mock_snap = MagicMock()
            mock_snap.compute_hash.return_value = "new_hash_different"
            MockSnapshot.return_value = mock_snap

            asyncio.get_event_loop().run_until_complete(
                svc._process_window("005930", "3M", "D", is_popular=False)
            )

        mocks["sr_finder"].find.assert_called_once_with(
            candles,
            _make_indicator_set().atr14,
            window="3M",
            interval="D",
            last_close=candles[-1].close,
        )
        assert mocks["confidence_scorer"].score.call_args.kwargs["levels"] == mocks["sr_finder"].find.return_value
        assert mocks["confidence_scorer"].score.call_args.kwargs["last_close"] == candles[-1].close
        assert isinstance(mocks["confidence_scorer"].score.call_args.kwargs["trade_plan"], TradePlan)
