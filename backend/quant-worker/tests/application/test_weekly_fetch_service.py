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
    )


def test_execute_rejects_invalid_date_window():
    p1, p2, p3, p4 = _patches()
    with p1, p2, p3, p4:
        with pytest.raises(ValueError):
            execute(WeeklyFetchOptions(provider="yfinance", start="2024-02-01", end="2024-01-01"))


def test_execute_all_routes_to_yfinance_only():
    p1, p2, p3, p4 = _patches()
    with p1 as MockJob, p2 as MockCatalog, p3, p4:
        MockJob.return_value.run_for_yfinance.return_value = []
        MockCatalog.return_value.list_symbols.return_value = []
        MockCatalog.return_value.update_collection_status.return_value = None

        result = execute(WeeklyFetchOptions(provider="all", start="2024-01-01", end="2024-01-31"))

        MockJob.return_value.run_for_yfinance.assert_called_once()
        assert result["provider"] == "all"

