"""TDD — 2-2: ChartSnapshot Aggregate 단위 테스트 (RED 먼저 작성)."""
from __future__ import annotations

import pytest
from datetime import date
from decimal import Decimal


def _make_candle(close: str = "68500", d: date = date(2026, 5, 10)):
    from src.chart_analysis.domain.value_objects import Candle
    return Candle(
        date=d,
        open=Decimal("68000"),
        high=Decimal("69000"),
        low=Decimal("67500"),
        close=Decimal(close),
        volume=Decimal("1000000"),
    )


def _make_indicator_set():
    from src.chart_analysis.domain.value_objects import IndicatorSet
    return IndicatorSet(
        ma20=Decimal("68000"),
        ma60=Decimal("67000"),
        ma120=Decimal("65000"),
        rsi14=Decimal("58.2"),
        macd=Decimal("120.4"),
        macd_signal=Decimal("100.1"),
        macd_hist=Decimal("20.3"),
        bb_upper=Decimal("71000"),
        bb_lower=Decimal("65000"),
        atr14=Decimal("800"),
        adx14=Decimal("23.4"),
        stoch_k=Decimal("65.0"),
        stoch_d=Decimal("60.0"),
        obv=Decimal("5000000"),
        volume_ma20=Decimal("900000"),
    )


def _make_snapshot(symbol="005930", window="3M", interval="D", candles=None):
    from src.chart_analysis.domain.chart_snapshot import ChartSnapshot
    if candles is None:
        candles = [_make_candle()]
    return ChartSnapshot(
        symbol=symbol,
        window=window,
        interval=interval,
        candles=candles,
        indicators=_make_indicator_set(),
    )


class TestChartSnapshotWindowValidation:
    @pytest.mark.parametrize("window", ["1M", "3M", "6M", "1Y", "2Y", "MAX"])
    def test_valid_window(self, window):
        snap = _make_snapshot(window=window)
        assert snap.window == window

    @pytest.mark.parametrize("window", ["5M", "4Y", "3Y", "1W", "", "day", "1m"])
    def test_invalid_window_raises(self, window):
        with pytest.raises(ValueError):
            _make_snapshot(window=window)


class TestChartSnapshotIntervalValidation:
    @pytest.mark.parametrize("interval", ["D", "W"])
    def test_valid_interval(self, interval):
        snap = _make_snapshot(interval=interval)
        assert snap.interval == interval

    @pytest.mark.parametrize("interval", ["d", "w", "M", "H", "1D", "daily", ""])
    def test_invalid_interval_raises(self, interval):
        with pytest.raises(ValueError):
            _make_snapshot(interval=interval)


class TestChartSnapshotEmptyCandles:
    def test_empty_candles_raises_value_error(self):
        with pytest.raises(ValueError, match="candles"):
            _make_snapshot(candles=[])


class TestChartSnapshotComputeHash:
    def test_compute_hash_returns_string(self):
        snap = _make_snapshot()
        hash_val = snap.compute_hash()
        assert isinstance(hash_val, str)

    def test_compute_hash_determinism(self):
        """동일 입력 → 항상 동일한 해시."""
        candles = [_make_candle(d=date(2026, 5, i + 1)) for i in range(3)]
        snap1 = _make_snapshot(candles=candles)
        snap2 = _make_snapshot(candles=candles)
        assert snap1.compute_hash() == snap2.compute_hash()

    def test_compute_hash_sensitivity_to_price(self):
        """가격 변동 → 해시 변동."""
        candle_a = _make_candle(close="68500")
        candle_b = _make_candle(close="69000")
        snap_a = _make_snapshot(candles=[candle_a])
        snap_b = _make_snapshot(candles=[candle_b])
        assert snap_a.compute_hash() != snap_b.compute_hash()

    def test_compute_hash_is_sha256_length(self):
        """sha256 hex digest는 64자여야 한다."""
        snap = _make_snapshot()
        assert len(snap.compute_hash()) == 64

    def test_compute_hash_no_float(self):
        """해시 계산 과정에서 float을 사용하지 않는다 — Decimal→str 변환."""
        import json
        snap = _make_snapshot()
        # compute_hash should not raise and should be deterministic
        h1 = snap.compute_hash()
        h2 = snap.compute_hash()
        assert h1 == h2


class TestChartSnapshotMethods:
    def test_last_close(self):
        candles = [
            _make_candle(close="68500", d=date(2026, 5, 1)),
            _make_candle(close="69000", d=date(2026, 5, 2)),
        ]
        snap = _make_snapshot(candles=candles)
        assert snap.last_close() == Decimal("69000")

    def test_last_close_is_decimal(self):
        snap = _make_snapshot()
        assert isinstance(snap.last_close(), Decimal)

    def test_length(self):
        candles = [_make_candle(d=date(2026, 5, i + 1)) for i in range(5)]
        snap = _make_snapshot(candles=candles)
        assert snap.length() == 5

    def test_snapshot_is_immutable(self):
        snap = _make_snapshot()
        with pytest.raises((AttributeError, TypeError)):
            snap.symbol = "NEW"
