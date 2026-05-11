"""차트 분석 API DTO — 도메인 객체 → API 응답 변환.

금융 안전: 모든 Decimal → str 변환 (float 금지).
"""
from __future__ import annotations

from typing import List, Optional

from pydantic import BaseModel

from chart_analysis.domain.chart_analysis_result import ChartAnalysisResult


# ---------------------------------------------------------------------------
# 중첩 DTO
# ---------------------------------------------------------------------------


class AnalysisSummaryDTO(BaseModel):
    recommendation: str
    confidence: str  # Decimal → str


class LevelsDTO(BaseModel):
    supports: List[str]
    resistances: List[str]
    entry: str
    stop_loss: str
    target: str
    risk_reward: str


class TrendDTO(BaseModel):
    direction: str
    strength: str
    ma_alignment: str
    adx: str
    hh_ll_structure: str


class PatternDTO(BaseModel):
    type: str
    index: int
    date: str


class IndicatorSignalDTO(BaseModel):
    name: str
    value: str
    interpretation: str


class TechnicalDTO(BaseModel):
    trend: TrendDTO
    patterns: List[PatternDTO]
    indicator_signals: List[IndicatorSignalDTO]


class VolumeDTO(BaseModel):
    trend: str
    spike_detected: bool
    avg_ratio: str


class ReportStatusDTO(BaseModel):
    status: str  # "available" | "none" | "pending"
    narrative: Optional[dict] = None


# ---------------------------------------------------------------------------
# 최상위 분석 윈도우 DTO
# ---------------------------------------------------------------------------


class AnalysisWindowDTO(BaseModel):
    window: str
    interval: str
    computed_at: str
    snapshot_hash: str
    summary: AnalysisSummaryDTO
    levels: LevelsDTO
    technical: TechnicalDTO
    volume: VolumeDTO
    report: ReportStatusDTO

    @classmethod
    def from_domain(cls, result: ChartAnalysisResult) -> "AnalysisWindowDTO":
        """도메인 ChartAnalysisResult → AnalysisWindowDTO 변환.

        모든 Decimal 값은 str()으로 변환한다.
        """
        report_status = "available" if result.report is not None else "none"

        return cls(
            window=result.window,
            interval=result.interval,
            computed_at=result.numeric_computed_at.isoformat(),
            snapshot_hash=result.snapshot_hash,
            summary=AnalysisSummaryDTO(
                recommendation=result.recommendation.grade.value,
                confidence=str(result.recommendation.confidence),
            ),
            levels=LevelsDTO(
                supports=[str(v) for v in result.levels.supports],
                resistances=[str(v) for v in result.levels.resistances],
                entry=str(result.trade_plan.entry_price),
                stop_loss=str(result.trade_plan.stop_loss),
                target=str(result.trade_plan.target_price),
                risk_reward=str(result.trade_plan.risk_reward_ratio),
            ),
            technical=TechnicalDTO(
                trend=TrendDTO(
                    direction=result.trend.direction.value,
                    strength=result.trend.strength.value,
                    ma_alignment=result.trend.ma_alignment,
                    adx=str(result.trend.adx),
                    hh_ll_structure=result.trend.hh_ll_structure,
                ),
                patterns=[
                    PatternDTO(
                        type=p.type.value,
                        index=p.index,
                        date=p.date.isoformat(),
                    )
                    for p in result.patterns
                ],
                indicator_signals=[
                    IndicatorSignalDTO(
                        name=sig.name,
                        value=str(sig.value),
                        interpretation=sig.interpretation,
                    )
                    for sig in result.indicator_signals
                ],
            ),
            volume=VolumeDTO(
                trend=result.volume_analysis.trend,
                spike_detected=result.volume_analysis.spike_detected,
                avg_ratio=str(result.volume_analysis.avg_ratio),
            ),
            report=ReportStatusDTO(status=report_status, narrative=None),
        )


# ---------------------------------------------------------------------------
# 최상위 응답 DTO
# ---------------------------------------------------------------------------


class ChartAnalysisResponseDTO(BaseModel):
    symbol: str
    analyses: List[AnalysisWindowDTO]


# ---------------------------------------------------------------------------
# LLM 리포트 요청/응답 DTO
# ---------------------------------------------------------------------------


class RequestLlmReportRequestDTO(BaseModel):
    symbol: str
    window: str
    interval: str


class RequestLlmReportResponseDTO(BaseModel):
    status: str
    queue_position: int
    estimated_wait: str
