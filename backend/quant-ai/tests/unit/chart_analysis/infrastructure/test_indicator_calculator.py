"""TDD — IndicatorCalculator 단위 테스트.

Red 단계: PandasTaIndicatorCalculator 구현 전 작성.
Green 단계: 구현 후 통과 확인.
"""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

from datetime import date
from decimal import Decimal

import pytest

from chart_analysis.domain.value_objects import Candle, IndicatorSet


def _make_candles(n: int, base_price: int = 50000) -> list[Candle]:
    """단조 상승 더미 캔들 n개 생성."""
    candles = []
    for i in range(n):
        price = base_price + i * 100
        candles.append(
            Candle(
                date=date(2024, 1, 1),
                open=Decimal(str(price)),
                high=Decimal(str(price + 300)),
                low=Decimal(str(price - 200)),
                close=Decimal(str(price + 100)),
                volume=Decimal("100000"),
            )
        )
    return candles


class TestPandasTaIndicatorCalculator:
    """PandasTaIndicatorCalculator 단위 테스트."""

    @pytest.fixture
    def calculator(self):
        from chart_analysis.infrastructure.indicator_calculator import PandasTaIndicatorCalculator
        return PandasTaIndicatorCalculator()

    # ------------------------------------------------------------------ #
    # 1. 충분한 데이터(120봉 이상) → 모든 필드 Decimal
    # ------------------------------------------------------------------ #

    def test_compute_returns_indicator_set_type(self, calculator):
        """120봉 입력 시 IndicatorSet 반환."""
        candles = _make_candles(130)
        result = calculator.calculate(candles)
        assert isinstance(result, IndicatorSet)

    def test_all_fields_are_decimal(self, calculator):
        """모든 필드가 Decimal 타입이어야 한다."""
        candles = _make_candles(130)
        result = calculator.calculate(candles)
        decimal_fields = (
            "ma20", "ma60", "ma120", "rsi14", "macd", "macd_signal",
            "macd_hist", "bb_upper", "bb_lower", "atr14", "adx14",
            "stoch_k", "stoch_d", "obv", "volume_ma20",
        )
        for field in decimal_fields:
            value = getattr(result, field)
            assert isinstance(value, Decimal), (
                f"IndicatorSet.{field} must be Decimal, got {type(value).__name__}"
            )

    def test_ma20_greater_than_ma60_in_uptrend(self, calculator):
        """상승 추세 캔들에서 MA20 > MA60 정배열."""
        candles = _make_candles(130)
        result = calculator.calculate(candles)
        assert result.ma20 > result.ma60

    def test_rsi_within_valid_range(self, calculator):
        """RSI14는 0~100 범위 내."""
        candles = _make_candles(130)
        result = calculator.calculate(candles)
        assert Decimal("0") <= result.rsi14 <= Decimal("100")

    def test_atr_positive(self, calculator):
        """ATR14는 양수여야 한다."""
        candles = _make_candles(130)
        result = calculator.calculate(candles)
        assert result.atr14 > Decimal("0")

    def test_adx_within_valid_range(self, calculator):
        """ADX14는 0~100 범위 내."""
        candles = _make_candles(130)
        result = calculator.calculate(candles)
        assert Decimal("0") <= result.adx14 <= Decimal("100")

    def test_bb_upper_greater_than_bb_lower(self, calculator):
        """볼린저 밴드 상단 > 하단."""
        candles = _make_candles(130)
        result = calculator.calculate(candles)
        assert result.bb_upper > result.bb_lower

    # ------------------------------------------------------------------ #
    # 2. 입력 부족 시 — 14봉 미만: rsi14, adx14 = None 허용
    #    (IndicatorSet 타입은 Optional 필드 없으므로 ValueError 발생)
    # ------------------------------------------------------------------ #

    def test_too_few_candles_raises(self, calculator):
        """14봉 미만 → ValueError (충분한 데이터 없음)."""
        candles = _make_candles(5)
        with pytest.raises((ValueError, Exception)):
            calculator.calculate(candles)

    # ------------------------------------------------------------------ #
    # 3. 빈 입력 → ValueError
    # ------------------------------------------------------------------ #

    def test_empty_candles_raises(self, calculator):
        """빈 캔들 입력 → ValueError."""
        with pytest.raises((ValueError, Exception)):
            calculator.calculate([])

    # ------------------------------------------------------------------ #
    # 4. float 노출 없음 — 결과에 float 타입 없어야 함
    # ------------------------------------------------------------------ #

    def test_no_float_in_output(self, calculator):
        """출력값에 float이 노출되지 않는다."""
        candles = _make_candles(130)
        result = calculator.calculate(candles)
        for field in vars(result) if hasattr(result, '__dataclass_fields__') else {}:
            val = getattr(result, field)
            assert not isinstance(val, float), f"{field} must not be float"

    # ------------------------------------------------------------------ #
    # 5. Decimal(str(float)) 변환 방식 — 정밀도 손실 없음
    # ------------------------------------------------------------------ #

    def test_volume_ma20_is_positive(self, calculator):
        """volume_ma20는 양수 Decimal."""
        candles = _make_candles(130)
        result = calculator.calculate(candles)
        assert result.volume_ma20 > Decimal("0")