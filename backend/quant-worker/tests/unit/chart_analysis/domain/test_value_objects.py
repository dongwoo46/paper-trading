"""TDD — 2-1: Value Objects 단위 테스트 (RED 먼저 작성)."""
from __future__ import annotations

import pytest
from datetime import date
from decimal import Decimal


class TestEnums:
    def test_grade_has_exactly_5_values(self):
        from src.chart_analysis.domain.value_objects import Grade
        assert len(Grade) == 5

    def test_grade_values(self):
        from src.chart_analysis.domain.value_objects import Grade
        names = {g.name for g in Grade}
        assert names == {"STRONG_BUY", "BUY", "HOLD", "SELL", "STRONG_SELL"}

    def test_direction_has_3_values(self):
        from src.chart_analysis.domain.value_objects import Direction
        assert len(Direction) == 3
        names = {d.name for d in Direction}
        assert names == {"UPTREND", "DOWNTREND", "SIDEWAYS"}

    def test_strength_has_3_values(self):
        from src.chart_analysis.domain.value_objects import Strength
        assert len(Strength) == 3
        names = {s.name for s in Strength}
        assert names == {"WEAK", "MEDIUM", "STRONG"}

    def test_pattern_type_has_7_values(self):
        from src.chart_analysis.domain.value_objects import PatternType
        assert len(PatternType) == 7
        names = {p.name for p in PatternType}
        assert names == {
            "DOJI", "HAMMER", "INVERTED_HAMMER",
            "BULLISH_ENGULFING", "BEARISH_ENGULFING",
            "MORNING_STAR", "EVENING_STAR",
        }

    def test_report_source_has_3_values(self):
        from src.chart_analysis.domain.value_objects import ReportSource
        assert len(ReportSource) == 3
        names = {r.name for r in ReportSource}
        assert names == {"LLM_PRIMARY", "RULE_TEMPLATE", "NONE"}


class TestCandle:
    def _make_candle(self, **kwargs):
        from src.chart_analysis.domain.value_objects import Candle
        defaults = dict(
            date=date(2026, 5, 10),
            open=Decimal("68000"),
            high=Decimal("69000"),
            low=Decimal("67500"),
            close=Decimal("68500"),
            volume=Decimal("1000000"),
        )
        defaults.update(kwargs)
        return Candle(**defaults)

    def test_candle_creation_with_decimal(self):
        candle = self._make_candle()
        assert candle.close == Decimal("68500")

    def test_candle_is_immutable(self):
        candle = self._make_candle()
        with pytest.raises((AttributeError, TypeError)):
            candle.close = Decimal("99999")

    def test_candle_rejects_float_open(self):
        from src.chart_analysis.domain.value_objects import Candle
        with pytest.raises((TypeError, ValueError)):
            Candle(
                date=date(2026, 5, 10),
                open=68000.0,  # float — must be rejected
                high=Decimal("69000"),
                low=Decimal("67500"),
                close=Decimal("68500"),
                volume=Decimal("1000000"),
            )

    def test_candle_rejects_float_close(self):
        from src.chart_analysis.domain.value_objects import Candle
        with pytest.raises((TypeError, ValueError)):
            Candle(
                date=date(2026, 5, 10),
                open=Decimal("68000"),
                high=Decimal("69000"),
                low=Decimal("67500"),
                close=68500.0,  # float — must be rejected
                volume=Decimal("1000000"),
            )

    def test_candle_rejects_int_volume(self):
        """volume은 int가 아닌 Decimal이어야 한다."""
        from src.chart_analysis.domain.value_objects import Candle
        with pytest.raises((TypeError, ValueError)):
            Candle(
                date=date(2026, 5, 10),
                open=Decimal("68000"),
                high=Decimal("69000"),
                low=Decimal("67500"),
                close=Decimal("68500"),
                volume=1000000,  # int — must be rejected
            )


