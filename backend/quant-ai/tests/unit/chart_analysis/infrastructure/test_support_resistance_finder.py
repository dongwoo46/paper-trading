"""Support/resistance quality acceptance tests."""
from __future__ import annotations

import os
import sys
from datetime import date, timedelta
from decimal import Decimal

import pytest

_THIS_DIR = os.path.dirname(__file__)
sys.path.insert(0, os.path.join(_THIS_DIR, "..", "..", "..", ".."))
sys.path.insert(0, os.path.join(_THIS_DIR, "..", "..", "..", "..", "src"))

from chart_analysis.domain.value_objects import Candle, LevelSet
from tests.fixtures.chart_analysis.loader import candles_from_fixture, load_fixture


FIXTURE_CASES = (
    "sample_uptrend_3m.json",
    "sample_sideways_6m.json",
    "sample_monotonic_uptrend_3m.json",
    "sample_role_flip_6m.json",
    "sample_long_weekly_2y.json",
    "sample_max_like_history.json",
)


def _atr(value: str) -> Decimal:
    return Decimal(value)


def _make_trending_candles(count: int = 40) -> list[Candle]:
    candles: list[Candle] = []
    start = Decimal("100")
    for i in range(count):
        close = start + Decimal(i)
        candles.append(
            Candle(
                date=date(2024, 1, 1) + timedelta(days=i),
                open=close - Decimal("0.2"),
                high=close + Decimal("1"),
                low=close - Decimal("1"),
                close=close,
                volume=Decimal("1000000"),
            )
        )
    return candles


def _find_from_fixture(finder, fixture_name: str) -> tuple[dict, list[Candle], LevelSet]:
    fixture = load_fixture(f"support_resistance/{fixture_name}")
    candles = candles_from_fixture(fixture)
    result = finder.find(
        candles,
        _atr(fixture["atr"]),
        window=fixture["window"],
        interval=fixture["interval"],
        last_close=candles[-1].close,
    )
    return fixture, candles, result


def _zone_tolerance(expected: Decimal, atr: Decimal) -> Decimal:
    percent_band = (expected.copy_abs() * Decimal("0.02")).quantize(Decimal("0.0001"))
    atr_band = atr * Decimal("1.5")
    return max(percent_band, atr_band)


def _within_zone(level: Decimal, expected: Decimal, atr: Decimal) -> bool:
    return abs(level - expected) <= _zone_tolerance(expected, atr)


def _assert_zone_in_top_k(levels: tuple[Decimal, ...], expected: str, atr: Decimal, top_k: int) -> None:
    expected_decimal = Decimal(expected)
    top_levels = levels[:top_k]
    assert any(_within_zone(level, expected_decimal, atr) for level in top_levels), (
        f"expected zone {expected_decimal} within top {top_k}, got {top_levels} "
        f"with tolerance {_zone_tolerance(expected_decimal, atr)}"
    )


def _assert_ranked_fixture_zones(result: LevelSet, fixture: dict) -> None:
    atr = _atr(fixture["atr"])
    expected = fixture["expected"]
    for zone in expected.get("top_k_support_zones", []):
        _assert_zone_in_top_k(result.supports, zone["price"], atr, int(zone["top_k"]))
    for zone in expected.get("top_k_resistance_zones", []):
        _assert_zone_in_top_k(result.resistances, zone["price"], atr, int(zone["top_k"]))


def _assert_trade_plan_eligible_levels_are_on_correct_side(result: LevelSet, fixture: dict) -> None:
    expected = fixture["expected"].get("correct_side_of_close")
    if expected is None:
        return

    close = Decimal(expected["close"])
    supports = [level for level in result.supports if level < close]
    resistances = [level for level in result.resistances if level > close]

    if expected.get("support_below"):
        assert supports, f"expected at least one support below close {close}, got {result.supports}"
        assert max(supports) < close
    if expected.get("resistance_above"):
        assert resistances, f"expected at least one resistance above close {close}, got {result.resistances}"
        assert min(resistances) > close


def _scored_index(finder, side: str, expected: str, atr: Decimal) -> int:
    expected_decimal = Decimal(expected)
    for index, level in enumerate(level for level in finder.last_scored_levels if level.side == side):
        if _within_zone(level.price, expected_decimal, atr):
            return index
    raise AssertionError(f"expected scored {side} zone {expected_decimal} not found")


