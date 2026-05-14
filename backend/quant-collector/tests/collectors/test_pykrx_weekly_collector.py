from __future__ import annotations

from datetime import date
from unittest.mock import patch

import pandas as pd
import pytest

from src.collectors.pykrx_weekly_collector import PykrxWeeklyCollector, WeeklyCollectRequest


def test_fetch_uses_weekly_freq_and_normalizes():
    raw = pd.DataFrame(
        {
            "시가": [100.0],
            "고가": [110.0],
            "저가": [90.0],
            "종가": [105.0],
            "거래량": [1000.0],
        },
        index=pd.to_datetime(["2024-01-05"]),
    )
    raw.index.name = "날짜"

    collector = PykrxWeeklyCollector()
    request = WeeklyCollectRequest(symbol="005930", start_date=date(2024, 1, 1), end_date=date(2024, 1, 31))

    with patch("src.collectors.pykrx_weekly_collector.stock.get_market_ohlcv_by_date", return_value=raw) as mock_api:
        frame = collector.fetch(request)

    assert list(frame.columns) == PykrxWeeklyCollector.OUTPUT_COLUMNS
    assert frame.iloc[0]["date"] == date(2024, 1, 5)
    assert frame.iloc[0]["source"] == "pykrx"
    assert frame.iloc[0]["symbol"] == "005930"
    assert frame.iloc[0]["adj_close"] == frame.iloc[0]["close"]
    assert mock_api.call_args.kwargs["freq"] == "w"


def test_fetch_rejects_invalid_window():
    collector = PykrxWeeklyCollector()
    request = WeeklyCollectRequest(symbol="005930", start_date=date(2024, 2, 1), end_date=date(2024, 1, 1))
    with pytest.raises(ValueError):
        collector.fetch(request)

