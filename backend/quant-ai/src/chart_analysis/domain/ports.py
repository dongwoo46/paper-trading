"""도메인 포트 (Port) 정의 — 의존성 역전 원칙.

quant-ai는 LLM 리포트 생성만 담당한다.
저장소, 계산기 포트는 quant-research로 이전되었다.
"""
from __future__ import annotations

from .value_objects import NarrativeReport


# ---------------------------------------------------------------------------
# QueueItem 데이터 클래스 (도메인 내부 타입)
# ---------------------------------------------------------------------------

class QueueItem:
    """분석 요청 큐 아이템."""

    __slots__ = ("id", "symbol", "window", "interval", "status")

    def __init__(
        self,
        id: int,
        symbol: str,
        window: str,
        interval: str,
        status: str,
    ) -> None:
        self.id = id
        self.symbol = symbol
        self.window = window
        self.interval = interval
        self.status = status


# ---------------------------------------------------------------------------
# LLM Report Generator Port
# ---------------------------------------------------------------------------

from typing import Protocol, runtime_checkable  # noqa: E402


@runtime_checkable
class LlmReportGenerator(Protocol):
    """LLM 자연어 리포트 생성 포트.

    도메인은 LangChain을 직접 알지 못한다.
    인프라 어댑터: LangChainOllamaReportGenerator | RuleTemplateReportGenerator
    """

    def generate(self, snapshot: object, result: object) -> NarrativeReport:
        """ChartSnapshot + ChartAnalysisResult를 받아 NarrativeReport를 반환한다."""
        ...
