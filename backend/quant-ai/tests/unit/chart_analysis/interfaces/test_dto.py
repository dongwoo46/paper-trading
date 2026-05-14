"""Substep 5-1: DTO 단위 테스트 — ChartAnalysisResult → AnalysisWindowDTO 변환."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

from datetime import datetime, date
from decimal import Decimal

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


def _make_result(
    window: str = "1M",
    interval: str = "D",
    report: "NarrativeReport | None" = None,
    report_source: ReportSource = ReportSource.NONE,
) -> ChartAnalysisResult:
    return ChartAnalysisResult(
        symbol="005930",
        window=window,
        interval=interval,
        snapshot_hash="abc123",
        trend=TrendAnalysis(
            direction=Direction.UPTREND,
            strength=Strength.MEDIUM,
            ma_alignment="MA20>MA60",
            adx=Decimal("23.4"),
            hh_ll_structure="HH",
        ),
        levels=LevelSet(
            supports=[Decimal("68500"), Decimal("67200")],
            resistances=[Decimal("71200"), Decimal("72500")],
        ),
        trade_plan=TradePlan(
            entry_price=Decimal("69400"),
            stop_loss=Decimal("67100"),
            target_price=Decimal("72500"),
            risk_reward_ratio=Decimal("1.35"),
        ),
        patterns=[
            CandlePattern(type=PatternType.HAMMER, index=18, date=date(2026, 5, 9))
        ],
        indicator_signals=[
            IndicatorSignal(name="RSI", value=Decimal("58.2"), interpretation="neutral_bullish"),
            IndicatorSignal(name="MACD", value=Decimal("120.4"), interpretation="golden_cross_3d"),
        ],
        volume_analysis=VolumeAnalysis(
            trend="increasing",
            spike_detected=True,
            avg_ratio=Decimal("1.42"),
        ),
        recommendation=Recommendation(grade=Grade.BUY, confidence=Decimal("0.612")),
        report=report,
        report_source=report_source,
        numeric_computed_at=datetime(2026, 5, 10, 8, 0, 0),
        llm_computed_at=None,
    )


class TestAnalysisWindowDtoFromDomain:
    """AnalysisWindowDTO.from_domain() 변환 검증."""

    def test_from_domain_basic_fields(self):
        from chart_analysis.interfaces.dto import AnalysisWindowDTO
        result = _make_result()
        dto = AnalysisWindowDTO.from_domain(result)

        assert dto.window == "1M"
        assert dto.interval == "D"
        assert dto.snapshot_hash == "abc123"

    def test_from_domain_decimal_to_str(self):
        """Decimal 값이 모두 str로 직렬화된다 — float 금지."""
        from chart_analysis.interfaces.dto import AnalysisWindowDTO
        result = _make_result()
        dto = AnalysisWindowDTO.from_domain(result)

        # summary
        assert isinstance(dto.summary.confidence, str)
        assert dto.summary.confidence == "0.612"
        assert dto.summary.recommendation == "BUY"

        # levels
        assert isinstance(dto.levels.entry, str)
        assert dto.levels.entry == "69400"
        assert isinstance(dto.levels.stop_loss, str)
        assert dto.levels.stop_loss == "67100"
        assert dto.levels.supports == ["68500", "67200"]
        assert dto.levels.resistances == ["71200", "72500"]

        # trend
        assert isinstance(dto.technical.trend.adx, str)
        assert dto.technical.trend.adx == "23.4"

        # indicator signals
        assert dto.technical.indicator_signals[0].value == "58.2"

        # volume
        assert dto.volume.avg_ratio == "1.42"

    def test_from_domain_report_status_none(self):
        """리포트 없음 → status='none'."""
        from chart_analysis.interfaces.dto import AnalysisWindowDTO
        result = _make_result(report=None)
        dto = AnalysisWindowDTO.from_domain(result)
        assert dto.report.status == "none"

    def test_from_domain_report_status_available(self):
        """LLM 리포트 있음 → status='available'."""
        from chart_analysis.interfaces.dto import AnalysisWindowDTO
        report = NarrativeReport(
            trend_section="추세 분석 텍스트",
            support_resistance_section="지지 저항 텍스트",
            entry_plan_section="진입 계획 텍스트",
            signal_evidence_section="신호 근거 텍스트",
            risk_section="리스크 텍스트",
            source=ReportSource.LLM_PRIMARY,
        )
        result = _make_result(report=report, report_source=ReportSource.LLM_PRIMARY)
        dto = AnalysisWindowDTO.from_domain(result)
        assert dto.report.status == "available"

    def test_dto_serialization_determinism(self):
        """동일 입력 → 동일 직렬화 결과 (결정성 검증)."""
        from chart_analysis.interfaces.dto import AnalysisWindowDTO
        result = _make_result()
        dto1 = AnalysisWindowDTO.from_domain(result)
        dto2 = AnalysisWindowDTO.from_domain(result)

        assert dto1.model_dump() == dto2.model_dump()

    def test_dto_json_has_no_float(self):
        """DTO JSON 직렬화에 float 타입 없음."""
        from chart_analysis.interfaces.dto import AnalysisWindowDTO
        import json
        result = _make_result()
        dto = AnalysisWindowDTO.from_domain(result)
        raw = dto.model_dump()
        raw_json = json.dumps(raw)
        parsed = json.loads(raw_json)
        _assert_no_float(parsed)


def _assert_no_float(obj, path=""):
    if isinstance(obj, float):
        raise AssertionError(f"float found at {path}: {obj}")
    if isinstance(obj, dict):
        for k, v in obj.items():
            _assert_no_float(v, f"{path}.{k}")
    if isinstance(obj, list):
        for i, v in enumerate(obj):
            _assert_no_float(v, f"{path}[{i}]")


class TestChartAnalysisResponseDTO:
    """ChartAnalysisResponseDTO 직렬화 검증."""

    def test_response_dto_contains_symbol_and_analyses(self):
        from chart_analysis.interfaces.dto import AnalysisWindowDTO, ChartAnalysisResponseDTO
        result = _make_result()
        dto = ChartAnalysisResponseDTO(
            symbol="005930",
            analyses=[AnalysisWindowDTO.from_domain(result)],
        )
        data = dto.model_dump()
        assert data["symbol"] == "005930"
        assert len(data["analyses"]) == 1

    def test_empty_analyses(self):
        from chart_analysis.interfaces.dto import ChartAnalysisResponseDTO
        dto = ChartAnalysisResponseDTO(symbol="000000", analyses=[])
        assert dto.analyses == []
