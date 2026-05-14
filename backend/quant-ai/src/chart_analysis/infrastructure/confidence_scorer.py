"""WeightedRuleConfidenceScorer — 룰 기반 가중치 합산 Confidence 산정기.

가중치 (코드 내 상수, 환경변수로 임계값 외부화):
  +0.25  MA 정배열(MA20>MA60) + ADX strong
  +0.15  강세 패턴 (Hammer / Bullish Engulfing / Morning Star)
  +0.10  RSI < 30 (반등 신호)
  +0.10  거래량 spike + 양봉 추세
  -0.15  약세 패턴 (Bearish Engulfing / Evening Star)
  -0.10  RSI > 70

합산 → clamp [-1.0, 1.0] → [0.0, 1.0] 정규화 → grade 매핑

임계값 (환경변수 외부화):
  CHART_ANALYSIS_CONFIDENCE_STRONG_THRESHOLD (default 0.7)
  CHART_ANALYSIS_CONFIDENCE_BUY_THRESHOLD (default 0.4)

금융 안전: confidence는 Decimal 반환.
"""
from __future__ import annotations

import os
from decimal import Decimal, ROUND_HALF_UP

from src.chart_analysis.domain.value_objects import (
    CandlePattern,
    Grade,
    IndicatorSignal,
    PatternType,
    Recommendation,
    Strength,
    TrendAnalysis,
    VolumeAnalysis,
)

# ------------------------------------------------------------------ #
# 가중치 상수 (모두 Decimal)
# ------------------------------------------------------------------ #
_W_MA_ADX_BULLISH = Decimal("0.25")
_W_BULLISH_PATTERN = Decimal("0.15")
_W_RSI_OVERSOLD = Decimal("0.10")
_W_VOLUME_SPIKE_BULLISH = Decimal("0.10")
_W_BEARISH_PATTERN = Decimal("-0.15")
_W_RSI_OVERBOUGHT = Decimal("-0.10")
_W_MA_ADX_BEARISH = Decimal("-0.25")

# 기준 임계값
_DEFAULT_STRONG_THRESHOLD = Decimal("0.7")
_DEFAULT_BUY_THRESHOLD = Decimal("0.4")

_ZERO = Decimal("0")
_ONE = Decimal("1")
_TWO = Decimal("2")

_BULLISH_PATTERN_TYPES = frozenset({
    PatternType.HAMMER,
    PatternType.INVERTED_HAMMER,
    PatternType.BULLISH_ENGULFING,
    PatternType.MORNING_STAR,
})
_BEARISH_PATTERN_TYPES = frozenset({
    PatternType.BEARISH_ENGULFING,
    PatternType.EVENING_STAR,
})


def _get_threshold(env_var: str, default: Decimal) -> Decimal:
    """환경변수에서 임계값을 읽는다. 없으면 default 반환."""
    raw = os.environ.get(env_var)
    if raw is not None:
        try:
            return Decimal(raw)
        except Exception:
            pass
    return default