class TestTradePlan:
    def test_trade_plan_all_decimal(self):
        from src.chart_analysis.domain.value_objects import TradePlan
        plan = TradePlan(
            entry_price=Decimal("69000"),
            stop_loss=Decimal("67000"),
            target_price=Decimal("72000"),
            risk_reward_ratio=Decimal("1.5"),
        )
        assert plan.entry_price == Decimal("69000")
        assert plan.risk_reward_ratio == Decimal("1.5")

    def test_trade_plan_rejects_float_entry(self):
        from src.chart_analysis.domain.value_objects import TradePlan
        with pytest.raises((TypeError, ValueError)):
            TradePlan(
                entry_price=69000.0,
                stop_loss=Decimal("67000"),
                target_price=Decimal("72000"),
                risk_reward_ratio=Decimal("1.5"),
            )

    def test_trade_plan_rejects_float_stop_loss(self):
        from src.chart_analysis.domain.value_objects import TradePlan
        with pytest.raises((TypeError, ValueError)):
            TradePlan(
                entry_price=Decimal("69000"),
                stop_loss=67000.0,
                target_price=Decimal("72000"),
                risk_reward_ratio=Decimal("1.5"),
            )


class TestRecommendation:
    def test_recommendation_decimal_confidence(self):
        from src.chart_analysis.domain.value_objects import Recommendation, Grade
        rec = Recommendation(grade=Grade.BUY, confidence=Decimal("0.612"))
        assert rec.confidence == Decimal("0.612")
        assert rec.grade == Grade.BUY

    def test_recommendation_rejects_float_confidence(self):
        from src.chart_analysis.domain.value_objects import Recommendation, Grade
        with pytest.raises((TypeError, ValueError)):
            Recommendation(grade=Grade.BUY, confidence=0.612)

    def test_recommendation_confidence_must_be_in_range(self):
        from src.chart_analysis.domain.value_objects import Recommendation, Grade
        with pytest.raises((TypeError, ValueError)):
            Recommendation(grade=Grade.BUY, confidence=Decimal("1.5"))

    def test_recommendation_confidence_negative_rejected(self):
        from src.chart_analysis.domain.value_objects import Recommendation, Grade
        with pytest.raises((TypeError, ValueError)):
            Recommendation(grade=Grade.BUY, confidence=Decimal("-0.1"))


class TestNarrativeReport:
    def _make_report(self, **kwargs):
        from src.chart_analysis.domain.value_objects import NarrativeReport, ReportSource
        defaults = dict(
            trend_section="추세 분석 내용",
            support_resistance_section="지지저항 내용",
            entry_plan_section="진입 계획 내용",
            signal_evidence_section="신호 근거 내용",
            risk_section="리스크 내용",
            source=ReportSource.LLM_PRIMARY,
        )
        defaults.update(kwargs)
        return NarrativeReport(**defaults)

    def test_narrative_report_creation(self):
        report = self._make_report()
        assert report.trend_section == "추세 분석 내용"

    def test_narrative_report_rejects_empty_trend_section(self):
        with pytest.raises((TypeError, ValueError)):
            self._make_report(trend_section="")

    def test_narrative_report_rejects_empty_support_resistance_section(self):
        with pytest.raises((TypeError, ValueError)):
            self._make_report(support_resistance_section="")

    def test_narrative_report_rejects_empty_entry_plan_section(self):
        with pytest.raises((TypeError, ValueError)):
            self._make_report(entry_plan_section="")

    def test_narrative_report_rejects_empty_signal_evidence_section(self):
        with pytest.raises((TypeError, ValueError)):
            self._make_report(signal_evidence_section="")

    def test_narrative_report_rejects_empty_risk_section(self):
        with pytest.raises((TypeError, ValueError)):
            self._make_report(risk_section="")

    def test_narrative_report_is_immutable(self):
        report = self._make_report()
        with pytest.raises((AttributeError, TypeError)):
            report.trend_section = "바뀐 내용"


class TestTrendAnalysis:
    def test_trend_analysis_creation(self):
        from src.chart_analysis.domain.value_objects import TrendAnalysis, Direction, Strength
        trend = TrendAnalysis(
            direction=Direction.UPTREND,
            strength=Strength.MEDIUM,
            ma_alignment="MA20>MA60",
            adx=Decimal("23.4"),
            hh_ll_structure="HH",
        )
        assert trend.direction == Direction.UPTREND
        assert trend.adx == Decimal("23.4")

    def test_trend_analysis_rejects_float_adx(self):
        from src.chart_analysis.domain.value_objects import TrendAnalysis, Direction, Strength
        with pytest.raises((TypeError, ValueError)):
            TrendAnalysis(
                direction=Direction.UPTREND,
                strength=Strength.MEDIUM,
                ma_alignment="MA20>MA60",
                adx=23.4,  # float
                hh_ll_structure="HH",
            )


