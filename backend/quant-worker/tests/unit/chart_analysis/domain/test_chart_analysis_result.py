"""TDD — 2-3: ChartAnalysisResult Aggregate 단위 테스트 (RED 먼저 작성)."""
from __future__ import annotations

import pytest
from datetime import datetime, timezone
from decimal import Decimal


def _make_trend():
    from src.chart_analysis.domain.value_objects import TrendAnalysis, Direction, Strength
    return TrendAnalysis(
        direction=Direction.UPTREND,
        strength=Strength.MEDIUM,
        ma_alignment="MA20>MA60",
        adx=Decimal("23.4"),
        hh_ll_structure="HH",
    )


def _make_levels():
    from src.chart_analysis.domain.value_objects import LevelSet
    return LevelSet(
        supports=[Decimal("68500"), Decimal("67200")],
        resistances=[Decimal("71200"), Decimal("72500")],
    )


def _make_trade_plan():
    from src.chart_analysis.domain.value_objects import TradePlan
    return TradePlan(
        entry_price=Decimal("69000"),
        stop_loss=Decimal("67000"),
        target_price=Decimal("72000"),
        risk_reward_ratio=Decimal("1.5"),
    )


def _make_volume_analysis():
    from src.chart_analysis.domain.value_objects import VolumeAnalysis
    return VolumeAnalysis(
        trend="increasing",
        spike_detected=True,
        avg_ratio=Decimal("1.42"),
    )


def _make_recommendation():
    from src.chart_analysis.domain.value_objects import Recommendation, Grade
    return Recommendation(grade=Grade.BUY, confidence=Decimal("0.612"))


def _make_narrative_report():
    from src.chart_analysis.domain.value_objects import NarrativeReport, ReportSource
    return NarrativeReport(
        trend_section="추세 분석 내용",
        support_resistance_section="지지저항 내용",
        entry_plan_section="진입 계획 내용",
        signal_evidence_section="신호 근거 내용",
        risk_section="리스크 내용",
        source=ReportSource.LLM_PRIMARY,
    )


def _make_result(**kwargs):
    from src.chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
    from src.chart_analysis.domain.value_objects import ReportSource
    defaults = dict(
        symbol="005930",
        window="3M",
        interval="D",
        snapshot_hash="a" * 64,
        trend=_make_trend(),
        levels=_make_levels(),
        trade_plan=_make_trade_plan(),
        patterns=[],
        indicator_signals=[],
        volume_analysis=_make_volume_analysis(),
        recommendation=_make_recommendation(),
        report=None,
        report_source=ReportSource.NONE,
        numeric_computed_at=datetime(2026, 5, 10, 8, 0, 0, tzinfo=timezone.utc),
        llm_computed_at=None,
    )
    defaults.update(kwargs)
    return ChartAnalysisResult(**defaults)


class TestChartAnalysisResultDefaults:
    def test_default_report_source_is_none(self):
        from src.chart_analysis.domain.value_objects import ReportSource
        result = _make_result()
        assert result.report_source == ReportSource.NONE

    def test_report_defaults_to_none(self):
        result = _make_result()
        assert result.report is None

    def test_llm_computed_at_defaults_to_none(self):
        result = _make_result()
        assert result.llm_computed_at is None


class TestChartAnalysisResultWithReport:
    def test_with_report_returns_new_instance(self):
        """with_report()는 기존 인스턴스를 변경하지 않고 새 인스턴스를 반환한다."""
        result = _make_result()
        report = _make_narrative_report()
        from src.chart_analysis.domain.value_objects import ReportSource
        new_result = result.with_report(report, ReportSource.LLM_PRIMARY)
        assert new_result is not result

    def test_with_report_original_unchanged(self):
        """원본 인스턴스 report는 None 그대로."""
        result = _make_result()
        report = _make_narrative_report()
        from src.chart_analysis.domain.value_objects import ReportSource
        result.with_report(report, ReportSource.LLM_PRIMARY)
        assert result.report is None

    def test_with_report_sets_report(self):
        result = _make_result()
        report = _make_narrative_report()
        from src.chart_analysis.domain.value_objects import ReportSource
        new_result = result.with_report(report, ReportSource.LLM_PRIMARY)
        assert new_result.report is report

    def test_with_report_sets_source(self):
        result = _make_result()
        report = _make_narrative_report()
        from src.chart_analysis.domain.value_objects import ReportSource
        new_result = result.with_report(report, ReportSource.RULE_TEMPLATE)
        assert new_result.report_source == ReportSource.RULE_TEMPLATE

    def test_with_report_preserves_other_fields(self):
        result = _make_result()
        report = _make_narrative_report()
        from src.chart_analysis.domain.value_objects import ReportSource
        new_result = result.with_report(report, ReportSource.LLM_PRIMARY)
        assert new_result.symbol == result.symbol
        assert new_result.window == result.window
        assert new_result.snapshot_hash == result.snapshot_hash


class TestChartAnalysisResultToResponseDict:
    def test_to_response_dict_returns_dict(self):
        result = _make_result()
        d = result.to_response_dict()
        assert isinstance(d, dict)

    def test_to_response_dict_no_decimal_values(self):
        """to_response_dict()는 Decimal 값을 포함하지 않는다 (모두 str로 변환)."""
        result = _make_result()
        d = result.to_response_dict()
        _assert_no_decimal(d)

    def test_to_response_dict_contains_symbol(self):
        result = _make_result()
        d = result.to_response_dict()
        assert d["symbol"] == "005930"

    def test_to_response_dict_contains_window(self):
        result = _make_result()
        d = result.to_response_dict()
        assert d["window"] == "3M"

    def test_to_response_dict_recommendation_is_string(self):
        result = _make_result()
        d = result.to_response_dict()
        # confidence는 str이어야 한다
        assert isinstance(d["summary"]["confidence"], str)

    def test_to_response_dict_report_is_none_when_no_report(self):
        result = _make_result()
        d = result.to_response_dict()
        assert d["report"]["narrative"] is None

    def test_to_response_dict_with_report(self):
        result = _make_result()
        report = _make_narrative_report()
        from src.chart_analysis.domain.value_objects import ReportSource
        result_with_report = result.with_report(report, ReportSource.LLM_PRIMARY)
        d = result_with_report.to_response_dict()
        assert d["report"]["narrative"] is not None


class TestChartAnalysisResultImmutability:
    def test_result_is_immutable(self):
        result = _make_result()
        with pytest.raises((AttributeError, TypeError)):
            result.symbol = "NEW"


# ---------------------------------------------------------------------------
# Helper
# ---------------------------------------------------------------------------

def _assert_no_decimal(obj, path=""):
    from decimal import Decimal
    if isinstance(obj, Decimal):
        raise AssertionError(f"Found Decimal at path '{path}': {obj!r}")
    elif isinstance(obj, dict):
        for k, v in obj.items():
            _assert_no_decimal(v, path=f"{path}.{k}")
    elif isinstance(obj, (list, tuple)):
        for i, v in enumerate(obj):
            _assert_no_decimal(v, path=f"{path}[{i}]")