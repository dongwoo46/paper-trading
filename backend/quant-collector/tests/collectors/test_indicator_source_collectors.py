from datetime import datetime
from decimal import Decimal

import pandas as pd
import pytest

from src.collectors.indicator_source_collectors import (
    AlternativeFlowCollector,
    MetadataCollector,
    MicrostructureCollector,
    RelativeStrengthCollector,
    SessionOhlcvCollector,
)


def test_microstructure_collector_normalizes_kr_payload():
    collector = MicrostructureCollector()
    frame = pd.DataFrame([
        {"timestamp": "2026-01-02T09:01:00+09:00", "symbol": "005930", "bid": "70100.1", "ask": "70100.9", "bid_size": "100", "ask_size": "120", "spread": "0.8", "source": "pykrx"}
    ])
    out = collector.normalize_kr(frame)
    assert list(out.columns) == collector.OUTPUT_COLUMNS
    assert out.iloc[0]["as_of"].isoformat().startswith("2026-01-02T00:01:00+00:00")
    assert out.iloc[0]["bid_ask_available"] == True
    assert out.iloc[0]["depth_available"] == True


def test_session_ohlcv_collector_sets_session_and_source_for_us_payload():
    collector = SessionOhlcvCollector()
    frame = pd.DataFrame([
        {"timestamp": "2026-01-02T14:30:00Z", "symbol": "AAPL", "open": "100", "high": "102", "low": "99", "close": "101", "volume": "12345", "session": "regular"}
    ])
    out = collector.normalize_us(frame, source="yfinance")
    assert list(out.columns) == collector.OUTPUT_COLUMNS
    assert out.iloc[0]["session"] == "regular"
    assert out.iloc[0]["source"] == "yfinance"
    assert out.iloc[0]["as_of"].tzinfo is not None


def test_session_ohlcv_collector_normalizes_session_aliases():
    collector = SessionOhlcvCollector()
    frame = pd.DataFrame([
        {"timestamp": "2026-01-02T14:30:00Z", "symbol": "AAPL", "open": "100", "high": "102", "low": "99", "close": "101", "volume": "12345", "session": "PRE_MARKET"},
        {"timestamp": "2026-01-02T20:30:00Z", "symbol": "AAPL", "open": "101", "high": "103", "low": "100", "close": "102", "volume": "10000", "session": "post"},
    ])
    out = collector.normalize_us(frame, source="yfinance")
    assert out.iloc[0]["session"] == "pre"
    assert out.iloc[1]["session"] == "after"


def test_session_ohlcv_collector_rejects_invalid_session_alias():
    collector = SessionOhlcvCollector()
    frame = pd.DataFrame([
        {"timestamp": "2026-01-02T14:30:00Z", "symbol": "AAPL", "open": "100", "high": "102", "low": "99", "close": "101", "volume": "12345", "session": "lunch"}
    ])
    with pytest.raises(ValueError, match="unsupported session alias"):
        collector.normalize_us(frame, source="yfinance")


def test_relative_strength_collector_defaults_benchmark_when_missing():
    collector = RelativeStrengthCollector()
    frame = pd.DataFrame([
        {"as_of": datetime(2026, 1, 2), "symbol": "AAPL", "symbol_return": "0.01", "benchmark_return": "0.005"}
    ])
    out = collector.normalize(frame, source="yfinance")
    assert out.iloc[0]["benchmark_symbol"] == "SPY"


def test_relative_strength_collector_uses_decimal_safe_precision():
    collector = RelativeStrengthCollector()
    frame = pd.DataFrame([
        {"as_of": datetime(2026, 1, 2), "symbol": "AAPL", "symbol_return": "0.1", "benchmark_return": "0.3"}
    ])
    out = collector.normalize(frame, source="yfinance")
    assert out.iloc[0]["relative_strength"] == (Decimal("0.1") / Decimal("0.3"))


def test_relative_strength_collector_returns_null_on_zero_or_invalid_benchmark():
    collector = RelativeStrengthCollector()
    frame = pd.DataFrame([
        {"as_of": datetime(2026, 1, 2), "symbol": "AAPL", "symbol_return": "0.01", "benchmark_return": "0"},
        {"as_of": datetime(2026, 1, 3), "symbol": "MSFT", "symbol_return": "0.01", "benchmark_return": "NaN"},
    ])
    out = collector.normalize(frame, source="yfinance")
    assert pd.isna(out.iloc[0]["relative_strength"])
    assert pd.isna(out.iloc[1]["relative_strength"])


def test_alternative_flow_collector_keeps_nullable_fields():
    collector = AlternativeFlowCollector()
    frame = pd.DataFrame([
        {"as_of": "2026-01-02", "symbol": "AAPL", "short_interest": None, "days_to_cover": "1.5", "shares_outstanding": "1000", "float_shares": "800"}
    ])
    out = collector.normalize(frame, source="yfinance")
    assert pd.isna(out.iloc[0]["short_interest"])


def test_metadata_collector_normalizes_timestamps_and_market_cap():
    collector = MetadataCollector()
    frame = pd.DataFrame([
        {"as_of": "2026-01-02T00:00:00Z", "symbol": "AAPL", "exchange": "NASDAQ", "currency": "USD", "timezone": "America/New_York", "market_cap": "1000000", "free_float": "900000"}
    ])
    out = collector.normalize(frame, source="yfinance")
    assert out.iloc[0]["exchange"] == "NASDAQ"
    assert out.iloc[0]["source"] == "yfinance"
