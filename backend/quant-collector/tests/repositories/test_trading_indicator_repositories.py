from __future__ import annotations

from datetime import date
from decimal import Decimal
from unittest.mock import MagicMock, patch

import pandas as pd

from src.catalog.postgres_symbol_catalog import DbConfig
from src.repositories.market_alternative_flow_repository import MarketAlternativeFlowRepository
from src.repositories.market_metadata_repository import MarketMetadataRepository
from src.repositories.market_microstructure_repository import MarketMicrostructureRepository
from src.repositories.market_relative_strength_repository import MarketRelativeStrengthRepository
from src.repositories.market_session_ohlcv_repository import MarketSessionOhlcvRepository


def _mock_db():
    mock_cursor = MagicMock()
    mock_cursor.__enter__ = MagicMock(return_value=mock_cursor)
    mock_cursor.__exit__ = MagicMock(return_value=False)
    mock_conn = MagicMock()
    mock_conn.__enter__ = MagicMock(return_value=mock_conn)
    mock_conn.__exit__ = MagicMock(return_value=False)
    mock_conn.cursor.return_value = mock_cursor
    return mock_conn, mock_cursor


_CONFIG = DbConfig(host="localhost", port=5432, database="test", user="test", password="test")


def test_microstructure_repository_uses_decimal_and_upsert_key():
    repo = MarketMicrostructureRepository(_CONFIG)
    frame = pd.DataFrame([{"trade_date": date(2026, 1, 1), "symbol": "AAPL", "spread_bps": 1.23, "source": "yfinance"}])
    conn, cur = _mock_db()
    with patch("src.repositories._indicator_base.connect", return_value=conn):
        assert repo.upsert_rows(frame, provider="yfinance") == 1
    params = cur.executemany.call_args[0][1][0]
    assert isinstance(params[3], Decimal)
    assert "ON CONFLICT (source, symbol, trade_date)" in cur.executemany.call_args[0][0]


def test_session_ohlcv_repository_uses_decimal():
    repo = MarketSessionOhlcvRepository(_CONFIG)
    frame = pd.DataFrame(
        [{"trade_date": date(2026, 1, 1), "symbol": "AAPL", "open_price": 10.1, "high_price": 11.1, "low_price": 9.9, "close_price": 10.5, "volume": 1000, "source": "yfinance"}]
    )
    conn, cur = _mock_db()
    with patch("src.repositories._indicator_base.connect", return_value=conn):
        repo.upsert_rows(frame, provider="yfinance")
    params = cur.executemany.call_args[0][1][0]
    assert isinstance(params[3], Decimal)
    assert isinstance(params[7], Decimal)


def test_relative_strength_repository_uses_idempotent_key():
    repo = MarketRelativeStrengthRepository(_CONFIG)
    frame = pd.DataFrame([{"trade_date": date(2026, 1, 1), "symbol": "AAPL", "close_price": 100.1, "volume": 1000, "source": "yfinance"}])
    conn, cur = _mock_db()
    with patch("src.repositories._indicator_base.connect", return_value=conn):
        repo.upsert_rows(frame, provider="yfinance")
    assert "ON CONFLICT (source, symbol, trade_date)" in cur.executemany.call_args[0][0]


def test_alternative_flow_repository_uses_decimal():
    repo = MarketAlternativeFlowRepository(_CONFIG)
    frame = pd.DataFrame(
        [{"trade_date": date(2026, 1, 1), "symbol": "005930", "retail_flow": 100.0, "institution_flow": -50.5, "foreign_flow": 10.25, "source": "pykrx"}]
    )
    conn, cur = _mock_db()
    with patch("src.repositories._indicator_base.connect", return_value=conn):
        repo.upsert_rows(frame, provider="pykrx")
    params = cur.executemany.call_args[0][1][0]
    assert isinstance(params[3], Decimal)
    assert isinstance(params[5], Decimal)


def test_metadata_repository_upsert():
    repo = MarketMetadataRepository(_CONFIG)
    frame = pd.DataFrame([{"symbol": "AAPL", "name": "Apple", "market": "US", "currency": "USD", "source": "yfinance"}])
    conn, cur = _mock_db()
    with patch("src.repositories._indicator_base.connect", return_value=conn):
        assert repo.upsert_rows(frame, provider="yfinance") == 1
    assert "ON CONFLICT (source, symbol)" in cur.executemany.call_args[0][0]