class TestIndicatorSet:
    def test_indicator_set_decimal_fields(self):
        from src.chart_analysis.domain.value_objects import IndicatorSet
        ind = IndicatorSet(
            ma20=Decimal("68000"),
            ma60=Decimal("67000"),
            ma120=Decimal("65000"),
            rsi14=Decimal("58.2"),
            macd=Decimal("120.4"),
            macd_signal=Decimal("100.1"),
            macd_hist=Decimal("20.3"),
            bb_upper=Decimal("71000"),
            bb_lower=Decimal("65000"),
            atr14=Decimal("800"),
            adx14=Decimal("23.4"),
            stoch_k=Decimal("65.0"),
            stoch_d=Decimal("60.0"),
            obv=Decimal("5000000"),
            volume_ma20=Decimal("900000"),
        )
        assert ind.rsi14 == Decimal("58.2")

    def test_indicator_set_rejects_float(self):
        from src.chart_analysis.domain.value_objects import IndicatorSet
        with pytest.raises((TypeError, ValueError)):
            IndicatorSet(
                ma20=68000.0,  # float
                ma60=Decimal("67000"),
                ma120=Decimal("65000"),
                rsi14=Decimal("58.2"),
                macd=Decimal("120.4"),
                macd_signal=Decimal("100.1"),
                macd_hist=Decimal("20.3"),
                bb_upper=Decimal("71000"),
                bb_lower=Decimal("65000"),
                atr14=Decimal("800"),
                adx14=Decimal("23.4"),
                stoch_k=Decimal("65.0"),
                stoch_d=Decimal("60.0"),
                obv=Decimal("5000000"),
                volume_ma20=Decimal("900000"),
            )


class TestLevelSet:
    def test_level_set_decimal_lists(self):
        from src.chart_analysis.domain.value_objects import LevelSet
        levels = LevelSet(
            supports=[Decimal("68500"), Decimal("67200")],
            resistances=[Decimal("71200"), Decimal("72500")],
        )
        assert levels.supports[0] == Decimal("68500")

    def test_level_set_rejects_float_in_supports(self):
        from src.chart_analysis.domain.value_objects import LevelSet
        with pytest.raises((TypeError, ValueError)):
            LevelSet(
                supports=[68500.0],
                resistances=[Decimal("71200")],
            )


class TestVolumeAnalysis:
    def test_volume_analysis_decimal_avg_ratio(self):
        from src.chart_analysis.domain.value_objects import VolumeAnalysis
        vol = VolumeAnalysis(
            trend="increasing",
            spike_detected=True,
            avg_ratio=Decimal("1.42"),
        )
        assert vol.avg_ratio == Decimal("1.42")

    def test_volume_analysis_rejects_float_avg_ratio(self):
        from src.chart_analysis.domain.value_objects import VolumeAnalysis
        with pytest.raises((TypeError, ValueError)):
            VolumeAnalysis(trend="increasing", spike_detected=True, avg_ratio=1.42)


class TestIndicatorSignal:
    def test_indicator_signal_decimal_value(self):
        from src.chart_analysis.domain.value_objects import IndicatorSignal
        sig = IndicatorSignal(name="RSI", value=Decimal("58.2"), interpretation="neutral_bullish")
        assert sig.value == Decimal("58.2")

    def test_indicator_signal_rejects_float_value(self):
        from src.chart_analysis.domain.value_objects import IndicatorSignal
        with pytest.raises((TypeError, ValueError)):
            IndicatorSignal(name="RSI", value=58.2, interpretation="neutral_bullish")


class TestCandlePattern:
    def test_candle_pattern_creation(self):
        from src.chart_analysis.domain.value_objects import CandlePattern, PatternType
        pattern = CandlePattern(
            type=PatternType.HAMMER,
            index=18,
            date=date(2026, 5, 9),
        )
        assert pattern.type == PatternType.HAMMER
        assert pattern.index == 18
