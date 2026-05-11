"""chart_analysis 패키지 — 차트 분석 AI 에이전트 도메인."""
from __future__ import annotations

from .domain.chart_analysis_result import ChartAnalysisResult
from .domain.chart_snapshot import ChartSnapshot
from .domain.ports import (
    AnalysisRequestQueueRepository,
    ChartAnalysisRepository,
    ConfidenceScorer,
    IndicatorCalculator,
    LlmReportGenerator,
    OhlcvRepository,
    PatternDetector,
    SupportResistanceFinder,
    TrendClassifier,
)
from .domain.value_objects import (
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

__all__ = [
    # Aggregates
    "ChartSnapshot",
    "ChartAnalysisResult",
    # Value Objects
    "Candle",
    "IndicatorSet",
    "TrendAnalysis",
    "LevelSet",
    "TradePlan",
    "CandlePattern",
    "IndicatorSignal",
    "VolumeAnalysis",
    "Recommendation",
    "NarrativeReport",
    # Enums
    "Direction",
    "Strength",
    "Grade",
    "PatternType",
    "ReportSource",
    # Ports
    "OhlcvRepository",
    "ChartAnalysisRepository",
    "AnalysisRequestQueueRepository",
    "LlmReportGenerator",
    "IndicatorCalculator",
    "SupportResistanceFinder",
    "PatternDetector",
    "TrendClassifier",
    "ConfidenceScorer",
]