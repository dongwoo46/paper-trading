from datetime import datetime
from decimal import Decimal
from unittest.mock import MagicMock, patch

import pandas as pd

from src.catalog.postgres_symbol_catalog import DbConfig
from src.repositories.indicator_source_repositories import (
    AlternativeFlowSourceRepository,
    MetadataSourceRepository,
    MicrostructureSourceRepository,
    RelativeStrengthSourceRepository,
    SessionOhlcvSourceRepository,
)

CONFIG = DbConfig(host="localhost", port=5432, database="paper", user="paper", password="paper")


def _mock_db():
    cur = MagicMock()
    cur.__enter__ = MagicMock(return_value=cur)
    cur.__exit__ = MagicMock(return_value=False)
    conn = MagicMock()
    conn.__enter__ = MagicMock(return_value=conn)
    conn.__exit__ = MagicMock(return_value=False)
    conn.cursor.return_value = cur
    return conn, cur


def _frame():
    return pd.DataFrame([
        {"as_of": datetime(2026, 1, 2), "symbol": "AAPL", "source": "yfinance", "bid": "10.1", "ask": "10.4", "bid_size": "100", "ask_size": "110", "spread": "0.3", "bid_ask_available": True, "depth_available": True, "availability_note": None, "session": "regular", "open": "10", "high": "11", "low": "9", "close": "10.5", "volume": "1000", "benchmark_symbol": "SPY", "symbol_return": "0.01", "benchmark_return": "0.005", "relative_strength": "2", "short_interest": "100", "days_to_cover": "3", "shares_outstanding": "1000", "float_shares": "800", "exchange": "NASDAQ", "currency": "USD", "timezone": "America/New_York", "market_cap": "1000000", "free_float": "900000"}
    ])


def test_microstructure_repository_uses_decimal_and_upsert():
    repo = MicrostructureSourceRepository(CONFIG)
    conn, cur = _mock_db()
    with patch("src.repositories.indicator_source_repositories.connect", return_value=conn):
        inserted = repo.upsert(_frame())
    assert inserted == 1
    row = cur.executemany.call_args[0][1][0]
    assert isinstance(row[3], Decimal)
    assert row[8] == True


def test_session_repository_empty_frame_returns_zero():
    repo = SessionOhlcvSourceRepository(CONFIG)
    assert repo.upsert(pd.DataFrame()) == 0


def test_relative_strength_repository_upserts():
    repo = RelativeStrengthSourceRepository(CONFIG)
    conn, cur = _mock_db()
    with patch("src.repositories.indicator_source_repositories.connect", return_value=conn):
        repo.upsert(_frame())
    assert "ON CONFLICT" in cur.executemany.call_args[0][0]


def test_relative_strength_repository_allows_nullable_relative_strength():
    repo = RelativeStrengthSourceRepository(CONFIG)
    conn, cur = _mock_db()
    f = _frame()
    f.loc[0, "relative_strength"] = None
    with patch("src.repositories.indicator_source_repositories.connect", return_value=conn):
        repo.upsert(f)
    row = cur.executemany.call_args[0][1][0]
    assert row[6] is None


def test_alternative_flow_repository_nullable_decimal():
    repo = AlternativeFlowSourceRepository(CONFIG)
    conn, cur = _mock_db()
    f = _frame()
    f.loc[0, "short_interest"] = None
    with patch("src.repositories.indicator_source_repositories.connect", return_value=conn):
        repo.upsert(f)
    row = cur.executemany.call_args[0][1][0]
    assert row[3] is None


def test_metadata_repository_upserts_market_cap_decimal():
    repo = MetadataSourceRepository(CONFIG)
    conn, cur = _mock_db()
    with patch("src.repositories.indicator_source_repositories.connect", return_value=conn):
        repo.upsert(_frame())
    row = cur.executemany.call_args[0][1][0]
    assert isinstance(row[7], Decimal)
