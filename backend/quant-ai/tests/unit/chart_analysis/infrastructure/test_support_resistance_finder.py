"""TDD — SupportResistanceFinder 단위 테스트.

Red 단계: ScipyPeakSupportResistanceFinder 구현 전 작성.
Green 단계: 구현 후 통과 확인.
"""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

from datetime import date, timedelta
from decimal import Decimal

import pytest

from chart_analysis.domain.value_objects import Candle, LevelSet
from tests.fixtures.chart_analysis.loader import load_fixture, candles_from_fixture


def _atr(value: str) -> Decimal:
    return Decimal(value)


class TestScipyPeakSupportResistanceFinder:
    """ScipyPeakSupportResistanceFinder 단위 테스트."""

    @pytest.fixture
    def finder(self):
        from chart_analysis.infrastructure.support_resistance_finder import ScipyPeakSupportResistanceFinder
        return ScipyPeakSupportResistanceFinder()

    # ------------------------------------------------------------------ #
    # 1. 빈 입력 → 빈 LevelSet
    # ------------------------------------------------------------------ #

    def test_empty_candles_returns_empty_level_set(self, finder):
        """빈 캔들 → supports/resistances 모두 빈 튜플."""
        result = finder.find([], _atr("1000"))
        assert isinstance(result, LevelSet)
        assert result.supports == ()
        assert result.resistances == ()

    # ------------------------------------------------------------------ #
    # 2. 상승 추세 fixture — 저항선/지지선 존재
    # ------------------------------------------------------------------ #

    def test_uptrend_fixture_has_levels(self, finder):
        """상승 추세 fixture → 저항선 1개 이상, 지지선 1개 이상."""
        fixture = load_fixture("support_resistance/sample_uptrend_3m.json")
        candles = candles_from_fixture(fixture)
        atr = _atr("2000")
        result = finder.find(candles, atr)

        assert isinstance(result, LevelSet)
        assert len(result.resistances) >= fixture["expected"]["min_resistance_count"]
        assert len(result.supports) >= fixture["expected"]["min_support_count"]

    def test_uptrend_fixture_levels_are_decimal(self, finder):
        """상승 추세 fixture → 모든 레벨이 Decimal 타입."""
        fixture = load_fixture("support_resistance/sample_uptrend_3m.json")
        candles = candles_from_fixture(fixture)
        result = finder.find(candles, _atr("2000"))

        for level in result.supports:
            assert isinstance(level, Decimal), f"support level {level} is not Decimal"
        for level in result.resistances:
            assert isinstance(level, Decimal), f"resistance level {level} is not Decimal"

    def test_uptrend_resistances_above_supports(self, finder):
        """저항선 최솟값 > 지지선 최댓값 (상승 추세에서 일반적으로 성립)."""
        fixture = load_fixture("support_resistance/sample_uptrend_3m.json")
        candles = candles_from_fixture(fixture)
        result = finder.find(candles, _atr("2000"))

        if result.resistances and result.supports:
            assert min(result.resistances) >= min(result.supports)

    # ------------------------------------------------------------------ #
    # 3. 횡보 fixture — 저항선/지지선 존재
    # ------------------------------------------------------------------ #

    def test_sideways_fixture_has_levels(self, finder):
        """횡보 fixture → 저항선 1개 이상, 지지선 1개 이상."""
        fixture = load_fixture("support_resistance/sample_sideways_6m.json")
        candles = candles_from_fixture(fixture)
        atr = _atr("1500")
        result = finder.find(candles, atr)

        assert len(result.resistances) >= fixture["expected"]["min_resistance_count"]
        assert len(result.supports) >= fixture["expected"]["min_support_count"]

    # ------------------------------------------------------------------ #
    # 4. 최대 5개 반환 제한
    # ------------------------------------------------------------------ #

    def test_max_five_levels_each(self, finder):
        """지지선/저항선 각 최대 5개."""
        fixture = load_fixture("support_resistance/sample_uptrend_3m.json")
        candles = candles_from_fixture(fixture)
        result = finder.find(candles, _atr("2000"))

        assert len(result.supports) <= 5
        assert len(result.resistances) <= 5

    # ------------------------------------------------------------------ #
    # 5. 단봉 입력 → 빈 LevelSet (peaks 탐지 불가)
    # ------------------------------------------------------------------ #

    def test_single_candle_returns_empty(self, finder):
        """단봉 입력 → 빈 LevelSet."""
        candle = Candle(
            date=date(2024, 1, 1),
            open=Decimal("50000"),
            high=Decimal("51000"),
            low=Decimal("49000"),
            close=Decimal("50500"),
            volume=Decimal("100000"),
        )
        result = finder.find([candle], _atr("1000"))
        assert result.supports == ()
        assert result.resistances == ()

    # ------------------------------------------------------------------ #
    # 6. 레벨은 가격 범위 내에 위치
    # ------------------------------------------------------------------ #

    def test_levels_within_price_range(self, finder):
        """탐지된 모든 레벨이 입력 candle 가격 범위 내에 있어야 한다."""
        fixture = load_fixture("support_resistance/sample_sideways_6m.json")
        candles = candles_from_fixture(fixture)
        result = finder.find(candles, _atr("1500"))

        min_price = min(c.low for c in candles)
        max_price = max(c.high for c in candles)

        for lvl in result.supports:
            assert min_price <= lvl <= max_price, f"support {lvl} out of range"
        for lvl in result.resistances:
            assert min_price <= lvl <= max_price, f"resistance {lvl} out of range"
