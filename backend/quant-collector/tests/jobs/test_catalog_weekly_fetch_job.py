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
    pykrx_collector = MagicMock()
    repository = MagicMock()
    repository.find_max_trade_dates.return_value = {"AAPL": date(2024, 1, 31)}
    job = CatalogWeeklyFetchJob(
        yfinance_collector=collector,
        pykrx_collector=pykrx_collector,
        ohlcv_repository=repository,
    )
    window = FetchWindow(start_date=date(2024, 1, 1), end_date=date(2024, 1, 31))

    results = job.run_for_yfinance([_symbol()], window, auto_adjust=False)

    assert len(results) == 1
    assert results[0].skipped is True
    repository.upsert_weekly_rows.assert_not_called()


def test_run_for_yfinance_collects_and_upserts():
    collector = MagicMock()
    pykrx_collector = MagicMock()
    collector.fetch.return_value = _frame()
    repository = MagicMock()
    repository.find_max_trade_dates.return_value = {}
    repository.upsert_weekly_rows.return_value = 1
    job = CatalogWeeklyFetchJob(
        yfinance_collector=collector,
        pykrx_collector=pykrx_collector,
        ohlcv_repository=repository,
    )
    window = FetchWindow(start_date=date(2024, 1, 1), end_date=date(2024, 1, 31))

    results = job.run_for_yfinance([_symbol()], window, auto_adjust=True)

    assert results[0].success is True
    assert results[0].rows_inserted == 1
    repository.upsert_weekly_rows.assert_called_once()


def test_run_for_pykrx_collects_and_upserts():
    yfinance_collector = MagicMock()
    pykrx_collector = MagicMock()
    pykrx_collector.fetch.return_value = _frame().assign(source="pykrx", symbol="005930")
    repository = MagicMock()
    repository.find_max_trade_dates.return_value = {}
    repository.upsert_weekly_rows.return_value = 1
    job = CatalogWeeklyFetchJob(
        yfinance_collector=yfinance_collector,
        pykrx_collector=pykrx_collector,
        ohlcv_repository=repository,
    )
    window = FetchWindow(start_date=date(2024, 1, 1), end_date=date(2024, 1, 31))
    symbol = CatalogSymbol(
        symbol="005930",
        name="삼성전자",
        market="KOSPI",
        enabled=True,
        is_default=True,
        fetched_until_date=None,
        last_collected_at=None,
    )

    results = job.run_for_pykrx([symbol], window, adjusted=True)

    assert results[0].success is True
    assert results[0].rows_inserted == 1
    assert results[0].provider == "pykrx"
    repository.upsert_weekly_rows.assert_called_once()


def test_run_for_pykrx_uses_weekly_table_date_not_catalog_date():
    """일봉이 catalog fetched_until_date를 업데이트해도 주봉 job은 weekly 테이블 실제 날짜를 기준으로 삼아야 한다."""
    yfinance_collector = MagicMock()
    pykrx_collector = MagicMock()
    pykrx_collector.fetch.return_value = _frame().assign(source="pykrx", symbol="005930")
    repository = MagicMock()
    # weekly 테이블의 마지막 날짜는 2024-01-05 (catalog은 일봉이 2024-01-20으로 덮어쓴 상태)
    repository.find_max_trade_dates.return_value = {"005930": date(2024, 1, 5)}
    repository.upsert_weekly_rows.return_value = 1
    job = CatalogWeeklyFetchJob(
        yfinance_collector=yfinance_collector,
        pykrx_collector=pykrx_collector,
        ohlcv_repository=repository,
    )
    window = FetchWindow(start_date=date(2024, 1, 1), end_date=date(2024, 1, 31))
    symbol = CatalogSymbol(
        symbol="005930",
        name="삼성전자",
        market="KOSPI",
        enabled=True,
        is_default=True,
        fetched_until_date=date(2024, 1, 20),  # 일봉이 덮어쓴 catalog 날짜
        last_collected_at=None,
    )

    results = job.run_for_pykrx([symbol], window, adjusted=True)

    # weekly 테이블 마지막 날짜(2024-01-05) 다음날부터 수집해야 함
    assert results[0].requested_start == date(2024, 1, 6)
    assert results[0].success is True
    assert results[0].rows_inserted == 1
