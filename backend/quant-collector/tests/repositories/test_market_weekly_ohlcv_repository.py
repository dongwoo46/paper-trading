from __future__ import annotations

from datetime import date, datetime
from decimal import Decimal
from unittest.mock import MagicMock, patch

import pandas as pd

from src.catalog.postgres_symbol_catalog import DbConfig
from src.repositories.market_weekly_ohlcv_repository import (
    MarketWeeklyOhlcvRepository,
    OhlcvUpsertContext,
)


def test_upsert_empty_returns_zero_without_db_call():
    repo = MarketWeeklyOhlcvRepository(DbConfig("localhost", 5432, "test", "test", "test"))
    with patch("src.repositories.market_weekly_ohlcv_repository.connect") as mock_connect:
        result = repo.upsert_weekly_rows(pd.DataFrame(), OhlcvUpsertContext("yfinance", "AAPL", "US", "yfinance"))
    assert result == 0
    mock_connect.assert_not_called()


def test_to_row_uses_decimal_only():
    repo = MarketWeeklyOhlcvRepository(DbConfig("localhost", 5432, "test", "test", "test"))
    item = {
        "date": date(2024, 1, 5),
        "open": 100.1,
        "high": 110.2,
        "low": 90.3,
        "close": 105.4,
        "volume": 1000.0,
        "adj_close": 104.9,
    }
    row = repo._to_row(item, OhlcvUpsertContext("yfinance", "AAPL", "US", "yfinance"), datetime.now())
    assert isinstance(row[4], Decimal)
    assert isinstance(row[8], Decimal)
    assert isinstance(row[9], Decimal)


def test_upsert_query_targets_weekly_table():
    repo = MarketWeeklyOhlcvRepository(DbConfig("localhost", 5432, "test", "test", "test"))
    query = repo._upsert_query()
    assert "INSERT INTO market_weekly_ohlcv" in query
    assert "ON CONFLICT (source, symbol, trade_date) DO UPDATE SET" in query
    assert "updated_at = CURRENT_TIMESTAMP" in query
