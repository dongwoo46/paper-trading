"""RuleBasedPatternDetector — 6종 캔들 패턴 룰 기반 탐지기.

패턴 룰 (모두 Decimal 연산):
  - Doji: |close-open| / (high-low) < 0.1
  - Hammer: 하락 추세 후, lower_shadow > 2×body, upper_shadow < 0.3×body
  - Inverted Hammer: upper_shadow > 2×body, lower_shadow < 0.3×body
  - Bullish Engulfing: 이전 음봉, 현재 양봉이 이전 봉 완전히 포괄
  - Bearish Engulfing: 이전 양봉, 현재 음봉이 이전 봉 완전히 포괄
  - Morning Star: 큰 음봉 + 작은 몸통 + 큰 양봉 (첫 봉 중간 이상)
  - Evening Star: 큰 양봉 + 작은 몸통 + 큰 음봉 (첫 봉 중간 이하)

금융 안전: 모든 수치 비교는 Decimal로 수행한다.
"""
from __future__ import annotations

from decimal import Decimal

from src.chart_analysis.domain.value_objects import Candle, CandlePattern, PatternType

# 패턴 판단 임계값 (Decimal 상수)
_DOJI_RATIO = Decimal("0.1")       # |close-open| / (high-low)
_SHADOW_BODY_RATIO = Decimal("2")   # 꼬리 길이 / 몸통 비율 기준
_UPPER_LIMIT_RATIO = Decimal("0.3") # 상단 꼬리 허용 비율 (hammer)
_ZERO = Decimal("0")
_HALF = Decimal("0.5")
# 작은 몸통 판단 비율 (Morning/Evening Star 중간 봉)
_SMALL_BODY_RATIO = Decimal("0.3")
# 큰 몸통 판단 비율 (Morning/Evening Star 첫/셋째 봉)
_LARGE_BODY_RATIO = Decimal("0.6")


def _body(c: Candle) -> Decimal:
    return abs(c.close - c.open)


def _full_range(c: Candle) -> Decimal:
    return c.high - c.low


def _upper_shadow(c: Candle) -> Decimal:
    return c.high - max(c.open, c.close)


def _lower_shadow(c: Candle) -> Decimal:
    return min(c.open, c.close) - c.low


def _is_bullish(c: Candle) -> bool:
    return c.close > c.open


def _is_bearish(c: Candle) -> bool:
    return c.close < c.open


