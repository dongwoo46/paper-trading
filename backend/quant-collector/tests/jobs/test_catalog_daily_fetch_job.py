from __future__ import annotations

import dataclasses
from datetime import date, timedelta
from unittest.mock import MagicMock

import pandas as pd
import pytest

from src.catalog.models import CatalogSymbol
from src.jobs.catalog_daily_fetch_job import (
    CatalogDailyFetchJob,
    FetchResult,
    FetchWindow,
)


def _make_symbol(
    symbol: str = "AAPL",
    fetched_until_date: date | None = None,
) -> CatalogSymbol:
    return CatalogSymbol(
        symbol=symbol,
        name="Apple",
        market="US",
        enabled=True,
        is_default=True,
        fetched_until_date=fetched_until_date,
        last_collected_at=None,
    )


def _make_frame(n: int = 5) -> pd.DataFrame:
    return pd.DataFrame(
        [
            {
                "date": date(2024, 1, 1 + i % 28),
                "symbol": "AAPL",
                "open": 100.0, "high": 110.0, "low": 90.0,
                "close": 105.0, "volume": 1_000_000.0,
                "adj_close": 104.5, "source": "yfinance",
            }
            for i in range(n)
        ]
    )


def _make_job(
    yf_fetch_return: pd.DataFrame | None = None,
    pykrx_fetch_return: pd.DataFrame | None = None,
    upsert_return: int = 5,
) -> tuple[CatalogDailyFetchJob, MagicMock, MagicMock, MagicMock]:
    mock_yf = MagicMock()
    mock_yf.fetch.return_value = yf_fetch_return if yf_fetch_return is not None else _make_frame()

    mock_pykrx = MagicMock()
    mock_pykrx.fetch.return_value = pykrx_fetch_return if pykrx_fetch_return is not None else _make_frame()

    mock_repo = MagicMock()
    mock_repo.upsert_daily_rows.return_value = upsert_return

    job = CatalogDailyFetchJob(
        yfinance_collector=mock_yf,
        pykrx_collector=mock_pykrx,
        ohlcv_repository=mock_repo,
    )
    return job, mock_yf, mock_pykrx, mock_repo


class TestFetchResultSchema:
    def test_has_rows_inserted_field(self):
        field_names = {f.name for f in dataclasses.fields(FetchResult)}
        assert "rows_inserted" in field_names

    def test_has_no_output_path_field(self):
        field_names = {f.name for f in dataclasses.fields(FetchResult)}
        assert "output_path" not in field_names


class TestRunForYfinance:
    _WINDOW = FetchWindow(start_date=date(2024, 1, 1), end_date=date(2024, 1, 31))

    def test_skip_when_fetched_until_date_is_at_window_end(self):
        job, _, _, mock_repo = _make_job()
        symbol = _make_symbol(fetched_until_date=self._WINDOW.end_date)

        results = job.run_for_yfinance(symbols=[symbol], window=self._WINDOW, auto_adjust=False)

        assert len(results) == 1
        assert results[0].skipped is True
        assert results[0].success is True
        mock_repo.upsert_daily_rows.assert_not_called()

    def test_normal_collect_calls_upsert_not_save_csv(self):
        job, mock_yf, _, mock_repo = _make_job(upsert_return=5)
        symbol = _make_symbol()

        results = job.run_for_yfinance(symbols=[symbol], window=self._WINDOW, auto_adjust=False)

        assert len(results) == 1
        assert results[0].success is True
        assert results[0].rows_inserted == 5
        mock_repo.upsert_daily_rows.assert_called_once()
        mock_yf.save_csv.assert_not_called()

    def test_collector_exception_produces_failed_result(self):
        job, mock_yf, _, _ = _make_job()
        mock_yf.fetch.side_effect = RuntimeError("network error")
        symbol = _make_symbol()

        results = job.run_for_yfinance(symbols=[symbol], window=self._WINDOW, auto_adjust=False)

        assert len(results) == 1
        assert results[0].success is False
        assert "network error" in results[0].error


class TestRunForPykrx:
    _WINDOW = FetchWindow(start_date=date(2024, 1, 1), end_date=date(2024, 1, 31))

    def test_skip_when_fetched_until_date_is_at_window_end(self):
        job, _, _, mock_repo = _make_job()
        symbol = _make_symbol(fetched_until_date=self._WINDOW.end_date)

        results = job.run_for_pykrx(symbols=[symbol], window=self._WINDOW, adjusted=False)

        assert len(results) == 1
        assert results[0].skipped is True
        mock_repo.upsert_daily_rows.assert_not_called()

    def test_normal_collect_calls_upsert_not_save_csv(self):
        job, _, mock_pykrx, mock_repo = _make_job(upsert_return=10)
        symbol = _make_symbol()

        results = job.run_for_pykrx(symbols=[symbol], window=self._WINDOW, adjusted=False)

        assert len(results) == 1
        assert results[0].success is True
        assert results[0].rows_inserted == 10
        mock_repo.upsert_daily_rows.assert_called_once()
        mock_pykrx.save_csv.assert_not_called()

    def test_collector_exception_produces_failed_result(self):
        job, _, mock_pykrx, _ = _make_job()
        mock_pykrx.fetch.side_effect = ValueError("bad symbol")
        symbol = _make_symbol()

        results = job.run_for_pykrx(symbols=[symbol], window=self._WINDOW, adjusted=False)

        assert len(results) == 1
        assert results[0].success is False
        assert "bad symbol" in results[0].error
