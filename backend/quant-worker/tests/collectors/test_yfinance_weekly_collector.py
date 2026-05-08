from __future__ import annotations

from datetime import date
from unittest.mock import patch

import pandas as pd
import pytest

from src.collectors.yfinance_weekly_collector import WeeklyCollectRequest, YFinanceWeeklyCollector


def test_fetch_uses_1wk_interval_and_normalizes():
    raw = pd.DataFrame(
        {
            "Open": [100.0],
            "High": [110.0],
            "Low": [90.0],
            "Close": [105.0],
            "Adj Close": [104.0],
            "Volume": [1000.0],
        },
        index=pd.to_datetime(["2024-01-05"]),
    )
    raw.index.name = "Date"

    collector = YFinanceWeeklyCollector()
    request = WeeklyCollectRequest(symbol="AAPL", start_date=date(2024, 1, 1), end_date=date(2024, 1, 31))

    with patch("src.collectors.yfinance_weekly_collector.yf.download", return_value=raw) as mock_download:
        frame = collector.fetch(request)

    assert list(frame.columns) == YFinanceWeeklyCollector.OUTPUT_COLUMNS
    assert frame.iloc[0]["date"] == date(2024, 1, 5)
    assert frame.iloc[0]["source"] == "yfinance"
    assert frame.iloc[0]["symbol"] == "AAPL"
    assert mock_download.call_args.kwargs["interval"] == "1wk"


def test_fetch_rejects_invalid_window():
    collector = YFinanceWeeklyCollector()
    request = WeeklyCollectRequest(symbol="AAPL", start_date=date(2024, 2, 1), end_date=date(2024, 1, 1))
    with pytest.raises(ValueError):
        collector.fetch(request)
