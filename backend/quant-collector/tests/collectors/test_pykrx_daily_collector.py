from __future__ import annotations

from datetime import date
from unittest.mock import patch

import pandas as pd
import pytest

from src.collectors.pykrx_daily_collector import DailyCollectRequest, PykrxDailyCollector


def test_fetch_normalizes_korean_columns():
    raw = pd.DataFrame(
        {
            "시가": [100.0],
            "고가": [110.0],
            "저가": [90.0],
            "종가": [105.0],
            "거래량": [1000.0],
        },
        index=pd.to_datetime(["2024-01-02"]),
    )
    raw.index.name = "날짜"

    collector = PykrxDailyCollector()
    request = DailyCollectRequest(symbol="005930", start_date=date(2024, 1, 1), end_date=date(2024, 1, 31))

    with patch("src.collectors.pykrx_daily_collector.stock.get_market_ohlcv_by_date", return_value=raw):
        frame = collector.fetch(request)

    assert list(frame.columns) == PykrxDailyCollector.OUTPUT_COLUMNS
    assert frame.iloc[0]["date"] == date(2024, 1, 2)
    assert frame.iloc[0]["source"] == "pykrx"
    assert frame.iloc[0]["symbol"] == "005930"
    assert frame.iloc[0]["adj_close"] == frame.iloc[0]["close"]


def test_fetch_normalizes_english_columns_and_numeric_strings():
    raw = pd.DataFrame(
        {
            "Open": ["100,000"],
            "High": ["110,000"],
            "Low": ["90,000"],
            "Close": ["105,000"],
            "Volume": ["1,234,567"],
        },
        index=pd.to_datetime(["2024-01-02"]),
    )
    raw.index.name = "Date"

    collector = PykrxDailyCollector()
    request = DailyCollectRequest(symbol="005930", start_date=date(2024, 1, 1), end_date=date(2024, 1, 31))

    with patch("src.collectors.pykrx_daily_collector.stock.get_market_ohlcv_by_date", return_value=raw):
        frame = collector.fetch(request)

    assert len(frame) == 1
    assert float(frame.iloc[0]["open"]) == 100000.0
    assert float(frame.iloc[0]["volume"]) == 1234567.0


def test_fetch_rejects_invalid_window():
    collector = PykrxDailyCollector()
    request = DailyCollectRequest(symbol="005930", start_date=date(2024, 2, 1), end_date=date(2024, 1, 1))
    with pytest.raises(ValueError):
        collector.fetch(request)
