from __future__ import annotations

import inspect
from datetime import date, datetime
from decimal import Decimal
from unittest.mock import MagicMock, patch

import pandas as pd
import pytest

from src.catalog.postgres_symbol_catalog import DbConfig
from src.repositories.market_daily_ohlcv_repository import (
    MarketDailyOhlcvRepository,
    OhlcvUpsertContext,
)

_CONFIG = DbConfig(host="localhost", port=5432, database="test", user="test", password="test")
_CONTEXT = OhlcvUpsertContext(source="yfinance", symbol="AAPL", market="US", provider="yfinance")


def _make_frame(n: int) -> pd.DataFrame:
    return pd.DataFrame(
        [
            {
                "date": date(2024, 1, 1),
                "open": 100.0 + i,
                "high": 110.0 + i,
                "low": 90.0 + i,
                "close": 105.0 + i,
                "volume": float(1_000_000 + i),
                "adj_close": 105.0 + i,
            }
            for i in range(n)
        ]
    )


def _make_mock_db():
    mock_cursor = MagicMock()
    mock_cursor.__enter__ = MagicMock(return_value=mock_cursor)
    mock_cursor.__exit__ = MagicMock(return_value=False)

    mock_conn = MagicMock()
    mock_conn.__enter__ = MagicMock(return_value=mock_conn)
    mock_conn.__exit__ = MagicMock(return_value=False)
    mock_conn.cursor.return_value = mock_cursor

    return mock_conn, mock_cursor


class TestUpsertDailyRowsEmpty:
    def test_empty_frame_returns_zero_without_db_call(self):
        repo = MarketDailyOhlcvRepository(_CONFIG)
        with patch("src.repositories.market_daily_ohlcv_repository.connect") as mock_connect:
            result = repo.upsert_daily_rows(pd.DataFrame(), _CONTEXT)
        assert result == 0
        mock_connect.assert_not_called()


class TestUpsertDailyRowsChunking:
    def test_default_chunk_size_is_500(self):
        sig = inspect.signature(MarketDailyOhlcvRepository.upsert_daily_rows)
        assert sig.parameters["chunk_size"].default == 500

    def test_1200_rows_splits_into_three_chunks(self):
        repo = MarketDailyOhlcvRepository(_CONFIG)
        frame = _make_frame(1200)
        mock_conn, mock_cursor = _make_mock_db()

        with patch("src.repositories.market_daily_ohlcv_repository.connect", return_value=mock_conn):
            result = repo.upsert_daily_rows(frame, _CONTEXT, chunk_size=500)

        assert result == 1200
        assert mock_cursor.executemany.call_count == 3
        calls = mock_cursor.executemany.call_args_list
        assert len(calls[0][0][1]) == 500
        assert len(calls[1][0][1]) == 500
        assert len(calls[2][0][1]) == 200

    def test_exactly_500_rows_is_one_chunk(self):
        repo = MarketDailyOhlcvRepository(_CONFIG)
        frame = _make_frame(500)
        mock_conn, mock_cursor = _make_mock_db()

        with patch("src.repositories.market_daily_ohlcv_repository.connect", return_value=mock_conn):
            result = repo.upsert_daily_rows(frame, _CONTEXT, chunk_size=500)

        assert result == 500
        assert mock_cursor.executemany.call_count == 1

    def test_single_connection_reused_across_chunks(self):
        repo = MarketDailyOhlcvRepository(_CONFIG)
        frame = _make_frame(1200)
        mock_conn, mock_cursor = _make_mock_db()

        with patch("src.repositories.market_daily_ohlcv_repository.connect", return_value=mock_conn) as mock_connect:
            repo.upsert_daily_rows(frame, _CONTEXT, chunk_size=500)

        mock_connect.assert_called_once()


class TestToRow:
    def test_adj_close_none_yields_none(self):
        repo = MarketDailyOhlcvRepository(_CONFIG)
        item = {
            "date": date(2024, 1, 1),
            "open": 100.0, "high": 110.0, "low": 90.0,
            "close": 105.0, "volume": 1_000_000.0, "adj_close": None,
        }
        row = repo._to_row(item, _CONTEXT, datetime.now())
        assert row[9] is None

    def test_adj_close_nan_yields_none(self):
        repo = MarketDailyOhlcvRepository(_CONFIG)
        item = {
            "date": date(2024, 1, 1),
            "open": 100.0, "high": 110.0, "low": 90.0,
            "close": 105.0, "volume": 1_000_000.0, "adj_close": float("nan"),
        }
        row = repo._to_row(item, _CONTEXT, datetime.now())
        assert row[9] is None

    def test_normal_row_has_correct_structure(self):
        repo = MarketDailyOhlcvRepository(_CONFIG)
        item = {
            "date": date(2024, 1, 2),
            "open": 100.0, "high": 110.0, "low": 90.0,
            "close": 105.0, "volume": 1_000_000.0, "adj_close": 104.5,
        }
        row = repo._to_row(item, _CONTEXT, datetime.now())
        assert len(row) == 14
        assert row[0] == "yfinance"
        assert row[1] == "AAPL"
        assert row[2] == "US"
        assert isinstance(row[4], Decimal)  # open_price
        assert isinstance(row[9], Decimal)  # adj_close_price
