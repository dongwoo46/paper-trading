"""RuleTemplateReportGenerator — LlmReportGenerator 폴백 어댑터.

LLM 없이 수치 분석 결과에서 한국어 룰 기반 자연어 리포트를 생성한다.
결정론적, 외부 호출 없음.
어조: ~한다 (~로 판단된다, ~로 보인다), 단정 금지, 초보자 친화.
"""
from __future__ import annotations

from chart_analysis.domain.value_objects import (
    Direction,
    Grade,
    NarrativeReport,
    ReportSource,
    Strength,
)


class RuleTemplateReportGenerator:
    """룰 기반 5섹션 한국어 리포트 생성기.

    도메인 LlmReportGenerator 프로토콜을 구현한다.
    동일 입력에 항상 동일 출력을 반환한다 (결정론적).
    """

    def generate(self, snapshot: object, result: object) -> NarrativeReport:
        """ChartSnapshot + ChartAnalysisResult → NarrativeReport 반환."""
        return NarrativeReport(
            trend_section=self._trend_section(result),
            support_resistance_section=self._support_resistance_section(result),
            entry_plan_section=self._entry_plan_section(result),
            signal_evidence_section=self._signal_evidence_section(result),
            risk_section=self._risk_section(result),
            source=ReportSource.RULE_TEMPLATE,
        )

    # ------------------------------------------------------------------
    # 5섹션 생성 메서드
    # ------------------------------------------------------------------

    def _trend_section(self, result: object) -> str:
        trend = result.trend
        rec = result.recommendation

        direction_label = self._direction_label(trend.direction)
        strength_label = self._strength_label(trend.strength)
        grade_label = rec.grade.value
        confidence_pct = f"{float(rec.confidence) * 100:.1f}"

        return (
            f"현재 {result.symbol} 종목은 {direction_label} 흐름으로 판단된다. "
            f"이동평균선 정렬({trend.ma_alignment})과 ADX 지표({trend.adx}) 기준으로 "
            f"추세 강도는 {strength_label} 수준으로 분석된다. "
            f"고점·저점 구조({trend.hh_ll_structure})가 확인되며, "
            f"종합 신뢰도 {confidence_pct}% 기준으로 {grade_label} 판단으로 분석된다."
        )

    def _support_resistance_section(self, result: object) -> str:
        levels = result.levels
        supports_str = ", ".join(str(s) for s in levels.supports)
        resistances_str = ", ".join(str(r) for r in levels.resistances)

        if not levels.supports and not levels.resistances:
            return (
                "현재 주요 지지·저항 레벨이 명확히 형성되지 않은 상태로 보인다. "
                "가격 변동성에 유의하며 관망하는 전략도 고려할 수 있다."
            )

        parts = []
        if levels.supports:
            parts.append(f"지지 구간({supports_str})은 가격이 하락할 때 매수세가 유입될 가능성이 있는 레벨로 분석된다")
        if levels.resistances:
            parts.append(
                f"저항 구간({resistances_str})은 상승 시 매도 압력이 나타날 수 있는 가격대로 판단된다"
            )

        return ". ".join(parts) + "."

    def _entry_plan_section(self, result: object) -> str:
        tp = result.trade_plan
        return (
            f"진입가 {tp.entry_price}원 수준에서 매수를 고려할 수 있으며, "
            f"손절가는 {tp.stop_loss}원으로 설정하는 전략이 분석된다. "
            f"목표가는 {tp.target_price}원이며, "
            f"위험 대비 수익 비율(Risk/Reward)은 약 {tp.risk_reward_ratio}배로 계산된다. "
            f"투자 시 개인의 위험 허용 범위를 충분히 고려하는 것이 중요하다."
        )

    def _signal_evidence_section(self, result: object) -> str:
        signals = result.indicator_signals
        if not signals:
            return (
                "보조지표 신호가 충분히 축적되지 않은 상태로 보인다. "
                "추가적인 데이터 확보 후 분석이 필요한 것으로 판단된다."
            )

        lines = []
        for sig in signals:
            interpretation_label = self._interpretation_label(sig.interpretation)
            lines.append(
                f"{sig.name}({sig.value}): {interpretation_label} 상태로 분석된다"
            )

        vol = result.volume_analysis
        vol_label = "증가" if vol.trend == "increasing" else ("감소" if vol.trend == "decreasing" else "횡보")
        spike_note = " (거래량 급증이 감지되었다)" if vol.spike_detected else ""
        lines.append(
            f"거래량은 평균 대비 {vol.avg_ratio}배 수준으로 {vol_label} 추세{spike_note}로 보인다"
        )

        return ". ".join(lines) + "."

    def _risk_section(self, result: object) -> str:
        trend = result.trend
        tp = result.trade_plan

        risks = []
        if trend.direction == Direction.DOWNTREND:
            risks.append("현재 하락 추세가 지속될 가능성이 있어 신중한 접근이 필요한 것으로 판단된다")
        if trend.strength == Strength.WEAK:
            risks.append("추세 강도가 약한 상태로, 방향성이 급변할 가능성에 유의해야 한다")
        risks.append(
            f"손절가({tp.stop_loss}원) 이탈 시 추가 하락 리스크가 있으므로 "
            f"원칙적인 손절매 실행이 중요하다고 판단된다"
        )
        risks.append("모든 투자 판단의 최종 책임은 투자자 본인에게 있다")

        return ". ".join(risks) + "."

    # ------------------------------------------------------------------
    # Label helpers
    # ------------------------------------------------------------------

    @staticmethod
    def _direction_label(direction: Direction) -> str:
        return {
            Direction.UPTREND: "상승",
            Direction.DOWNTREND: "하락",
            Direction.SIDEWAYS: "횡보",
        }.get(direction, "불명확")

    @staticmethod
    def _strength_label(strength: Strength) -> str:
        return {
            Strength.WEAK: "약한",
            Strength.MEDIUM: "중간",
            Strength.STRONG: "강한",
        }.get(strength, "불명확")

    @staticmethod
    def _interpretation_label(interpretation: str) -> str:
        labels = {
            "neutral_bullish": "중립 매수 우위",
            "neutral_bearish": "중립 매도 우위",
            "overbought": "과매수",
            "oversold": "과매도",
            "golden_cross_3d": "골든크로스(최근 3일)",
            "dead_cross_3d": "데드크로스(최근 3일)",
            "bullish": "상승 신호",
            "bearish": "하락 신호",
        }
        return labels.get(interpretation, interpretation)
