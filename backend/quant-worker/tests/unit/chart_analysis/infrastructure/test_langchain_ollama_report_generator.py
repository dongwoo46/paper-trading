"""단위 테스트: LangChainOllamaReportGenerator."""
from __future__ import annotations

import os
import sys
from unittest.mock import MagicMock, patch, PropertyMock

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "..", "src"))

from datetime import datetime, timezone, date
from decimal import Decimal

import pytest

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
from chart_analysis.domain.chart_snapshot import ChartSnapshot
from chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
from chart_analysis.infrastructure.rule_template_report_generator import (
    RuleTemplateReportGenerator,
)


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

def _make_snapshot() -> ChartSnapshot:
    candles = [
        Candle(
            date=date(2026, 1, i + 1),
            open=Decimal("100"),
            high=Decimal("105"),
            low=Decimal("99"),
            close=Decimal("103"),
            volume=Decimal("1000000"),
        )
        for i in range(5)
    ]
    indicators = IndicatorSet(
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
    return ChartSnapshot(
        symbol="005930",
        window="1M",
        interval="D",
        candles=candles,
        indicators=indicators,
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
        supports=[Decimal("68500")],
        resistances=[Decimal("71200")],
    )
    trade_plan = TradePlan(
        entry_price=Decimal("69400"),
        stop_loss=Decimal("67100"),
        target_price=Decimal("72500"),
        risk_reward_ratio=Decimal("1.35"),
    )
    indicator_signals = [
        IndicatorSignal(name="RSI", value=Decimal("58.2"), interpretation="neutral_bullish"),
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
# LLM 성공 응답 mock 데이터
# ---------------------------------------------------------------------------

_VALID_LLM_RESPONSE_DICT = {
    "trend_section": "삼성전자는 현재 상승 추세로 판단된다. MA20이 MA60 위에 위치하며 ADX가 23.4로 중간 강도 추세로 분석된다.",
    "support_resistance_section": "주요 지지선은 68,500원으로 분석된다. 이 가격대에서 매수세 유입 가능성이 있다.",
    "entry_plan_section": "69,400원 수준에서 진입을 고려할 수 있다. 손절가는 67,100원, 목표가는 72,500원으로 설정된다.",
    "signal_evidence_section": "RSI 58.2는 중립 매수 우위 구간으로 분석된다. 거래량 증가가 상승 신호를 지지하는 것으로 보인다.",
    "risk_section": "추세 전환 시 손절가 이탈 위험이 있다. 개인의 투자 판단이 중요하다고 판단된다.",
}


# ---------------------------------------------------------------------------
# Tests
# ---------------------------------------------------------------------------

class TestLangChainOllamaReportGeneratorNormal:
    """정상 응답 → NarrativeReport(source=LLM_PRIMARY) 반환."""

    def test_returns_narrative_report_on_success(self):
        """LLM 성공 응답 시 NarrativeReport 반환."""
        # langchain_ollama 모듈 자체를 mock
        mock_llm_module = MagicMock()
        mock_ollama = MagicMock()
        mock_llm_module.ChatOllama = MagicMock(return_value=mock_ollama)

        mock_langchain_core = MagicMock()
        mock_output_parsers = MagicMock()
        mock_parser = MagicMock()
        mock_parser.parse.return_value = MagicMock(**_VALID_LLM_RESPONSE_DICT)
        mock_output_parsers.PydanticOutputParser = MagicMock(return_value=mock_parser)
        mock_langchain_core.output_parsers = mock_output_parsers
        mock_langchain_core.prompts = MagicMock()
        mock_langchain_core.prompts.ChatPromptTemplate = MagicMock()

        with patch.dict("sys.modules", {
            "langchain_ollama": mock_llm_module,
            "langchain_core": mock_langchain_core,
            "langchain_core.output_parsers": mock_output_parsers,
            "langchain_core.prompts": mock_langchain_core.prompts,
        }):
            # langchain_ollama가 mock된 상태에서 import
            from chart_analysis.infrastructure.langchain_ollama_report_generator import (
                LangChainOllamaReportGenerator,
            )
            fallback = RuleTemplateReportGenerator()

            # chain invoke mock
            mock_chain = MagicMock()
            mock_chain.invoke.return_value = MagicMock(**_VALID_LLM_RESPONSE_DICT)

            gen = LangChainOllamaReportGenerator.__new__(LangChainOllamaReportGenerator)
            gen._fallback = fallback
            gen._chain = mock_chain
            gen._parser = mock_parser

            snapshot = _make_snapshot()
            result = _make_result()
            report = gen.generate(snapshot, result)

        assert isinstance(report, NarrativeReport)
        assert report.source == ReportSource.LLM_PRIMARY

    def test_all_sections_filled_on_success(self):
        """성공 응답 시 5섹션 모두 비어있지 않음."""
        mock_response = MagicMock(**_VALID_LLM_RESPONSE_DICT)

        mock_chain = MagicMock()
        mock_chain.invoke.return_value = mock_response
        mock_parser = MagicMock()
        mock_parser.parse.return_value = mock_response

        with patch.dict("sys.modules", {
            "langchain_ollama": MagicMock(),
            "langchain_core": MagicMock(),
            "langchain_core.output_parsers": MagicMock(),
            "langchain_core.prompts": MagicMock(),
        }):
            from chart_analysis.infrastructure.langchain_ollama_report_generator import (
                LangChainOllamaReportGenerator,
            )
            fallback = RuleTemplateReportGenerator()

            gen = LangChainOllamaReportGenerator.__new__(LangChainOllamaReportGenerator)
            gen._fallback = fallback
            gen._chain = mock_chain
            gen._parser = mock_parser

            snapshot = _make_snapshot()
            result = _make_result()
            report = gen.generate(snapshot, result)

        assert report.trend_section.strip()
        assert report.support_resistance_section.strip()
        assert report.entry_plan_section.strip()
        assert report.signal_evidence_section.strip()
        assert report.risk_section.strip()


class TestLangChainOllamaReportGeneratorFallback:
    """스키마 위반/타임아웃 → 룰 템플릿 폴백 호출."""

    def _make_gen_with_failing_chain(self, side_effect):
        """체인 호출이 예외를 던지는 generator 생성."""
        mock_chain = MagicMock()
        mock_chain.invoke.side_effect = side_effect

        with patch.dict("sys.modules", {
            "langchain_ollama": MagicMock(),
            "langchain_core": MagicMock(),
            "langchain_core.output_parsers": MagicMock(),
            "langchain_core.prompts": MagicMock(),
        }):
            from chart_analysis.infrastructure.langchain_ollama_report_generator import (
                LangChainOllamaReportGenerator,
            )
            fallback = RuleTemplateReportGenerator()

            gen = LangChainOllamaReportGenerator.__new__(LangChainOllamaReportGenerator)
            gen._fallback = fallback
            gen._chain = mock_chain
            gen._parser = MagicMock()
            return gen

    def test_schema_violation_falls_back_to_rule_template(self):
        """스키마 위반 시 룰 템플릿 폴백 호출 → source=RULE_TEMPLATE."""
        gen = self._make_gen_with_failing_chain(ValueError("schema violation"))

        snapshot = _make_snapshot()
        result = _make_result()
        report = gen.generate(snapshot, result)

        assert isinstance(report, NarrativeReport)
        assert report.source == ReportSource.RULE_TEMPLATE

    def test_timeout_falls_back_to_rule_template(self):
        """타임아웃 시 룰 템플릿 폴백 호출."""
        import concurrent.futures
        gen = self._make_gen_with_failing_chain(
            TimeoutError("LLM timeout")
        )

        snapshot = _make_snapshot()
        result = _make_result()
        report = gen.generate(snapshot, result)

        assert report.source == ReportSource.RULE_TEMPLATE

    def test_general_exception_falls_back(self):
        """일반 예외 시 폴백 호출."""
        gen = self._make_gen_with_failing_chain(Exception("LLM error"))

        snapshot = _make_snapshot()
        result = _make_result()
        report = gen.generate(snapshot, result)

        assert report.source == ReportSource.RULE_TEMPLATE

    def test_fallback_report_sections_non_empty(self):
        """폴백 리포트의 5섹션이 모두 비어있지 않음."""
        gen = self._make_gen_with_failing_chain(Exception("error"))

        snapshot = _make_snapshot()
        result = _make_result()
        report = gen.generate(snapshot, result)

        assert report.trend_section.strip()
        assert report.support_resistance_section.strip()
        assert report.entry_plan_section.strip()
        assert report.signal_evidence_section.strip()
        assert report.risk_section.strip()
