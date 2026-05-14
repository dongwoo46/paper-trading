"""LangChainOllamaReportGenerator — LlmReportGenerator 메인 어댑터.

LangChain Ollama를 사용하여 한국어 자연어 리포트를 생성한다.
스키마 위반 시 1회 재시도 후 실패하면 RuleTemplateReportGenerator 폴백.
타임아웃 시 즉시 폴백.

환경변수:
    CHART_ANALYSIS_LLM_MODEL             = qwen2.5:7b
    OLLAMA_BASE_URL                      = http://ollama:11434
    CHART_ANALYSIS_LLM_INNER_TIMEOUT_S   = 13
"""
from __future__ import annotations

import json
import logging
import os
from decimal import Decimal
from typing import Any

from pydantic import BaseModel

from src.chart_analysis.domain.value_objects import NarrativeReport, ReportSource
from src.chart_analysis.infrastructure.rule_template_report_generator import (
    RuleTemplateReportGenerator,
)

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# 환경변수 기본값
# ---------------------------------------------------------------------------
_DEFAULT_MODEL = "qwen2.5:7b"
_DEFAULT_BASE_URL = "http://ollama:11434"
_DEFAULT_TIMEOUT = 13


# ---------------------------------------------------------------------------
# Pydantic 스키마 (어댑터 내부 전용 — 도메인 NarrativeReport와 분리)
# ---------------------------------------------------------------------------

class NarrativeReportSchema(BaseModel):
    """LLM 출력 파싱용 Pydantic 스키마 (어댑터 내부 전용)."""
    trend_section: str
    support_resistance_section: str
    entry_plan_section: str
    signal_evidence_section: str
    risk_section: str


# ---------------------------------------------------------------------------
# 프롬프트 상수
# ---------------------------------------------------------------------------

_SYSTEM_PROMPT = """당신은 초보 투자자를 위한 주식 차트 분석 리포트를 작성하는 전문가이다.

작성 규칙:
- 어조: 한국어 평문 (~한다, ~된다, ~보인다, ~판단된다) 어조를 사용한다
- 단정 금지: '확실히', '100%', '반드시' 같은 절대적 표현을 사용하지 않는다
- 초보자 친화: 전문 용어 옆에 괄호로 간단한 설명을 붙인다 (예: RSI(상대강도지수))
- 길이: 전체 500-800자 (한국어 기준)
- 5개 섹션을 JSON 형식으로 반환한다

반환 형식 (JSON):
{{
  "trend_section": "추세 분석 + 근거 (HH/LL, MA 정배열, ADX) — 100-180자",
  "support_resistance_section": "핵심 레벨 + 왜 이 가격대인지 — 80-150자",
  "entry_plan_section": "진입/손절/목표가 + 이유 — 80-160자",
  "signal_evidence_section": "각 신호별 근거 (RSI/거래량/패턴 등) — 100-180자",
  "risk_section": "리스크 요인 — 60-120자"
}}
"""

_HUMAN_PROMPT = """다음 수치 분석 결과를 바탕으로 리포트를 작성한다:

{analysis_json}
"""


# ---------------------------------------------------------------------------
# Generator
# ---------------------------------------------------------------------------

