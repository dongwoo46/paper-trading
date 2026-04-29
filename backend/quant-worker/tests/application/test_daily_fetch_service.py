from __future__ import annotations

from unittest.mock import MagicMock, patch

import pytest

from src.application.daily_fetch_service import DailyFetchOptions, execute


def _mock_patches():
    return (
        patch("src.application.daily_fetch_service.CatalogDailyFetchJob"),
        patch("src.application.daily_fetch_service.PostgresSymbolCatalogRepository"),
        patch("src.application.daily_fetch_service.MarketDailyOhlcvRepository"),
        patch("src.application.daily_fetch_service.YFinanceDailyCollector"),
        patch("src.application.daily_fetch_service.PykrxDailyCollector"),
    )


def _run_execute(provider: str = "all") -> dict:
    p1, p2, p3, p4, p5 = _mock_patches()
    with p1 as MockJob, p2 as MockCatalog, p3, p4, p5:
        mock_job = MockJob.return_value
        mock_job.run_for_yfinance.return_value = []
        mock_job.run_for_pykrx.return_value = []
        MockCatalog.return_value.list_symbols.return_value = []
        MockCatalog.return_value.update_collection_status.return_value = None
        return execute(DailyFetchOptions(provider=provider, start="2024-01-01", end="2024-01-31"))


class TestExecuteValidation:
    def test_start_after_end_raises_value_error(self):
        p1, p2, p3, p4, p5 = _mock_patches()
        with p1, p2, p3, p4, p5:
            with pytest.raises(ValueError):
                execute(DailyFetchOptions(provider="all", start="2024-02-01", end="2024-01-01"))


class TestExecuteProviderRouting:
    def test_yfinance_only_does_not_call_pykrx(self):
        p1, p2, p3, p4, p5 = _mock_patches()
        with p1 as MockJob, p2 as MockCatalog, p3, p4, p5:
            mock_job = MockJob.return_value
            mock_job.run_for_yfinance.return_value = []
            mock_job.run_for_pykrx.return_value = []
            MockCatalog.return_value.list_symbols.return_value = []
            MockCatalog.return_value.update_collection_status.return_value = None

            execute(DailyFetchOptions(provider="yfinance", start="2024-01-01", end="2024-01-31"))

            mock_job.run_for_yfinance.assert_called_once()
            mock_job.run_for_pykrx.assert_not_called()

    def test_pykrx_only_does_not_call_yfinance(self):
        p1, p2, p3, p4, p5 = _mock_patches()
        with p1 as MockJob, p2 as MockCatalog, p3, p4, p5:
            mock_job = MockJob.return_value
            mock_job.run_for_yfinance.return_value = []
            mock_job.run_for_pykrx.return_value = []
            MockCatalog.return_value.list_symbols.return_value = []
            MockCatalog.return_value.update_collection_status.return_value = None

            execute(DailyFetchOptions(provider="pykrx", start="2024-01-01", end="2024-01-31"))

            mock_job.run_for_pykrx.assert_called_once()
            mock_job.run_for_yfinance.assert_not_called()

    def test_all_calls_both_providers(self):
        p1, p2, p3, p4, p5 = _mock_patches()
        with p1 as MockJob, p2 as MockCatalog, p3, p4, p5:
            mock_job = MockJob.return_value
            mock_job.run_for_yfinance.return_value = []
            mock_job.run_for_pykrx.return_value = []
            MockCatalog.return_value.list_symbols.return_value = []
            MockCatalog.return_value.update_collection_status.return_value = None

            execute(DailyFetchOptions(provider="all", start="2024-01-01", end="2024-01-31"))

            mock_job.run_for_yfinance.assert_called_once()
            mock_job.run_for_pykrx.assert_called_once()


class TestExecuteResponseShape:
    def test_response_has_total_rows_inserted(self):
        result = _run_execute()
        assert "total_rows_inserted" in result

    def test_response_has_no_summary_path(self):
        result = _run_execute()
        assert "summary_path" not in result

    def test_daily_fetch_options_has_no_output_root(self):
        import dataclasses
        field_names = {f.name for f in dataclasses.fields(DailyFetchOptions)}
        assert "output_root" not in field_names
