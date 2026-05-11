"""TDD — PatternDetector 단위 테스트.

Red 단계: RuleBasedPatternDetector 구현 전 작성.
각 golden fixture 입력 → 해당 패턴이 정확히 검출됨.
"""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

from datetime import date
from decimal import Decimal

import pytest

from chart_analysis.domain.value_objects import Candle, CandlePattern, PatternType
from tests.fixtures.chart_analysis.loader import load_fixture, candles_from_fixture


def _detected_types(patterns: list[CandlePattern]) -> set[str]:
    return {p.type.value for p in patterns}


class TestRuleBasedPatternDetector:
    """RuleBasedPatternDetector 단위 테스트."""

    @pytest.fixture
    def detector(self):
        from chart_analysis.infrastructure.pattern_detector import RuleBasedPatternDetector
        return RuleBasedPatternDetector()

    # ------------------------------------------------------------------ #
    # 1. 도지 (Doji)
    # ------------------------------------------------------------------ #

    def test_doji_detected(self, detector):
        """doji_01 fixture → DOJI 패턴 검출."""
        fixture = load_fixture("patterns/doji_01.json")
        candles = candles_from_fixture(fixture)
        result = detector.detect(candles)
        assert PatternType.DOJI in {p.type for p in result}, f"DOJI not detected, got: {_detected_types(result)}"

    def test_doji_returns_candle_pattern_type(self, detector):
        """패턴 결과가 CandlePattern 타입."""
        fixture = load_fixture("patterns/doji_01.json")
        candles = candles_from_fixture(fixture)
        result = detector.detect(candles)
        for p in result:
            assert isinstance(p, CandlePattern)

    # ------------------------------------------------------------------ #
    # 2. 망치 (Hammer)
    # ------------------------------------------------------------------ #

    def test_hammer_detected(self, detector):
        """hammer_01 fixture → HAMMER 패턴 검출."""
        fixture = load_fixture("patterns/hammer_01.json")
        candles = candles_from_fixture(fixture)
        result = detector.detect(candles)
        assert PatternType.HAMMER in {p.type for p in result}, f"HAMMER not detected, got: {_detected_types(result)}"

    # ------------------------------------------------------------------ #
    # 3. 역망치 (Inverted Hammer)
    # ------------------------------------------------------------------ #

    def test_inverted_hammer_detected(self, detector):
        """inverted_hammer_01 fixture → INVERTED_HAMMER 패턴 검출."""
        fixture = load_fixture("patterns/inverted_hammer_01.json")
        candles = candles_from_fixture(fixture)
        result = detector.detect(candles)
        assert PatternType.INVERTED_HAMMER in {p.type for p in result}, (
            f"INVERTED_HAMMER not detected, got: {_detected_types(result)}"
        )

    # ------------------------------------------------------------------ #
    # 4. 강세 엔걸핑 (Bullish Engulfing)
    # ------------------------------------------------------------------ #

    def test_bullish_engulfing_detected(self, detector):
        """engulfing_bull_01 fixture → BULLISH_ENGULFING 패턴 검출."""
        fixture = load_fixture("patterns/engulfing_bull_01.json")
        candles = candles_from_fixture(fixture)
        result = detector.detect(candles)
        assert PatternType.BULLISH_ENGULFING in {p.type for p in result}, (
            f"BULLISH_ENGULFING not detected, got: {_detected_types(result)}"
        )

    # ------------------------------------------------------------------ #
    # 5. 약세 엔걸핑 (Bearish Engulfing)
    # ------------------------------------------------------------------ #

    def test_bearish_engulfing_detected(self, detector):
        """engulfing_bear_01 fixture → BEARISH_ENGULFING 패턴 검출."""
        fixture = load_fixture("patterns/engulfing_bear_01.json")
        candles = candles_from_fixture(fixture)
        result = detector.detect(candles)
        assert PatternType.BEARISH_ENGULFING in {p.type for p in result}, (
            f"BEARISH_ENGULFING not detected, got: {_detected_types(result)}"
        )

    # ------------------------------------------------------------------ #
    # 6. 모닝 스타 (Morning Star)
    # ------------------------------------------------------------------ #

    def test_morning_star_detected(self, detector):
        """morning_star_01 fixture → MORNING_STAR 패턴 검출."""
        fixture = load_fixture("patterns/morning_star_01.json")
        candles = candles_from_fixture(fixture)
        result = detector.detect(candles)
        assert PatternType.MORNING_STAR in {p.type for p in result}, (
            f"MORNING_STAR not detected, got: {_detected_types(result)}"
        )

    # ------------------------------------------------------------------ #
    # 7. 이브닝 스타 (Evening Star)
    # ------------------------------------------------------------------ #

    def test_evening_star_detected(self, detector):
        """evening_star_01 fixture → EVENING_STAR 패턴 검출."""
        fixture = load_fixture("patterns/evening_star_01.json")
        candles = candles_from_fixture(fixture)
        result = detector.detect(candles)
        assert PatternType.EVENING_STAR in {p.type for p in result}, (
            f"EVENING_STAR not detected, got: {_detected_types(result)}"
        )

    # ------------------------------------------------------------------ #
    # 8. 빈 입력 → 빈 리스트
    # ------------------------------------------------------------------ #

    def test_empty_candles_returns_empty(self, detector):
        """빈 캔들 입력 → 빈 패턴 리스트."""
        result = detector.detect([])
        assert result == []

    # ------------------------------------------------------------------ #
    # 9. 비대상 패턴 케이스 — 강세 엔걸핑 캔들로 망치 미검출
    # ------------------------------------------------------------------ #

    def test_engulfing_bull_does_not_detect_hammer(self, detector):
        """engulfing_bull_01 fixture에서 HAMMER는 검출되지 않는다."""
        fixture = load_fixture("patterns/engulfing_bull_01.json")
        candles = candles_from_fixture(fixture)
        result = detector.detect(candles)
        assert PatternType.HAMMER not in {p.type for p in result}

    # ------------------------------------------------------------------ #
    # 10. 패턴 index/date 필드 존재
    # ------------------------------------------------------------------ #

    def test_pattern_has_index_and_date(self, detector):
        """검출된 패턴에 index와 date 필드가 있다."""
        fixture = load_fixture("patterns/doji_01.json")
        candles = candles_from_fixture(fixture)
        result = detector.detect(candles)
        if result:
            p = result[0]
            assert isinstance(p.index, int)
            assert p.date is not None
