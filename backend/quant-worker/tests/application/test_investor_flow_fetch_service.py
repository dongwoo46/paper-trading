from __future__ import annotations

from datetime import date, timedelta
from decimal import Decimal
from unittest.mock import MagicMock, patch

import pytest

from src.application.investor_flow_fetch_service import InvestorFlowFetchService


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_collector(
    investor_flow=None,
    short_selling=None,
    program_trading=None,
    foreign_holding=None,
):
    collector = MagicMock()
    collector.fetch_investor_flow.return_value = investor_flow or []
    collector.fetch_short_selling.return_value = short_selling or []
    collector.fetch_program_trading.return_value = program_trading or []
    collector.fetch_foreign_holding.return_value = foreign_holding or []
    return collector


def _make_repository():
    repo = MagicMock()
    repo.upsert_investor_flow.return_value = 0
    repo.upsert_short_selling.return_value = 0
    repo.upsert_program_trading.return_value = 0
    repo.upsert_foreign_holding.return_value = 0
    return repo


def _make_conn():
    conn = MagicMock()
    conn.__enter__ = MagicMock(return_value=conn)
    conn.__exit__ = MagicMock(return_value=False)
    return conn


# ---------------------------------------------------------------------------
# _resolve_business_date
# ---------------------------------------------------------------------------

class TestResolveBusinessDate:
    def test_monday_returns_monday(self):
        svc = InvestorFlowFetchService(_make_collector(), _make_repository(), MagicMock())
        # 2024-01-08 is Monday → business day, return as-is
        result = svc._resolve_business_date(date(2024, 1, 8))
        assert result == date(2024, 1, 8)

    def test_tuesday_returns_tuesday(self):
        svc = InvestorFlowFetchService(_make_collector(), _make_repository(), MagicMock())
        # 2024-01-09 is Tuesday → business day, return as-is
        result = svc._resolve_business_date(date(2024, 1, 9))
        assert result == date(2024, 1, 9)

    def test_wednesday_returns_wednesday(self):
        svc = InvestorFlowFetchService(_make_collector(), _make_repository(), MagicMock())
        # 2024-01-10 is Wednesday → business day, return as-is
        result = svc._resolve_business_date(date(2024, 1, 10))
        assert result == date(2024, 1, 10)

    def test_friday_returns_friday(self):
        svc = InvestorFlowFetchService(_make_collector(), _make_repository(), MagicMock())
        # 2024-01-12 is Friday → business day, return as-is
        result = svc._resolve_business_date(date(2024, 1, 12))
        assert result == date(2024, 1, 12)

    def test_saturday_resolves_to_friday(self):
        svc = InvestorFlowFetchService(_make_collector(), _make_repository(), MagicMock())
        # 2024-01-13 is Saturday → return previous Friday
        result = svc._resolve_business_date(date(2024, 1, 13))
        assert result == date(2024, 1, 12)  # Friday

    def test_sunday_resolves_to_friday(self):
        svc = InvestorFlowFetchService(_make_collector(), _make_repository(), MagicMock())
        # 2024-01-14 is Sunday → return previous Friday
        result = svc._resolve_business_date(date(2024, 1, 14))
        assert result == date(2024, 1, 12)  # Friday

    def test_none_uses_today(self):
        svc = InvestorFlowFetchService(_make_collector(), _make_repository(), MagicMock())
        # Should not raise; the result is weekday-dependent
        result = svc._resolve_business_date(None)
        assert isinstance(result, date)

    def test_none_with_weekday_today_returns_today(self):
        svc = InvestorFlowFetchService(_make_collector(), _make_repository(), MagicMock())
        # 2024-01-09 is Tuesday (weekday) → today itself is returned
        with patch("src.application.investor_flow_fetch_service.date") as mock_date:
            mock_date.today.return_value = date(2024, 1, 9)
            mock_date.side_effect = lambda *args, **kwargs: date(*args, **kwargs)
            result = svc._resolve_business_date(None)
        assert result == date(2024, 1, 9)

    def test_none_with_saturday_today_returns_friday(self):
        svc = InvestorFlowFetchService(_make_collector(), _make_repository(), MagicMock())
        # 2024-01-13 is Saturday → return Friday
        with patch("src.application.investor_flow_fetch_service.date") as mock_date:
            mock_date.today.return_value = date(2024, 1, 13)
            mock_date.side_effect = lambda *args, **kwargs: date(*args, **kwargs)
            result = svc._resolve_business_date(None)
        assert result == date(2024, 1, 12)


# ---------------------------------------------------------------------------
# execute — success path
# ---------------------------------------------------------------------------

