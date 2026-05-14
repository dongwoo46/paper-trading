"""단위 테스트: RuleTemplateReportGenerator."""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "..", "src"))

from datetime import datetime, timezone
from decimal import Decimal

import pytest

from chart_analysis.infrastructure.rule_template_report_generator import (
    RuleTemplateReportGenerator,
)
from chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
from chart_analysis.domain.chart_snapshot import ChartSnapshot
from chart_analysis.domain.value_objects import (
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
from datetime import date


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

def _make_candles(n: int = 5) -> list[Candle]:
    return [
        Candle(
            date=date(2026, 1, i + 1),
            open=Decimal("100"),
            high=Decimal("105"),
            low=Decimal("99"),
            close=Decimal("103"),
            volume=Decimal("1000000"),
        )
        for i in range(n)
    ]


def _make_indicators() -> IndicatorSet:
    return IndicatorSet(
        ma20=Decimal("69000"),
        ma60=Decimal("67500"),
        ma120=Decimal("65000"),
        rsi14=Decimal("58.2"),
        macd=Decimal("120.4"),
        macd_signal=Decimal("90.1"),
        macd_hist=Decimal("30.3"),
        bb_upper=Decimal("72000"),
        bb_lower=Decimal("66000"),
        atr14=Decimal("1500"),
        adx14=Decimal("23.4"),
        stoch_k=Decimal("65.0"),
        stoch_d=Decimal("60.0"),
        obv=Decimal("5000000"),
        volume_ma20=Decimal("1200000"),
    )


def _make_snapshot() -> ChartSnapshot:
    return ChartSnapshot(
        symbol="005930",
        window="1M",
        interval="D",
        candles=_make_candles(),
        indicators=_make_indicators(),
    )


def _make_result() -> ChartAnalysisResult:
    trend = TrendAnalysis(
        direction=Direction.UPTREND,
        strength=Strength.MEDIUM,
        ma_alignment="MA20>MA60",
        adx=Decimal("23.4"),
        hh_ll_structure="HH",
    )
    levels = LevelSet(
        supports=[Decimal("68500"), Decimal("67200")],
        resistances=[Decimal("71200"), Decimal("72500")],
    )
    trade_plan = TradePlan(
        entry_price=Decimal("69400"),
        stop_loss=Decimal("67100"),
        target_price=Decimal("72500"),
        risk_reward_ratio=Decimal("1.35"),
    )
    indicator_signals = [
        IndicatorSignal(name="RSI", value=Decimal("58.2"), interpretation="neutral_bullish"),
        IndicatorSignal(name="MACD", value=Decimal("120.4"), interpretation="golden_cross_3d"),
    ]
    volume_analysis = VolumeAnalysis(
        trend="increasing",
        spike_detected=True,
        avg_ratio=Decimal("1.42"),
    )
    recommendation = Recommendation(grade=Grade.BUY, confidence=Decimal("0.612"))

    return ChartAnalysisResult(
        symbol="005930",
        window="1M",
        interval="D",
        snapshot_hash="abc123",
        trend=trend,
        levels=levels,
        trade_plan=trade_plan,
        patterns=[],
        indicator_signals=indicator_signals,
        volume_analysis=volume_analysis,
        recommendation=recommendation,
        report=None,
        report_source=ReportSource.NONE,
        numeric_computed_at=datetime(2026, 5, 11, 8, 0, 0, tzinfo=timezone.utc),
        llm_computed_at=None,
    )


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

class TestRuleTemplateReportGenerator:
    """RuleTemplateReportGenerator 단위 테스트."""

    def setup_method(self):
        self.gen = RuleTemplateReportGenerator()
        self.snapshot = _make_snapshot()
        self.result = _make_result()

    def test_returns_narrative_report(self):
        report = self.gen.generate(self.snapshot, self.result)
        assert isinstance(report, NarrativeReport)

    def test_source_is_rule_template(self):
        report = self.gen.generate(self.snapshot, self.result)
        assert report.source == ReportSource.RULE_TEMPLATE

    def test_all_five_sections_non_empty(self):
        report = self.gen.generate(self.snapshot, self.result)
        assert report.trend_section.strip(), "trend_section이 비어있습니다."
        assert report.support_resistance_section.strip(), "support_resistance_section이 비어있습니다."
        assert report.entry_plan_section.strip(), "entry_plan_section이 비어있습니다."
        assert report.signal_evidence_section.strip(), "signal_evidence_section이 비어있습니다."
        assert report.risk_section.strip(), "risk_section이 비어있습니다."

    def test_deterministic_same_input_same_output(self):
        report1 = self.gen.generate(self.snapshot, self.result)
        report2 = self.gen.generate(self.snapshot, self.result)
        assert report1.trend_section == report2.trend_section
        assert report1.support_resistance_section == report2.support_resistance_section
        assert report1.entry_plan_section == report2.entry_plan_section
        assert report1.signal_evidence_section == report2.signal_evidence_section
        assert report1.risk_section == report2.risk_section

    def test_assertive_tone_uses_handa_style(self):
        """~한다 어조 확인 — 각 섹션에 한다/된다/있다 포함."""
        report = self.gen.generate(self.snapshot, self.result)
        all_text = " ".join([
            report.trend_section,
            report.support_resistance_section,
            report.entry_plan_section,
            report.signal_evidence_section,
            report.risk_section,
        ])
        tone_markers = ["한다", "된다", "있다", "보인다", "판단된다"]
        assert any(marker in all_text for marker in tone_markers), \
            f"~한다 어조 어미가 없습니다. 텍스트: {all_text[:200]}"

    def test_no_absolute_certainty_words(self):
        """'확실히', '100%' 어휘 없음 검증."""
        report = self.gen.generate(self.snapshot, self.result)
        all_text = " ".join([
            report.trend_section,
            report.support_resistance_section,
            report.entry_plan_section,
            report.signal_evidence_section,
            report.risk_section,
        ])
        forbidden = ["확실히", "100%", "반드시"]
        for word in forbidden:
            assert word not in all_text, f"금지 어휘 발견: '{word}'"

    def test_numeric_values_in_sections(self):
        """가격/수치 값이 섹션에 포함되는지 확인."""
        report = self.gen.generate(self.snapshot, self.result)
        # 지지/저항 레벨이 섹션에 반영되어야 함
        assert "69400" in report.entry_plan_section or "69,400" in report.entry_plan_section, \
            "entry_plan_section에 진입가(69400)가 없습니다."

    def test_confidence_value_in_output(self):
        report = self.gen.generate(self.snapshot, self.result)
        all_text = " ".join([
            report.trend_section,
            report.support_resistance_section,
            report.entry_plan_section,
            report.signal_evidence_section,
            report.risk_section,
        ])
        # 신뢰도 값이 언급되어야 함
        assert "0.612" in all_text or "61.2" in all_text or "BUY" in all_text, \
            "recommendation/confidence 값이 섹션에 없습니다."
