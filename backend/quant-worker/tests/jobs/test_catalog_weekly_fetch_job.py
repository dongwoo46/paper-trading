from __future__ import annotations

from datetime import date
from unittest.mock import MagicMock

import pandas as pd

from src.catalog.models import CatalogSymbol
from src.jobs.catalog_weekly_fetch_job import CatalogWeeklyFetchJob, FetchWindow


def _symbol(fetched_until_date: date | None = None) -> CatalogSymbol:
    return CatalogSymbol(
        symbol="AAPL",
        name="Apple",
        market="US",
        enabled=True,
        is_default=True,
        fetched_until_date=fetched_until_date,
        last_collected_at=None,
    )


def _frame() -> pd.DataFrame:
    return pd.DataFrame(
        [
            {
                "date": date(2024, 1, 5),
                "symbol": "AAPL",
                "open": 100.0,
                "high": 110.0,
                "low": 90.0,
                "close": 105.0,
                "volume": 1000.0,
                "adj_close": 104.0,
                "source": "yfinance",
            }
        ]
    )


def test_run_for_yfinance_skips_when_window_already_collected():
    collector = MagicMock()
    repository = MagicMock()
    job = CatalogWeeklyFetchJob(yfinance_collector=collector, ohlcv_repository=repository)
    window = FetchWindow(start_date=date(2024, 1, 1), end_date=date(2024, 1, 31))

    results = job.run_for_yfinance([_symbol(fetched_until_date=date(2024, 1, 31))], window, auto_adjust=False)

    assert len(results) == 1
    assert results[0].skipped is True
    repository.upsert_weekly_rows.assert_not_called()


def test_run_for_yfinance_collects_and_upserts():
    collector = MagicMock()
    collector.fetch.return_value = _frame()
    repository = MagicMock()
    repository.upsert_weekly_rows.return_value = 1
    job = CatalogWeeklyFetchJob(yfinance_collector=collector, ohlcv_repository=repository)
    window = FetchWindow(start_date=date(2024, 1, 1), end_date=date(2024, 1, 31))

    results = job.run_for_yfinance([_symbol()], window, auto_adjust=True)

    assert results[0].success is True
    assert results[0].rows_inserted == 1
    repository.upsert_weekly_rows.assert_called_once()

