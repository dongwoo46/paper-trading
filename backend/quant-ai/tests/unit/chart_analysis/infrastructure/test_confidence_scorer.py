"""TDD — ConfidenceScorer 단위 테스트.

Red 단계: WeightedRuleConfidenceScorer 구현 전 작성.
Green 단계: 구현 후 통과 확인.
"""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

from decimal import Decimal

import pytest

from chart_analysis.domain.value_objects import (
    CandlePattern,
    Direction,
    Grade,
    IndicatorSignal,
    LevelSet,
    PatternType,
    Recommendation,
    Strength,
    TradePlan,
    TrendAnalysis,
    VolumeAnalysis,
)
from datetime import date


def _make_trend(direction: Direction, strength: Strength, adx: str = "25") -> TrendAnalysis:
    return TrendAnalysis(
        direction=direction,
        strength=strength,
        ma_alignment="MA20>MA60" if direction == Direction.UPTREND else "MA20<MA60",
        adx=Decimal(adx),
        hh_ll_structure="HH" if direction == Direction.UPTREND else "LL",
    )


def _make_neutral_volume() -> VolumeAnalysis:
    return VolumeAnalysis(trend="neutral", spike_detected=False, avg_ratio=Decimal("1.0"))


def _make_spike_volume(bullish: bool = True) -> VolumeAnalysis:
    return VolumeAnalysis(
        trend="up" if bullish else "down",
        spike_detected=True,
        avg_ratio=Decimal("1.5"),
    )


def _bullish_pattern(ptype: PatternType = PatternType.HAMMER) -> CandlePattern:
    return CandlePattern(type=ptype, index=0, date=date(2024, 1, 1))


def _bearish_pattern() -> CandlePattern:
    return CandlePattern(type=PatternType.BEARISH_ENGULFING, index=0, date=date(2024, 1, 1))


def _rsi_signal(value: str) -> IndicatorSignal:
    return IndicatorSignal(name="RSI", value=Decimal(value), interpretation="RSI")


def _trade_plan(stop: str, target: str) -> TradePlan:
    return TradePlan(
        entry_price=Decimal("100"),
        stop_loss=Decimal(stop),
        target_price=Decimal(target),
        risk_reward_ratio=Decimal("1.00"),
    )