class TestScipyPeakSupportResistanceFinder:
    """ScipyPeakSupportResistanceFinder unit and acceptance tests."""

    @pytest.fixture
    def finder(self):
        from chart_analysis.infrastructure.support_resistance_finder import ScipyPeakSupportResistanceFinder

        return ScipyPeakSupportResistanceFinder()

    def test_empty_candles_returns_empty_level_set(self, finder):
        result = finder.find([], _atr("1000"))
        assert isinstance(result, LevelSet)
        assert result.supports == ()
        assert result.resistances == ()

    @pytest.mark.parametrize("fixture_name", FIXTURE_CASES)
    def test_curated_fixtures_return_public_decimal_levelset(self, finder, fixture_name):
        _, _, result = _find_from_fixture(finder, fixture_name)

        assert isinstance(result, LevelSet)
        assert isinstance(result.supports, tuple)
        assert isinstance(result.resistances, tuple)
        assert all(isinstance(level, Decimal) for level in result.supports)
        assert all(isinstance(level, Decimal) for level in result.resistances)

    @pytest.mark.parametrize("fixture_name", FIXTURE_CASES)
    def test_curated_fixtures_rank_expected_zones_in_top_k(self, finder, fixture_name):
        fixture, _, result = _find_from_fixture(finder, fixture_name)

        _assert_ranked_fixture_zones(result, fixture)

    @pytest.mark.parametrize("fixture_name", FIXTURE_CASES)
    def test_curated_fixtures_have_trade_plan_eligible_correct_side_levels(self, finder, fixture_name):
        fixture, _, result = _find_from_fixture(finder, fixture_name)

        _assert_trade_plan_eligible_levels_are_on_correct_side(result, fixture)

    def test_sideways_fixture_levels_remain_within_price_range(self, finder):
        _, candles, result = _find_from_fixture(finder, "sample_sideways_6m.json")
        min_price = min(c.low for c in candles)
        max_price = max(c.high for c in candles)

        for level in result.supports:
            assert min_price <= level <= max_price, f"support {level} out of range"
        for level in result.resistances:
            assert min_price <= level <= max_price, f"resistance {level} out of range"

    def test_max_five_levels_each_for_default_policy(self, finder):
        fixture = load_fixture("support_resistance/sample_uptrend_3m.json")
        candles = candles_from_fixture(fixture)
        result = finder.find(candles, _atr(fixture["atr"]))

        assert len(result.supports) <= 5
        assert len(result.resistances) <= 5

    def test_single_candle_returns_empty(self, finder):
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

    def test_window_specific_policy_is_explicit(self):
        from chart_analysis.infrastructure.support_resistance_finder import policy_for_window

        daily_3m = policy_for_window("3M", "D")
        weekly_max = policy_for_window("MAX", "W")

        assert daily_3m != weekly_max
        assert daily_3m.peak_distance < weekly_max.peak_distance
        assert daily_3m.recency_decay < weekly_max.recency_decay

    def test_monotonic_uptrend_uses_recent_endpoint_levels(self, finder):
        candles = _make_trending_candles()
        result = finder.find(
            candles,
            _atr("2"),
            window="3M",
            interval="D",
            last_close=candles[-1].close,
        )

        assert result.resistances
        assert result.supports
        assert result.resistances[0] >= candles[-1].close
        assert result.supports[0] < candles[-1].close
        assert any(level.endpoint for level in finder.last_scored_levels)

    def test_internal_scored_levels_do_not_change_levelset_shape(self, finder):
        candles = _make_trending_candles()
        result = finder.find(
            candles,
            _atr("2"),
            window="3M",
            interval="D",
            last_close=candles[-1].close,
        )

        assert isinstance(result, LevelSet)
        assert isinstance(result.supports, tuple)
        assert isinstance(result.resistances, tuple)
        assert finder.last_scored_levels
        assert all(isinstance(level.price, Decimal) for level in finder.last_scored_levels)

    def test_max_like_history_recent_resistance_outranks_stale_old_resistance(self, finder):
        fixture, _, result = _find_from_fixture(finder, "sample_max_like_history.json")
        atr = _atr(fixture["atr"])
        expected = fixture["expected"]
        close = Decimal(expected["correct_side_of_close"]["close"])

        recent_index = _scored_index(finder, "resistance", expected["recent_resistance_zone"], atr)
        stale_index = _scored_index(finder, "resistance", expected["stale_resistance_zone"], atr)

        assert recent_index < stale_index
        assert result.resistances[0] > close
