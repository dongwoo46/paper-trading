"""MaAdxTrendClassifier — MA 정배열 + ADX + HH/LL 기반 추세 분류기.

분류 로직:
  - MA 정배열: MA20/MA60 비교 (±0.5% 이내 FLAT)
  - ADX 강도: >25 STRONG, 20-25 MEDIUM, <20 WEAK
  - HH/LL: 직전 20봉 스윙 고점/저점 방향성
  - Direction:
    - MA20>MA60 AND ADX STRONG/MEDIUM → UPTREND
    - MA20<MA60 AND ADX STRONG/MEDIUM → DOWNTREND
    - 그 외 → SIDEWAYS

금융 안전: adx 필드는 Decimal 반환.
"""
from __future__ import annotations

from decimal import Decimal

from src.chart_analysis.domain.value_objects import (
    Candle,
    Direction,
    IndicatorSet,
    Strength,
    TrendAnalysis,
)

# ADX 임계값
_ADX_STRONG = Decimal("25")
_ADX_MEDIUM_LOW = Decimal("20")
# MA 정배열 허용 오차 (±0.5%)
_MA_FLAT_THRESHOLD = Decimal("0.005")
# HH/LL 판정에 사용할 최근 봉 수
_HH_LL_WINDOW = 20


class MaAdxTrendClassifier:
    """MA 정배열 + ADX 강도 + HH/LL 구조 기반 추세 분류기.

    TrendClassifier 포트를 구현한다.
    """

    def classify(
        self, candles: list[Candle], indicators: IndicatorSet
    ) -> TrendAnalysis:
        """봉 + 보조지표로 추세를 분류한다.

        Args:
            candles: 시간 순으로 정렬된 Candle 리스트 (최소 3봉 권장).
            indicators: 사전 계산된 보조지표 세트.

        Returns:
            TrendAnalysis — direction, strength, ma_alignment, adx, hh_ll_structure.
        """
        ma_alignment = self._compute_ma_alignment(indicators.ma20, indicators.ma60)
        strength = self._compute_adx_strength(indicators.adx14)
        hh_ll = self._compute_hh_ll(candles)
        direction = self._compute_direction(ma_alignment, strength)

        return TrendAnalysis(
            direction=direction,
            strength=strength,
            ma_alignment=ma_alignment,
            adx=indicators.adx14,
            hh_ll_structure=hh_ll,
        )

    # ------------------------------------------------------------------ #
    # Private helpers
    # ------------------------------------------------------------------ #

    @staticmethod
    def _compute_ma_alignment(ma20: Decimal, ma60: Decimal) -> str:
        """MA20 vs MA60 정배열 판단.

        Returns:
            "MA20>MA60" | "MA20<MA60" | "FLAT"
        """
        if ma60 == Decimal("0"):
            return "FLAT"
        ratio = (ma20 - ma60) / ma60
        if ratio > _MA_FLAT_THRESHOLD:
            return "MA20>MA60"
        if ratio < -_MA_FLAT_THRESHOLD:
            return "MA20<MA60"
        return "FLAT"

    @staticmethod
    def _compute_adx_strength(adx: Decimal) -> Strength:
        """ADX 값으로 추세 강도를 판단한다.

        Returns:
            Strength.STRONG | Strength.MEDIUM | Strength.WEAK
        """
        if adx > _ADX_STRONG:
            return Strength.STRONG
        if adx >= _ADX_MEDIUM_LOW:
            return Strength.MEDIUM
        return Strength.WEAK

    @staticmethod
    def _compute_hh_ll(candles: list[Candle]) -> str:
        """직전 최대 20봉의 스윙 고점/저점으로 방향성을 판단한다.

        알고리즘:
          - 짝수 인덱스 봉의 고가를 연속 비교 → 모두 상승이면 HH
          - 짝수 인덱스 봉의 저가를 연속 비교 → 모두 하락이면 LL
          - 그 외 RANGING

        Returns:
            "HH" | "LL" | "RANGING"
        """
        window = candles[-_HH_LL_WINDOW:] if len(candles) > _HH_LL_WINDOW else candles
        n = len(window)

        if n < 3:
            return "RANGING"

        # 스윙 고점: 연속 비교
        highs = [c.high for c in window]
        lows = [c.low for c in window]

        # 단순 선형 비교: 전반 절반 vs 후반 절반 평균
        mid = n // 2
        avg_high_early = sum(highs[:mid]) / Decimal(str(mid))
        avg_high_late = sum(highs[mid:]) / Decimal(str(n - mid))
        avg_low_early = sum(lows[:mid]) / Decimal(str(mid))
        avg_low_late = sum(lows[mid:]) / Decimal(str(n - mid))

        hh = avg_high_late > avg_high_early
        hl = avg_low_late > avg_low_early
        lh = avg_high_late < avg_high_early
        ll = avg_low_late < avg_low_early

        if hh and hl:
            return "HH"
        if lh and ll:
            return "LL"
        return "RANGING"

    @staticmethod
    def _compute_direction(ma_alignment: str, strength: Strength) -> Direction:
        """MA 정배열 + ADX 강도로 방향성을 결정한다.

        Returns:
            Direction.UPTREND | Direction.DOWNTREND | Direction.SIDEWAYS
        """
        strong_or_medium = strength in (Strength.STRONG, Strength.MEDIUM)

        if ma_alignment == "MA20>MA60" and strong_or_medium:
            return Direction.UPTREND
        if ma_alignment == "MA20<MA60" and strong_or_medium:
            return Direction.DOWNTREND
        return Direction.SIDEWAYS