class TestExecuteSuccess:
    def test_returns_dict_with_expected_keys(self):
        collector = _make_collector()
        repo = _make_repository()
        conn_factory = MagicMock(return_value=_make_conn())
        svc = InvestorFlowFetchService(collector, repo, conn_factory)

        result = svc.execute(date(2024, 1, 5), "KOSPI")
        assert "investor_flow" in result
        assert "short_selling" in result
        assert "program_trading" in result
        assert "foreign_holding" in result
        assert "errors" in result
        assert "trade_date" in result
        assert "market" in result

    def test_collector_called_with_correct_date_and_market(self):
        collector = _make_collector()
        repo = _make_repository()
        conn_factory = MagicMock(return_value=_make_conn())
        svc = InvestorFlowFetchService(collector, repo, conn_factory)

        svc.execute(date(2024, 1, 5), "KOSPI")
        collector.fetch_investor_flow.assert_called_once_with("20240105", "KOSPI")
        collector.fetch_short_selling.assert_called_once_with("20240105", "KOSPI")
        collector.fetch_program_trading.assert_called_once_with("20240105", "KOSPI")
        collector.fetch_foreign_holding.assert_called_once_with("20240105", "KOSPI")

    def test_none_date_calls_collector_with_today_if_weekday(self):
        collector = _make_collector()
        repo = _make_repository()
        conn_factory = MagicMock(return_value=_make_conn())
        svc = InvestorFlowFetchService(collector, repo, conn_factory)

        # 2024-01-09 is Tuesday (weekday) → use today as-is
        with patch("src.application.investor_flow_fetch_service.date") as mock_date:
            mock_date.today.return_value = date(2024, 1, 9)
            mock_date.side_effect = lambda *args, **kwargs: date(*args, **kwargs)
            svc.execute(None, "KOSPI")
        # today is Tuesday (2024-01-09) → collector called with today
        collector.fetch_investor_flow.assert_called_once_with("20240109", "KOSPI")

    def test_row_counts_in_result(self):
        sample = [{"symbol": "005930", "market": "KOSPI", "trade_date": "20240105"}]
        collector = _make_collector(
            investor_flow=sample,
            short_selling=sample,
            program_trading=sample,
            foreign_holding=sample,
        )
        repo = _make_repository()
        repo.upsert_investor_flow.return_value = 1
        repo.upsert_short_selling.return_value = 1
        repo.upsert_program_trading.return_value = 1
        repo.upsert_foreign_holding.return_value = 1

        conn_factory = MagicMock(return_value=_make_conn())
        svc = InvestorFlowFetchService(collector, repo, conn_factory)
        result = svc.execute(date(2024, 1, 5), "KOSPI")

        assert result["investor_flow"] == 1
        assert result["short_selling"] == 1
        assert result["program_trading"] == 1
        assert result["foreign_holding"] == 1

    def test_errors_empty_on_success(self):
        collector = _make_collector()
        repo = _make_repository()
        conn_factory = MagicMock(return_value=_make_conn())
        svc = InvestorFlowFetchService(collector, repo, conn_factory)

        result = svc.execute(date(2024, 1, 5), "KOSPI")
        assert result["errors"] == []


# ---------------------------------------------------------------------------
# execute — error isolation
# ---------------------------------------------------------------------------

class TestExecuteErrorIsolation:
    def test_collector_exception_recorded_in_errors(self):
        collector = _make_collector()
        collector.fetch_investor_flow.side_effect = RuntimeError("pykrx timeout")
        repo = _make_repository()
        conn_factory = MagicMock(return_value=_make_conn())
        svc = InvestorFlowFetchService(collector, repo, conn_factory)

        result = svc.execute(date(2024, 1, 5), "KOSPI")
        assert len(result["errors"]) == 1
        assert "investor_flow" in result["errors"][0]["dataset"]

    def test_one_collector_error_does_not_stop_others(self):
        collector = _make_collector()
        collector.fetch_investor_flow.side_effect = RuntimeError("pykrx timeout")
        repo = _make_repository()
        conn_factory = MagicMock(return_value=_make_conn())
        svc = InvestorFlowFetchService(collector, repo, conn_factory)

        result = svc.execute(date(2024, 1, 5), "KOSPI")
        # Other datasets still called
        collector.fetch_short_selling.assert_called_once()
        collector.fetch_program_trading.assert_called_once()
        collector.fetch_foreign_holding.assert_called_once()

    def test_multiple_errors_all_recorded(self):
        collector = _make_collector()
        collector.fetch_investor_flow.side_effect = RuntimeError("err1")
        collector.fetch_short_selling.side_effect = RuntimeError("err2")
        repo = _make_repository()
        conn_factory = MagicMock(return_value=_make_conn())
        svc = InvestorFlowFetchService(collector, repo, conn_factory)

        result = svc.execute(date(2024, 1, 5), "KOSPI")
        assert len(result["errors"]) == 2