class RuleBasedPatternDetector:
    """룰 기반 캔들 패턴 탐지기.

    PatternDetector 포트를 구현한다.
    """

    def detect(self, candles: list[Candle]) -> list[CandlePattern]:
        """봉 목록에서 캔들 패턴 목록을 반환한다.

        Args:
            candles: 시간 순으로 정렬된 Candle 리스트.

        Returns:
            탐지된 CandlePattern 리스트 (index는 candles 내 위치).
        """
        patterns: list[CandlePattern] = []
        n = len(candles)

        for i in range(n):
            c = candles[i]
            prev = candles[i - 1] if i > 0 else None
            prev2 = candles[i - 2] if i > 1 else None

            if self._is_doji(c):
                patterns.append(CandlePattern(type=PatternType.DOJI, index=i, date=c.date))

            if prev is not None and self._is_hammer(c, candles, i):
                patterns.append(CandlePattern(type=PatternType.HAMMER, index=i, date=c.date))

            if prev is not None and self._is_inverted_hammer(c, candles, i):
                patterns.append(CandlePattern(type=PatternType.INVERTED_HAMMER, index=i, date=c.date))

            if prev is not None and self._is_bullish_engulfing(c, prev):
                patterns.append(CandlePattern(type=PatternType.BULLISH_ENGULFING, index=i, date=c.date))

            if prev is not None and self._is_bearish_engulfing(c, prev):
                patterns.append(CandlePattern(type=PatternType.BEARISH_ENGULFING, index=i, date=c.date))

            if prev is not None and prev2 is not None:
                star = self._is_morning_or_evening_star(prev2, prev, c)
                if star == "morning":
                    patterns.append(CandlePattern(type=PatternType.MORNING_STAR, index=i, date=c.date))
                elif star == "evening":
                    patterns.append(CandlePattern(type=PatternType.EVENING_STAR, index=i, date=c.date))

        return patterns

    # ------------------------------------------------------------------ #
    # Private pattern rules
    # ------------------------------------------------------------------ #

    def _is_doji(self, c: Candle) -> bool:
        """도지: |close-open| / (high-low) < 0.1"""
        rng = _full_range(c)
        if rng == _ZERO:
            return False
        return _body(c) / rng < _DOJI_RATIO

    def _is_hammer(self, c: Candle, candles: list[Candle], i: int) -> bool:
        """망치: 하락 추세 후 긴 아래 꼬리, 짧은 위 꼬리, 작은 몸통.

        조건:
          - lower_shadow > 2 × body
          - upper_shadow < 0.3 × body (위 꼬리 거의 없음)  [완화: ≤ body]
          - 최소 2봉 하락 추세 선행
        """
        body = _body(c)
        if body == _ZERO:
            return False
        lower = _lower_shadow(c)
        upper = _upper_shadow(c)

        if lower < _SHADOW_BODY_RATIO * body:
            return False
        if upper > body:  # 위 꼬리가 몸통보다 작아야 함
            return False

        # 선행 하락 추세: 직전 2봉 이상이 하락 (close < open)
        prior = candles[:i]
        if len(prior) < 2:
            return False
        return all(_is_bearish(p) for p in prior[-2:])

    def _is_inverted_hammer(self, c: Candle, candles: list[Candle], i: int) -> bool:
        """역망치: 긴 위 꼬리, 짧은 아래 꼬리, 작은 몸통.

        조건:
          - upper_shadow > 2 × body
          - lower_shadow ≤ body
        """
        body = _body(c)
        if body == _ZERO:
            return False
        upper = _upper_shadow(c)
        lower = _lower_shadow(c)

        if upper < _SHADOW_BODY_RATIO * body:
            return False
        if lower > body:
            return False

        # 선행 하락 추세
        prior = candles[:i]
        if len(prior) < 2:
            return False
        return all(_is_bearish(p) for p in prior[-2:])

    def _is_bullish_engulfing(self, curr: Candle, prev: Candle) -> bool:
        """강세 엔걸핑: 이전 음봉, 현재 양봉이 이전 봉 완전히 감싼다.

        조건:
          - prev: 음봉 (close < open)
          - curr: 양봉 (close > open)
          - curr.open < prev.close (현재 시가 < 이전 종가)
          - curr.close > prev.open (현재 종가 > 이전 시가)
        """
        if not _is_bearish(prev):
            return False
        if not _is_bullish(curr):
            return False
        return curr.open < prev.close and curr.close > prev.open

    def _is_bearish_engulfing(self, curr: Candle, prev: Candle) -> bool:
        """약세 엔걸핑: 이전 양봉, 현재 음봉이 이전 봉 완전히 감싼다.

        조건:
          - prev: 양봉 (close > open)
          - curr: 음봉 (close < open)
          - curr.open > prev.close (현재 시가 > 이전 종가)
          - curr.close < prev.open (현재 종가 < 이전 시가)
        """
        if not _is_bullish(prev):
            return False
        if not _is_bearish(curr):
            return False
        return curr.open > prev.close and curr.close < prev.open

    def _is_morning_or_evening_star(
        self, first: Candle, middle: Candle, third: Candle
    ) -> str:
        """모닝/이브닝 스타 판별.

        Returns:
            "morning" | "evening" | "" (해당 없음)
        """
        first_body = _body(first)
        middle_body = _body(middle)
        third_body = _body(third)

        if first_body == _ZERO or third_body == _ZERO:
            return ""

        # 중간 봉은 작은 몸통
        avg_body = (first_body + third_body) / Decimal("2")
        if middle_body > avg_body * _SMALL_BODY_RATIO:
            return ""

        # Morning Star: 첫봉 음봉 + 셋째봉 양봉 + 셋째봉이 첫봉 중점 이상
        if _is_bearish(first) and _is_bullish(third):
            midpoint = (first.open + first.close) / Decimal("2")
            if third.close > midpoint:
                return "morning"

        # Evening Star: 첫봉 양봉 + 셋째봉 음봉 + 셋째봉이 첫봉 중점 이하
        if _is_bullish(first) and _is_bearish(third):
            midpoint = (first.open + first.close) / Decimal("2")
            if third.close < midpoint:
                return "evening"

        return ""
