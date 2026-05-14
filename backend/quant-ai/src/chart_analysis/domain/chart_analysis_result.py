"""ChartAnalysisResult Aggregate Root — 분석 출력 단위."""
from __future__ import annotations

import dataclasses
from dataclasses import dataclass
from datetime import datetime
from typing import Optional

from .value_objects import (
    CandlePattern,
    IndicatorSignal,
    LevelSet,
    NarrativeReport,
    Recommendation,
    ReportSource,
    TradePlan,
    TrendAnalysis,
    VolumeAnalysis,
)


@dataclass(frozen=True)
class ChartAnalysisResult:
    """분석 결과 Aggregate Root.

    식별자: (symbol, window, interval)
    불변 객체. 리포트 추가 시 with_report()로 새 인스턴스 반환.
    """

    symbol: str
    window: str
    interval: str
    snapshot_hash: str
    trend: TrendAnalysis
    levels: LevelSet
    trade_plan: TradePlan
    patterns: tuple[CandlePattern, ...]
    indicator_signals: tuple[IndicatorSignal, ...]
    volume_analysis: VolumeAnalysis
    recommendation: Recommendation
    report: Optional[NarrativeReport]
    report_source: ReportSource
    numeric_computed_at: datetime
    llm_computed_at: Optional[datetime]

    def __init__(
        self,
        symbol: str,
        window: str,
        interval: str,
        snapshot_hash: str,
        trend: TrendAnalysis,
        levels: LevelSet,
        trade_plan: TradePlan,
        patterns: list[CandlePattern],
        indicator_signals: list[IndicatorSignal],
        volume_analysis: VolumeAnalysis,
        recommendation: Recommendation,
        report: Optional[NarrativeReport],
        report_source: ReportSource,
        numeric_computed_at: datetime,
        llm_computed_at: Optional[datetime],
    ) -> None:
        object.__setattr__(self, "symbol", symbol)
        object.__setattr__(self, "window", window)
        object.__setattr__(self, "interval", interval)
        object.__setattr__(self, "snapshot_hash", snapshot_hash)
        object.__setattr__(self, "trend", trend)
        object.__setattr__(self, "levels", levels)
        object.__setattr__(self, "trade_plan", trade_plan)
        object.__setattr__(self, "patterns", tuple(patterns))
        object.__setattr__(self, "indicator_signals", tuple(indicator_signals))
        object.__setattr__(self, "volume_analysis", volume_analysis)
        object.__setattr__(self, "recommendation", recommendation)
        object.__setattr__(self, "report", report)
        object.__setattr__(self, "report_source", report_source)
        object.__setattr__(self, "numeric_computed_at", numeric_computed_at)
        object.__setattr__(self, "llm_computed_at", llm_computed_at)

    # ------------------------------------------------------------------
    # Command methods (return new instance — immutable)
    # ------------------------------------------------------------------

    def with_report(
        self, report: NarrativeReport, source: ReportSource
    ) -> "ChartAnalysisResult":
        """리포트를 포함한 새 인스턴스를 반환한다 (원본 불변)."""
        return dataclasses.replace(self, report=report, report_source=source)

    # ------------------------------------------------------------------
    # Query / serialization
    # ------------------------------------------------------------------

    def to_response_dict(self) -> dict:
        """API 응답용 딕셔너리 직렬화.

        모든 Decimal 값은 str로 변환한다 (float 금지).
        """
        return {
            "symbol": self.symbol,
            "window": self.window,
            "interval": self.interval,
            "snapshot_hash": self.snapshot_hash,
            "computed_at": self.numeric_computed_at.isoformat(),
            "summary": {
                "recommendation": self.recommendation.grade.value,
                "confidence": str(self.recommendation.confidence),
            },
            "levels": {
                "supports": [str(v) for v in self.levels.supports],
                "resistances": [str(v) for v in self.levels.resistances],
                "entry": str(self.trade_plan.entry_price),
                "stop_loss": str(self.trade_plan.stop_loss),
                "target": str(self.trade_plan.target_price),
                "risk_reward": str(self.trade_plan.risk_reward_ratio),
            },
            "technical": {
                "trend": {
                    "direction": self.trend.direction.value,
                    "strength": self.trend.strength.value,
                    "ma_alignment": self.trend.ma_alignment,
                    "adx": str(self.trend.adx),
                    "hh_ll_structure": self.trend.hh_ll_structure,
                },
                "patterns": [
                    {
                        "type": p.type.value,
                        "index": p.index,
                        "date": p.date.isoformat(),
                    }
                    for p in self.patterns
                ],
                "indicator_signals": [
                    {
                        "name": sig.name,
                        "value": str(sig.value),
                        "interpretation": sig.interpretation,
                    }
                    for sig in self.indicator_signals
                ],
            },
            "volume": {
                "trend": self.volume_analysis.trend,
                "spike_detected": self.volume_analysis.spike_detected,
                "avg_ratio": str(self.volume_analysis.avg_ratio),
            },
            "report": {
                "status": "available" if self.report is not None else "none",
                "source": self.report_source.value,
                "narrative": _serialize_report(self.report) if self.report is not None else None,
            },
        }


def _serialize_report(report: NarrativeReport) -> dict:
    return {
        "trend_section": report.trend_section,
        "support_resistance_section": report.support_resistance_section,
        "entry_plan_section": report.entry_plan_section,
        "signal_evidence_section": report.signal_evidence_section,
        "risk_section": report.risk_section,
        "source": report.source.value,
    }