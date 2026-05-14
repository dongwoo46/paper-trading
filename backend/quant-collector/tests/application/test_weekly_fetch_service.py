from __future__ import annotations

from unittest.mock import patch

import pytest

from src.application.weekly_fetch_service import WeeklyFetchOptions, execute


def _patches():
    return (
        patch("src.application.weekly_fetch_service.CatalogWeeklyFetchJob"),
        patch("src.application.weekly_fetch_service.PostgresSymbolCatalogRepository"),
        patch("src.application.weekly_fetch_service.MarketWeeklyOhlcvRepository"),
        patch("src.application.weekly_fetch_service.YFinanceWeeklyCollector"),
        patch("src.application.weekly_fetch_service.PykrxWeeklyCollector"),
    )


def test_execute_rejects_invalid_date_window():
    p1, p2, p3, p4, p5 = _patches()
    with p1, p2, p3, p4, p5:
        with pytest.raises(ValueError):
            execute(WeeklyFetchOptions(provider="yfinance", start="2024-02-01", end="2024-01-01"))


def test_execute_all_routes_to_both_providers():
    p1, p2, p3, p4, p5 = _patches()
    with p1 as MockJob, p2 as MockCatalog, p3, p4, p5:
        MockJob.return_value.run_for_yfinance.return_value = []
        MockJob.return_value.run_for_pykrx.return_value = []
        MockCatalog.return_value.list_symbols.return_value = []
        MockCatalog.return_value.update_collection_status.return_value = None

        result = execute(WeeklyFetchOptions(provider="all", start="2024-01-01", end="2024-01-31"))

        MockJob.return_value.run_for_yfinance.assert_called_once()
        MockJob.return_value.run_for_pykrx.assert_called_once()
        assert result["provider"] == "all"


def test_execute_pykrx_only_does_not_call_yfinance():
    p1, p2, p3, p4, p5 = _patches()
    with p1 as MockJob, p2 as MockCatalog, p3, p4, p5:
        MockJob.return_value.run_for_yfinance.return_value = []
        MockJob.return_value.run_for_pykrx.return_value = []
        MockCatalog.return_value.list_symbols.return_value = []
        MockCatalog.return_value.update_collection_status.return_value = None

        execute(WeeklyFetchOptions(provider="pykrx", start="2024-01-01", end="2024-01-31"))

        MockJob.return_value.run_for_pykrx.assert_called_once()
        MockJob.return_value.run_for_yfinance.assert_not_called()
