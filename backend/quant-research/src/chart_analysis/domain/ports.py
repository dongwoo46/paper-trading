"""도메인 포트 (Port) 정의 — 의존성 역전 원칙.

도메인은 인프라(LangChain, SQLAlchemy, pandas, redis)를 직접 알지 못한다.
모든 외부 의존성은 이 파일의 Protocol을 통해 추상화한다.
"""
from __future__ import annotations

from decimal import Decimal
from typing import Optional, Protocol, runtime_checkable

from .value_objects import (
    Candle,
    CandlePattern,
    IndicatorSet,
    IndicatorSignal,
    LevelSet,
    NarrativeReport,
    Recommendation,
    TrendAnalysis,
    VolumeAnalysis,
)


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
# Repository Ports
# ---------------------------------------------------------------------------

@runtime_checkable
class OhlcvRepository(Protocol):
    """OHLCV 데이터 조회 포트.

    인프라 어댑터: src/chart_analysis/infrastructure/ohlcv_repository.py
    """

    def find_window(
        self, symbol: str, window: str, interval: str
    ) -> list[Candle]:
        """지정 종목/윈도우/간격의 OHLCV 봉 목록을 반환한다."""
        ...


@runtime_checkable
class ChartAnalysisRepository(Protocol):
    """ChartAnalysisResult 영속화 포트.

    인프라 어댑터: src/chart_analysis/infrastructure/chart_analysis_repository.py
    """

    def upsert(self, result: object) -> None:
        """분석 결과를 저장하거나 갱신한다."""
        ...

    def find_by_symbol(self, symbol: str) -> list[object]:
        """종목코드로 모든 윈도우 분석 결과를 반환한다."""
        ...

    def find_one(
        self, symbol: str, window: str, interval: str
    ) -> Optional[object]:
        """단일 (symbol, window, interval) 분석 결과를 반환한다. 없으면 None."""
        ...


@runtime_checkable
class AnalysisRequestQueueRepository(Protocol):
    """LLM 분석 요청 큐 포트.

    인프라 어댑터: src/chart_analysis/infrastructure/analysis_request_queue_repository.py
    """

    def enqueue(self, symbol: str, window: str, interval: str) -> None:
        """분석 요청을 큐에 등록한다. 중복 시 requested_count 증가."""
        ...

    def fetch_pending(self, limit: int) -> list[object]:
        """pending 상태 아이템을 요청 시간 순으로 limit개 반환한다."""
        ...

    def mark_processed(self, id: int, status: str) -> None:
        """아이템 처리 상태를 갱신한다."""
        ...


# ---------------------------------------------------------------------------
# LLM Report Generator Port
# ---------------------------------------------------------------------------

@runtime_checkable
class LlmReportGenerator(Protocol):
    """LLM 자연어 리포트 생성 포트.

    도메인은 LangChain을 직접 알지 못한다.
    인프라 어댑터: LangChainOllamaReportGenerator | RuleTemplateReportGenerator
    """

    def generate(self, snapshot: object, result: object) -> NarrativeReport:
        """ChartSnapshot + ChartAnalysisResult를 받아 NarrativeReport를 반환한다."""
        ...


# ---------------------------------------------------------------------------
# Domain Service Ports (계산 위임)
# ---------------------------------------------------------------------------

@runtime_checkable
class IndicatorCalculator(Protocol):
    """보조지표 계산 포트.

    인프라 어댑터: PandasTaIndicatorCalculator (pandas-ta 위임)
    """

    def calculate(self, candles: list[Candle]) -> IndicatorSet:
        """봉 목록으로 보조지표 세트를 계산한다."""
        ...


@runtime_checkable
class SupportResistanceFinder(Protocol):
    """지지·저항선 탐지 포트.

    인프라 어댑터: ScipyPeaksSupportResistanceFinder (scipy.signal.find_peaks 기반)
    """

    def find(
        self,
        candles: list[Candle],
        atr: Decimal,
        *,
        window: str | None = None,
        interval: str | None = None,
        last_close: Decimal | None = None,
    ) -> LevelSet:
        """봉 목록과 ATR을 받아 지지·저항 레벨 세트를 반환한다."""
        ...


@runtime_checkable
class PatternDetector(Protocol):
    """캔들 패턴 탐지 포트.

    인프라 어댑터: RuleBasedPatternDetector (6종 패턴 자체 구현)
    """

    def detect(self, candles: list[Candle]) -> list[CandlePattern]:
        """봉 목록에서 캔들 패턴 목록을 반환한다."""
        ...


@runtime_checkable
class TrendClassifier(Protocol):
    """추세 분류 포트.

    인프라 어댑터: MaTrendClassifier (MA 정배열 + ADX + HH/LL)
    """

    def classify(
        self, candles: list[Candle], indicators: IndicatorSet
    ) -> TrendAnalysis:
        """봉 + 보조지표로 추세를 분류한다."""
        ...


@runtime_checkable
class ConfidenceScorer(Protocol):
    """Confidence 산정 포트.

    인프라 어댑터: WeightedRuleConfidenceScorer (룰 기반 가중치 합산 → 정규화)
    """

    def score(
        self,
        trend: TrendAnalysis,
        patterns: list[CandlePattern],
        indicator_signals: list[IndicatorSignal],
        volume_analysis: VolumeAnalysis,
        *,
        levels: LevelSet | None = None,
        trade_plan: object | None = None,
        last_close: Decimal | None = None,
    ) -> Recommendation:
        """분석 결과를 받아 Recommendation (grade + confidence)을 반환한다."""
        ...