class TestWeightedRuleConfidenceScorer:
    """WeightedRuleConfidenceScorer 단위 테스트."""

    @pytest.fixture
    def scorer(self):
        from chart_analysis.infrastructure.confidence_scorer import WeightedRuleConfidenceScorer
        return WeightedRuleConfidenceScorer()

    # ------------------------------------------------------------------ #
    # 1. 반환 타입
    # ------------------------------------------------------------------ #

    def test_returns_recommendation_type(self, scorer):
        """score() → Recommendation 타입."""
        result = scorer.score(
            trend=_make_trend(Direction.UPTREND, Strength.STRONG),
            patterns=[],
            indicator_signals=[],
            volume_analysis=_make_neutral_volume(),
        )
        assert isinstance(result, Recommendation)

    def test_confidence_within_range(self, scorer):
        """confidence는 항상 [0.0, 1.0] 범위 내."""
        for direction in Direction:
            for strength in Strength:
                result = scorer.score(
                    trend=TrendAnalysis(
                        direction=direction,
                        strength=strength,
                        ma_alignment="FLAT",
                        adx=Decimal("20"),
                        hh_ll_structure="RANGING",
                    ),
                    patterns=[],
                    indicator_signals=[],
                    volume_analysis=_make_neutral_volume(),
                )
                assert Decimal("0") <= result.confidence <= Decimal("1"), (
                    f"confidence {result.confidence} out of [0,1] for {direction}/{strength}"
                )

    # ------------------------------------------------------------------ #
    # 2. 강세 신호만 → STRONG_BUY 또는 BUY
    # ------------------------------------------------------------------ #

    def test_all_bullish_signals_gives_buy_grade(self, scorer):
        """MA 정배열 + ADX strong + 강세 패턴 + RSI<30 + volume spike → BUY 또는 STRONG_BUY."""
        result = scorer.score(
            trend=_make_trend(Direction.UPTREND, Strength.STRONG, adx="30"),
            patterns=[_bullish_pattern(PatternType.HAMMER)],
            indicator_signals=[_rsi_signal("25")],
            volume_analysis=_make_spike_volume(bullish=True),
        )
        assert result.grade in (Grade.BUY, Grade.STRONG_BUY), (
            f"Expected BUY/STRONG_BUY but got {result.grade} (confidence={result.confidence})"
        )
        assert result.confidence > Decimal("0.5")

    def test_all_bullish_signals_high_confidence(self, scorer):
        """강세 신호만 있을 때 confidence > 0.5."""
        result = scorer.score(
            trend=_make_trend(Direction.UPTREND, Strength.STRONG, adx="30"),
            patterns=[_bullish_pattern(PatternType.MORNING_STAR)],
            indicator_signals=[_rsi_signal("28")],
            volume_analysis=_make_spike_volume(bullish=True),
        )
        assert result.confidence > Decimal("0.5")

    # ------------------------------------------------------------------ #
    # 3. 약세 신호만 → SELL 또는 STRONG_SELL
    # ------------------------------------------------------------------ #

    def test_all_bearish_signals_gives_sell_grade(self, scorer):
        """MA 역배열 + ADX strong + 약세 패턴 + RSI>70 → SELL 또는 STRONG_SELL."""
        result = scorer.score(
            trend=_make_trend(Direction.DOWNTREND, Strength.STRONG, adx="30"),
            patterns=[_bearish_pattern()],
            indicator_signals=[_rsi_signal("75")],
            volume_analysis=_make_neutral_volume(),
        )
        assert result.grade in (Grade.SELL, Grade.STRONG_SELL), (
            f"Expected SELL/STRONG_SELL but got {result.grade} (confidence={result.confidence})"
        )

    # ------------------------------------------------------------------ #
    # 4. 중립/혼재 → HOLD
    # ------------------------------------------------------------------ #

    def test_neutral_signals_gives_hold(self, scorer):
        """신호 없음 / 중립 추세 → HOLD."""
        result = scorer.score(
            trend=TrendAnalysis(
                direction=Direction.SIDEWAYS,
                strength=Strength.WEAK,
                ma_alignment="FLAT",
                adx=Decimal("15"),
                hh_ll_structure="RANGING",
            ),
            patterns=[],
            indicator_signals=[],
            volume_analysis=_make_neutral_volume(),
        )
        assert result.grade == Grade.HOLD, (
            f"Expected HOLD but got {result.grade} (confidence={result.confidence})"
        )

    # ------------------------------------------------------------------ #
    # 5. 환경변수 임계값 적용 (monkeypatch)
    # ------------------------------------------------------------------ #

    def test_env_strong_threshold(self, scorer, monkeypatch):
        """CHART_ANALYSIS_CONFIDENCE_STRONG_THRESHOLD 환경변수 적용."""
        monkeypatch.setenv("CHART_ANALYSIS_CONFIDENCE_STRONG_THRESHOLD", "0.9")

        # 기본 임계값(0.7)에서는 STRONG_BUY일 신호들을 임계값(0.9)에서는 BUY
        result = scorer.score(
            trend=_make_trend(Direction.UPTREND, Strength.STRONG, adx="30"),
            patterns=[_bullish_pattern()],
            indicator_signals=[_rsi_signal("25")],
            volume_analysis=_make_spike_volume(bullish=True),
        )
        # confidence가 0.9 미만이면 STRONG_BUY가 아니어야 함 (임계값이 0.9로 올라갔으므로)
        # 또는 confidence >= 0.9이면 STRONG_BUY도 허용
        if result.confidence < Decimal("0.9"):
            assert result.grade != Grade.STRONG_BUY, (
                f"confidence {result.confidence} < 0.9 threshold, should not be STRONG_BUY"
            )

    def test_env_buy_threshold(self, scorer, monkeypatch):
        """CHART_ANALYSIS_CONFIDENCE_BUY_THRESHOLD 환경변수 적용."""
        monkeypatch.setenv("CHART_ANALYSIS_CONFIDENCE_BUY_THRESHOLD", "0.8")

        # 높은 buy 임계값 → 강세 신호도 HOLD 가능
        result = scorer.score(
            trend=_make_trend(Direction.UPTREND, Strength.MEDIUM, adx="22"),
            patterns=[],
            indicator_signals=[],
            volume_analysis=_make_neutral_volume(),
        )
        # confidence < 0.8이면 BUY가 아니어야 함
        if result.confidence < Decimal("0.8"):
            assert result.grade not in (Grade.BUY, Grade.STRONG_BUY)

    # ------------------------------------------------------------------ #
    # 6. confidence는 Decimal
    # ------------------------------------------------------------------ #

    def test_confidence_is_decimal(self, scorer):
        """confidence 필드는 Decimal 타입."""
        result = scorer.score(
            trend=_make_trend(Direction.UPTREND, Strength.STRONG),
            patterns=[],
            indicator_signals=[],
            volume_analysis=_make_neutral_volume(),
        )
        assert isinstance(result.confidence, Decimal)

    # ------------------------------------------------------------------ #
    # 7. grade 필드는 Grade Enum
    # ------------------------------------------------------------------ #

    def test_grade_is_grade_enum(self, scorer):
        """grade 필드는 Grade Enum 타입."""
        result = scorer.score(
            trend=_make_trend(Direction.SIDEWAYS, Strength.WEAK),
            patterns=[],
            indicator_signals=[],
            volume_analysis=_make_neutral_volume(),
        )
        assert isinstance(result.grade, Grade)

    def test_valid_support_resistance_plan_adds_bounded_confidence(self, scorer):
        """올바른 방향의 지지/저항 기반 trade plan은 작은 범위 안에서 confidence를 높인다."""
        baseline = scorer.score(
            trend=TrendAnalysis(
                direction=Direction.SIDEWAYS,
                strength=Strength.WEAK,
                ma_alignment="FLAT",
                adx=Decimal("15"),
                hh_ll_structure="RANGING",
            ),
            patterns=[],
            indicator_signals=[],
            volume_analysis=_make_neutral_volume(),
        )

        adjusted = scorer.score(
            trend=TrendAnalysis(
                direction=Direction.SIDEWAYS,
                strength=Strength.WEAK,
                ma_alignment="FLAT",
                adx=Decimal("15"),
                hh_ll_structure="RANGING",
            ),
            patterns=[],
            indicator_signals=[],
            volume_analysis=_make_neutral_volume(),
            levels=LevelSet(
                supports=[Decimal("95"), Decimal("90")],
                resistances=[Decimal("108"), Decimal("115")],
            ),
            trade_plan=_trade_plan("95", "108"),
            last_close=Decimal("100"),
        )

        delta = adjusted.confidence - baseline.confidence
        assert Decimal("0") < delta <= Decimal("0.0500")
        assert adjusted.grade == Grade.HOLD

    def test_invalid_trade_plan_subtracts_bounded_confidence(self, scorer):
        """잘못된 방향의 stop/target은 bounded penalty로 confidence를 낮춘다."""
        baseline = scorer.score(
            trend=TrendAnalysis(
                direction=Direction.SIDEWAYS,
                strength=Strength.WEAK,
                ma_alignment="FLAT",
                adx=Decimal("15"),
                hh_ll_structure="RANGING",
            ),
            patterns=[],
            indicator_signals=[],
            volume_analysis=_make_neutral_volume(),
        )

        adjusted = scorer.score(
            trend=TrendAnalysis(
                direction=Direction.SIDEWAYS,
                strength=Strength.WEAK,
                ma_alignment="FLAT",
                adx=Decimal("15"),
                hh_ll_structure="RANGING",
            ),
            patterns=[],
            indicator_signals=[],
            volume_analysis=_make_neutral_volume(),
            levels=LevelSet(supports=[Decimal("105")], resistances=[Decimal("95")]),
            trade_plan=_trade_plan("105", "95"),
            last_close=Decimal("100"),
        )

        delta = baseline.confidence - adjusted.confidence
        assert Decimal("0") < delta <= Decimal("0.0500")
        assert adjusted.grade == Grade.HOLD

    def test_support_resistance_only_does_not_change_hold_to_buy_or_sell(self, scorer):
        """SR 품질 보정만으로 중립 방향성 신호가 BUY/SELL로 바뀌지 않는다."""
        neutral_kwargs = {
            "trend": TrendAnalysis(
                direction=Direction.SIDEWAYS,
                strength=Strength.WEAK,
                ma_alignment="FLAT",
                adx=Decimal("15"),
                hh_ll_structure="RANGING",
            ),
            "patterns": [],
            "indicator_signals": [],
            "volume_analysis": _make_neutral_volume(),
        }

        valid_plan = scorer.score(
            **neutral_kwargs,
            levels=LevelSet(
                supports=[Decimal("95")],
                resistances=[Decimal("108")],
            ),
            trade_plan=_trade_plan("95", "108"),
            last_close=Decimal("100"),
        )
        invalid_plan = scorer.score(
            **neutral_kwargs,
            levels=LevelSet(
                supports=[Decimal("105")],
                resistances=[Decimal("95")],
            ),
            trade_plan=_trade_plan("105", "95"),
            last_close=Decimal("100"),
        )

        assert valid_plan.grade == Grade.HOLD
        assert invalid_plan.grade == Grade.HOLD