class LangChainOllamaReportGenerator:
    """LangChain Ollama 기반 LLM 리포트 생성기.

    도메인 LlmReportGenerator 프로토콜을 구현한다.
    의존성 주입: fallback generator (RuleTemplateReportGenerator).
    """

    def __init__(self, fallback: RuleTemplateReportGenerator | None = None) -> None:
        from langchain_ollama import ChatOllama  # type: ignore[import]
        from langchain_core.output_parsers import PydanticOutputParser  # type: ignore[import]
        from langchain_core.prompts import ChatPromptTemplate  # type: ignore[import]

        self._fallback = fallback or RuleTemplateReportGenerator()

        model = os.getenv("CHART_ANALYSIS_LLM_MODEL", _DEFAULT_MODEL)
        base_url = os.getenv("OLLAMA_BASE_URL", _DEFAULT_BASE_URL)
        timeout = int(os.getenv("CHART_ANALYSIS_LLM_INNER_TIMEOUT_S", str(_DEFAULT_TIMEOUT)))

        llm = ChatOllama(model=model, base_url=base_url, timeout=timeout)
        self._parser = PydanticOutputParser(pydantic_object=NarrativeReportSchema)

        prompt = ChatPromptTemplate.from_messages([
            ("system", _SYSTEM_PROMPT),
            ("human", _HUMAN_PROMPT),
        ])
        self._chain = prompt | llm | self._parser

    def generate(self, snapshot: object, result: object) -> NarrativeReport:
        """ChartSnapshot + ChartAnalysisResult → NarrativeReport 반환.

        실패(스키마 위반, 타임아웃, 예외) 시 RuleTemplateReportGenerator 폴백.
        """
        analysis_json = _build_analysis_json(result)

        try:
            schema_obj = self._chain.invoke({"analysis_json": analysis_json})
            return _schema_to_report(schema_obj, ReportSource.LLM_PRIMARY)
        except Exception as exc:
            logger.warning(
                "LLM 리포트 생성 실패 (1차 시도): %s — 룰 템플릿 폴백 전환",
                exc,
            )

        # 1회 재시도
        try:
            schema_obj = self._chain.invoke({"analysis_json": analysis_json})
            return _schema_to_report(schema_obj, ReportSource.LLM_PRIMARY)
        except Exception as exc:
            logger.warning(
                "LLM 리포트 생성 실패 (2차 시도): %s — 룰 템플릿 폴백 전환",
                exc,
            )

        return self._fallback.generate(snapshot, result)


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

class _DecimalEncoder(json.JSONEncoder):
    def default(self, obj: Any) -> Any:
        if isinstance(obj, Decimal):
            return str(obj)
        return super().default(obj)


def _build_analysis_json(result: object) -> str:
    """ChartAnalysisResult → LLM 입력 JSON 문자열 생성."""
    data = {
        "symbol": result.symbol,
        "window": result.window,
        "interval": result.interval,
        "recommendation": result.recommendation.grade.value,
        "confidence": result.recommendation.confidence,
        "trend": {
            "direction": result.trend.direction.value,
            "strength": result.trend.strength.value,
            "ma_alignment": result.trend.ma_alignment,
            "adx": result.trend.adx,
            "hh_ll_structure": result.trend.hh_ll_structure,
        },
        "levels": {
            "supports": list(result.levels.supports),
            "resistances": list(result.levels.resistances),
            "entry": result.trade_plan.entry_price,
            "stop_loss": result.trade_plan.stop_loss,
            "target": result.trade_plan.target_price,
            "risk_reward": result.trade_plan.risk_reward_ratio,
        },
        "indicator_signals": [
            {
                "name": s.name,
                "value": s.value,
                "interpretation": s.interpretation,
            }
            for s in result.indicator_signals
        ],
        "volume": {
            "trend": result.volume_analysis.trend,
            "spike_detected": result.volume_analysis.spike_detected,
            "avg_ratio": result.volume_analysis.avg_ratio,
        },
    }
    return json.dumps(data, cls=_DecimalEncoder, ensure_ascii=False, indent=2)


def _schema_to_report(schema_obj: Any, source: ReportSource) -> NarrativeReport:
    """NarrativeReportSchema (또는 mock 객체) → NarrativeReport 도메인 객체 변환."""
    return NarrativeReport(
        trend_section=schema_obj.trend_section,
        support_resistance_section=schema_obj.support_resistance_section,
        entry_plan_section=schema_obj.entry_plan_section,
        signal_evidence_section=schema_obj.signal_evidence_section,
        risk_section=schema_obj.risk_section,
        source=source,
    )
