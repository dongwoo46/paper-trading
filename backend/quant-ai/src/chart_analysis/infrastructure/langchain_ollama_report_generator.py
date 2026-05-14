"""LangChainOllamaReportGenerator — LlmReportGenerator 메인 어댑터.

LangChain Ollama를 사용하여 한국어 자연어 리포트를 생성한다.
format="json" 강제 모드로 구조화 출력을 보장한다.
스키마 위반 시 1회 재시도 후 실패하면 호출자에게 예외 전파.

환경변수:
    CHART_ANALYSIS_LLM_MODEL             = qwen2.5:7b
    OLLAMA_BASE_URL                      = http://localhost:11434
    CHART_ANALYSIS_LLM_INNER_TIMEOUT_S   = 60
"""
from __future__ import annotations

import json
import logging
import os
from decimal import Decimal
from typing import Any

from pydantic import BaseModel, ValidationError

from src.chart_analysis.domain.value_objects import NarrativeReport, ReportSource

logger = logging.getLogger(__name__)

_DEFAULT_MODEL = "qwen2.5:7b"
_DEFAULT_BASE_URL = "http://localhost:11434"
_DEFAULT_TIMEOUT = 60


class NarrativeReportSchema(BaseModel):
    """LLM JSON 출력 검증 스키마 (어댑터 내부 전용)."""
    trend_section: str
    support_resistance_section: str
    entry_plan_section: str
    signal_evidence_section: str
    risk_section: str


# format="json" 모드에서는 LLM이 JSON을 직접 출력하므로
# 프롬프트에 JSON 예시를 넣지 않는다 — 5개 키 이름만 명시한다.
_SYSTEM_PROMPT = """당신은 초보 투자자를 위한 주식 차트 분석 리포트 작성 전문가이다.
반드시 한국어로만 작성한다. 다른 언어를 절대 사용하지 않는다.

작성 규칙:
- 어조: ~한다, ~된다, ~보인다, ~판단된다 (평문체)
- 단정 금지: '확실히', '100%', '반드시' 같은 절대적 표현 사용 금지
- 초보자 친화: 전문 용어 옆에 괄호로 설명 (예: RSI(상대강도지수))
- 전체 길이: 500-800자 (한국어 기준)

반드시 다음 5개 키만 포함한 JSON 객체를 반환한다:
- trend_section: 추세 분석 + 근거 (100-180자)
- support_resistance_section: 핵심 지지·저항 레벨 설명 (80-150자)
- entry_plan_section: 진입·손절·목표가와 이유 (80-160자)
- signal_evidence_section: 각 지표 신호별 근거 (100-180자)
- risk_section: 주요 리스크 요인 (60-120자)"""

_HUMAN_PROMPT = """아래 수치 분석 데이터를 바탕으로 한국어 투자 리포트를 작성한다:

{analysis_json}"""


class LangChainOllamaReportGenerator:
    """LangChain Ollama 기반 LLM 리포트 생성기.

    format="json" 강제 모드 + 직접 JSON 파싱.
    """

    def __init__(self) -> None:
        from langchain_ollama import ChatOllama  # type: ignore[import]
        from langchain_core.prompts import ChatPromptTemplate  # type: ignore[import]

        model = os.getenv("CHART_ANALYSIS_LLM_MODEL", _DEFAULT_MODEL)
        base_url = os.getenv("OLLAMA_BASE_URL", _DEFAULT_BASE_URL)
        timeout = int(os.getenv("CHART_ANALYSIS_LLM_INNER_TIMEOUT_S", str(_DEFAULT_TIMEOUT)))

        llm = ChatOllama(model=model, base_url=base_url, timeout=timeout, format="json")
        prompt = ChatPromptTemplate.from_messages([
            ("system", _SYSTEM_PROMPT),
            ("human", _HUMAN_PROMPT),
        ])
        self._chain = prompt | llm

    def generate(self, snapshot: object, result: object) -> NarrativeReport:
        """ChartAnalysisResult → NarrativeReport.

        실패(파싱 오류, 타임아웃) 시 1회 재시도 후 예외 전파.
        """
        analysis_json = _build_analysis_json(result)

        for attempt in (1, 2):
            try:
                response = self._chain.invoke({"analysis_json": analysis_json})
                schema_obj = _parse_response(response.content)
                return _to_report(schema_obj)
            except Exception as exc:
                logger.warning("LLM 리포트 생성 실패 (%d차 시도): %s", attempt, exc)

        raise RuntimeError("LLM 리포트 생성 2회 모두 실패")


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

class _DecimalEncoder(json.JSONEncoder):
    def default(self, obj: Any) -> Any:
        if isinstance(obj, Decimal):
            return str(obj)
        return super().default(obj)


def _build_analysis_json(result: object) -> str:
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
            {"name": s.name, "value": s.value, "interpretation": s.interpretation}
            for s in result.indicator_signals
        ],
        "volume": {
            "trend": result.volume_analysis.trend,
            "spike_detected": result.volume_analysis.spike_detected,
            "avg_ratio": result.volume_analysis.avg_ratio,
        },
    }
    return json.dumps(data, cls=_DecimalEncoder, ensure_ascii=False, indent=2)


def _parse_response(content: str) -> NarrativeReportSchema:
    """LLM 출력 문자열 → NarrativeReportSchema.

    format="json" 모드여도 가끔 마크다운 블록이 붙는 경우를 방어한다.
    """
    text = content.strip()

    # 마크다운 코드블록 제거
    if text.startswith("```"):
        lines = text.splitlines()
        inner = [l for l in lines if not l.startswith("```")]
        text = "\n".join(inner).strip()

    data = json.loads(text)
    return NarrativeReportSchema(**data)


def _to_report(schema: NarrativeReportSchema) -> NarrativeReport:
    return NarrativeReport(
        trend_section=schema.trend_section,
        support_resistance_section=schema.support_resistance_section,
        entry_plan_section=schema.entry_plan_section,
        signal_evidence_section=schema.signal_evidence_section,
        risk_section=schema.risk_section,
        source=ReportSource.LLM_PRIMARY,
    )