class WeightedRuleConfidenceScorer:
    """룰 기반 가중치 합산 Confidence 산정기.

    ConfidenceScorer 포트를 구현한다.
    """

    def score(
        self,
        trend: TrendAnalysis,
        patterns: list[CandlePattern],
        indicator_signals: list[IndicatorSignal],
        volume_analysis: VolumeAnalysis,
    ) -> Recommendation:
        """분석 결과를 받아 Recommendation (grade + confidence)을 반환한다.

        Args:
            trend: 추세 분석 결과.
            patterns: 탐지된 캔들 패턴 리스트.
            indicator_signals: 보조지표 신호 리스트.
            volume_analysis: 거래량 분석 결과.

        Returns:
            Recommendation — confidence in [0.0, 1.0], grade in Grade enum.
        """
        raw_score = _ZERO

        # MA 정배열 + ADX 강도
        raw_score += self._score_ma_adx(trend)

        # 패턴 신호
        raw_score += self._score_patterns(patterns)

        # RSI 신호
        raw_score += self._score_rsi(indicator_signals)

        # 거래량 spike
        raw_score += self._score_volume(volume_analysis)

        # clamp to [-1, 1] then normalize to [0, 1]
        clamped = max(Decimal("-1"), min(Decimal("1"), raw_score))
        confidence = (clamped + _ONE) / _TWO  # [-1,1] → [0,1]

        # 임계값 (환경변수 외부화)
        strong_threshold = _get_threshold("CHART_ANALYSIS_CONFIDENCE_STRONG_THRESHOLD", _DEFAULT_STRONG_THRESHOLD)
        buy_threshold = _get_threshold("CHART_ANALYSIS_CONFIDENCE_BUY_THRESHOLD", _DEFAULT_BUY_THRESHOLD)
        # grade 결정
        grade = self._map_to_grade(
            raw_score=raw_score,
            confidence=confidence,
            strong_threshold=strong_threshold,
            buy_threshold=buy_threshold,
        )

        # confidence를 4자리 소수점으로 반올림
        confidence_rounded = confidence.quantize(Decimal("0.0001"), rounding=ROUND_HALF_UP)

        return Recommendation(grade=grade, confidence=confidence_rounded)

    # ------------------------------------------------------------------ #
    # Private scoring helpers
    # ------------------------------------------------------------------ #

    @staticmethod
    def _score_ma_adx(trend: TrendAnalysis) -> Decimal:
        """MA 정배열 + ADX 강도 점수."""
        strong_medium = trend.strength in (Strength.STRONG, Strength.MEDIUM)
        if trend.ma_alignment == "MA20>MA60" and strong_medium:
            return _W_MA_ADX_BULLISH
        if trend.ma_alignment == "MA20<MA60" and strong_medium:
            return _W_MA_ADX_BEARISH
        return _ZERO

    @staticmethod
    def _score_patterns(patterns: list[CandlePattern]) -> Decimal:
        """패턴 신호 점수."""
        score = _ZERO
        has_bullish = any(p.type in _BULLISH_PATTERN_TYPES for p in patterns)
        has_bearish = any(p.type in _BEARISH_PATTERN_TYPES for p in patterns)
        if has_bullish:
            score += _W_BULLISH_PATTERN
        if has_bearish:
            score += _W_BEARISH_PATTERN
        return score

    @staticmethod
    def _score_rsi(signals: list[IndicatorSignal]) -> Decimal:
        """RSI 신호 점수."""
        score = _ZERO
        for sig in signals:
            if sig.name.upper() == "RSI":
                if sig.value < Decimal("30"):
                    score += _W_RSI_OVERSOLD
                elif sig.value > Decimal("70"):
                    score += _W_RSI_OVERBOUGHT
        return score

    @staticmethod
    def _score_volume(volume: VolumeAnalysis) -> Decimal:
        """거래량 spike 점수."""
        if volume.spike_detected and volume.trend in ("up", "bullish"):
            return _W_VOLUME_SPIKE_BULLISH
        return _ZERO

    @staticmethod
    def _map_to_grade(
        raw_score: Decimal,
        confidence: Decimal,
        strong_threshold: Decimal,
        buy_threshold: Decimal,
    ) -> Grade:
        """confidence와 원시 점수 방향으로 grade를 매핑한다.

        Mapping (decisions §3):
          long + confidence > strong_threshold → STRONG_BUY
          long + buy_threshold < confidence ≤ strong_threshold → BUY
          |raw_score| 작음 (≤ buy_threshold 양쪽) → HOLD
          short + buy_threshold < confidence ≤ strong_threshold → SELL
          short + confidence > strong_threshold → STRONG_SELL
        """
        # raw_score > 0: long signal, raw_score < 0: short signal
        is_long = raw_score > _ZERO
        is_short = raw_score < _ZERO

        if is_long:
            if confidence > strong_threshold:
                return Grade.STRONG_BUY
            if confidence > buy_threshold:
                return Grade.BUY
        elif is_short:
            # short 측: confidence가 높을수록 매도 신호 강함
            # raw_score < 0이면 confidence = (raw_score+1)/2 < 0.5
            # 대칭 매핑: sell_confidence = 1 - confidence
            sell_confidence = _ONE - confidence
            if sell_confidence > strong_threshold:
                return Grade.STRONG_SELL
            if sell_confidence > buy_threshold:
                return Grade.SELL

        return Grade.HOLD
