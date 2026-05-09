from __future__ import annotations

from datetime import date

import pandas as pd

from src.collectors.trading_indicator_collectors import (
    PykrxTradingIndicatorCollector,
    YFinanceTradingIndicatorCollector,
)


def test_yfinance_relative_strength_normalize_has_expected_columns():
    raw = pd.DataFrame(
        {
            "Date": pd.to_datetime(["2026-01-02"]),
            "Close": [101.25],
            "Volume": [1000],
        }
    )
    collector = YFinanceTradingIndicatorCollector()
    frame = collector.normalize_relative_strength(raw, symbol="AAPL")
    assert list(frame.columns) == ["trade_date", "symbol", "close_price", "volume", "source"]
    assert frame.iloc[0]["trade_date"] == date(2026, 1, 2)
    assert frame.iloc[0]["source"] == "yfinance"


def test_pykrx_alternative_flow_normalize_has_expected_columns():
    raw = pd.DataFrame(
        {
            "날짜": pd.to_datetime(["2026-01-03"]),
            "개인": [1200.5],
            "기관합계": [-500.25],
            "외국인합계": [300.0],
        }
    )
    collector = PykrxTradingIndicatorCollector()
    frame = collector.normalize_alternative_flow(raw, symbol="005930")
    assert list(frame.columns) == [
        "trade_date",
        "symbol",
        "retail_flow",
        "institution_flow",
        "foreign_flow",
        "source",
    ]
    assert frame.iloc[0]["trade_date"] == date(2026, 1, 3)
    assert frame.iloc[0]["source"] == "pykrx"


def test_pykrx_metadata_normalize_has_expected_columns():
    raw = pd.DataFrame(
        {
            "symbol": ["005930"],
            "name": ["삼성전자"],
            "market": ["KOSPI"],
            "currency": ["KRW"],
        }
    )
    collector = PykrxTradingIndicatorCollector()
    frame = collector.normalize_metadata(raw)
    assert list(frame.columns) == ["symbol", "name", "market", "currency", "source"]
    assert frame.iloc[0]["source"] == "pykrx"
