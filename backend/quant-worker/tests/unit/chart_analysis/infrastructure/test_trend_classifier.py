"""TDD — TrendClassifier 단위 테스트.

Red 단계: MaAdxTrendClassifier 구현 전 작성.
3개 fixture (uptrend / downtrend / sideways) 입력 → expected Direction/Strength.
"""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

from datetime import date
from decimal import Decimal

import pytest

from chart_analysis.domain.value_objects import (
    Candle, Direction, IndicatorSet, Strength, TrendAnalysis,
)
from tests.fixtures.chart_analysis.loader import load_fixture, indicator_set_from_fixture


def _make_candles_with_hh(n: int = 20) -> list[Candle]:
    """상승 추세 HH 구조 캔들 n개 — 각 고점/저점이 이전보다 높음."""
    candles = []
    for i in range(n):
        price = 50000 + i * 300
        candles.append(
            Candle(
                date=date(2024, 1, 1),
                open=Decimal(str(price)),
                high=Decimal(str(price + 400)),
                low=Decimal(str(price - 100)),
                close=Decimal(str(price + 200)),
                volume=Decimal("100000"),
            )
        )
    return candles


def _make_candles_with_ll(n: int = 20) -> list[Candle]:
    """하락 추세 LL 구조 캔들 n개 — 각 고점/저점이 이전보다 낮음."""
    candles = []
    for i in range(n):
        price = 70000 - i * 300
        candles.append(
            Candle(
                date=date(2024, 1, 1),
                open=Decimal(str(price)),
                high=Decimal(str(price + 100)),
                low=Decimal(str(price - 400)),
                close=Decimal(str(price - 200)),
                volume=Decimal("100000"),
            )
        )
    return candles


def _make_sideways_candles(n: int = 20) -> list[Candle]:
    """횡보 캔들 n개 — 홀수/짝수 봉이 교대로 고가/저가 반복 (RANGING 보장)."""
    candles = []
    for i in range(n):
        # 홀수 봉: 고가 쪽, 짝수 봉: 저가 쪽 → 평균적으로 변화 없음
        price = 53200 if i % 2 == 0 else 52800
        candles.append(
            Candle(
                date=date(2024, 1, 1),
                open=Decimal(str(price)),
                high=Decimal(str(price + 300)),
                low=Decimal(str(price - 300)),
                close=Decimal(str(price + 50)),
                volume=Decimal("90000"),
            )
        )
    return candles


class TestMaAdxTrendClassifier:
    """MaAdxTrendClassifier 단위 테스트."""

    @pytest.fixture
    def classifier(self):
        from chart_analysis.infrastructure.trend_classifier import MaAdxTrendClassifier
        return MaAdxTrendClassifier()

    # ------------------------------------------------------------------ #
    # 1. 상승 추세 fixture
    # ------------------------------------------------------------------ #

    def test_uptrend_direction(self, classifier):
        """uptrend_ma_aligned fixture → Direction.UPTREND."""
        fixture = load_fixture("trend/uptrend_ma_aligned.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_candles_with_hh(25)
        result = classifier.classify(candles, indicators)

        assert isinstance(result, TrendAnalysis)
        assert result.direction == Direction.UPTREND, f"Expected UPTREND, got {result.direction}"

    def test_uptrend_strength_strong(self, classifier):
        """uptrend fixture ADX=30 → Strength.STRONG."""
        fixture = load_fixture("trend/uptrend_ma_aligned.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_candles_with_hh(25)
        result = classifier.classify(candles, indicators)

        assert result.strength == Strength.STRONG, f"Expected STRONG, got {result.strength}"

    def test_uptrend_ma_alignment(self, classifier):
        """uptrend fixture → ma_alignment 'MA20>MA60'."""
        fixture = load_fixture("trend/uptrend_ma_aligned.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_candles_with_hh(25)
        result = classifier.classify(candles, indicators)

        assert result.ma_alignment == "MA20>MA60"

    def test_uptrend_hh_structure(self, classifier):
        """HH 구조 캔들 → hh_ll_structure 'HH'."""
        fixture = load_fixture("trend/uptrend_ma_aligned.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_candles_with_hh(25)
        result = classifier.classify(candles, indicators)

        assert result.hh_ll_structure == "HH"

    # ------------------------------------------------------------------ #
    # 2. 하락 추세 fixture
    # ------------------------------------------------------------------ #

    def test_downtrend_direction(self, classifier):
        """downtrend_ma_inverted fixture → Direction.DOWNTREND."""
        fixture = load_fixture("trend/downtrend_ma_inverted.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_candles_with_ll(25)
        result = classifier.classify(candles, indicators)

        assert result.direction == Direction.DOWNTREND, f"Expected DOWNTREND, got {result.direction}"

    def test_downtrend_strength_strong(self, classifier):
        """downtrend fixture ADX=28 → Strength.STRONG."""
        fixture = load_fixture("trend/downtrend_ma_inverted.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_candles_with_ll(25)
        result = classifier.classify(candles, indicators)

        assert result.strength == Strength.STRONG

    def test_downtrend_ma_alignment(self, classifier):
        """downtrend fixture → ma_alignment 'MA20<MA60'."""
        fixture = load_fixture("trend/downtrend_ma_inverted.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_candles_with_ll(25)
        result = classifier.classify(candles, indicators)

        assert result.ma_alignment == "MA20<MA60"

    def test_downtrend_ll_structure(self, classifier):
        """LL 구조 캔들 → hh_ll_structure 'LL'."""
        fixture = load_fixture("trend/downtrend_ma_inverted.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_candles_with_ll(25)
        result = classifier.classify(candles, indicators)

        assert result.hh_ll_structure == "LL"

    # ------------------------------------------------------------------ #
    # 3. 횡보 fixture
    # ------------------------------------------------------------------ #

    def test_sideways_direction(self, classifier):
        """sideways_low_adx fixture → Direction.SIDEWAYS."""
        fixture = load_fixture("trend/sideways_low_adx.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_sideways_candles(25)
        result = classifier.classify(candles, indicators)

        assert result.direction == Direction.SIDEWAYS, f"Expected SIDEWAYS, got {result.direction}"

    def test_sideways_strength_weak(self, classifier):
        """sideways fixture ADX=15 → Strength.WEAK."""
        fixture = load_fixture("trend/sideways_low_adx.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_sideways_candles(25)
        result = classifier.classify(candles, indicators)

        assert result.strength == Strength.WEAK, f"Expected WEAK, got {result.strength}"

    def test_sideways_ma_flat(self, classifier):
        """sideways fixture MA20≈MA60 (±0.5%) → ma_alignment 'FLAT'."""
        fixture = load_fixture("trend/sideways_low_adx.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_sideways_candles(25)
        result = classifier.classify(candles, indicators)

        assert result.ma_alignment == "FLAT"

    def test_sideways_ranging_structure(self, classifier):
        """횡보 캔들 → hh_ll_structure 'RANGING'."""
        fixture = load_fixture("trend/sideways_low_adx.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_sideways_candles(25)
        result = classifier.classify(candles, indicators)

        assert result.hh_ll_structure == "RANGING"

    # ------------------------------------------------------------------ #
    # 4. 반환 타입 검증
    # ------------------------------------------------------------------ #

    def test_adx_is_decimal(self, classifier):
        """TrendAnalysis.adx는 Decimal."""
        fixture = load_fixture("trend/uptrend_ma_aligned.json")
        indicators = indicator_set_from_fixture(fixture)
        candles = _make_candles_with_hh(25)
        result = classifier.classify(candles, indicators)

        assert isinstance(result.adx, Decimal)
