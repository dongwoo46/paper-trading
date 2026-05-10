from __future__ import annotations

from datetime import date
from decimal import Decimal
from unittest.mock import MagicMock, call, patch

import pytest

from src.repositories.investor_flow_repository import InvestorFlowRepository


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_mock_conn():
    mock_cursor = MagicMock()
    mock_cursor.__enter__ = MagicMock(return_value=mock_cursor)
    mock_cursor.__exit__ = MagicMock(return_value=False)

    mock_conn = MagicMock()
    mock_conn.__enter__ = MagicMock(return_value=mock_conn)
    mock_conn.__exit__ = MagicMock(return_value=False)
    mock_conn.cursor.return_value = mock_cursor
    return mock_conn, mock_cursor


def _investor_flow_record(symbol: str = "005930") -> dict:
    return {
        "trade_date": "20240101",
        "symbol": symbol,
        "market": "KOSPI",
        "foreign_buy_volume": 1000,
        "foreign_sell_volume": 800,
        "foreign_net_volume": 200,
        "foreign_buy_amount": Decimal("100000000"),
        "foreign_sell_amount": Decimal("80000000"),
        "foreign_net_amount": Decimal("20000000"),
        "institution_buy_volume": 500,
        "institution_sell_volume": 400,
        "institution_net_volume": 100,
        "institution_buy_amount": Decimal("50000000"),
        "institution_sell_amount": Decimal("40000000"),
        "institution_net_amount": Decimal("10000000"),
        "finance_invest_buy_volume": 100,
        "finance_invest_sell_volume": 80,
        "finance_invest_net_volume": 20,
        "finance_invest_buy_amount": Decimal("10000000"),
        "finance_invest_sell_amount": Decimal("8000000"),
        "finance_invest_net_amount": Decimal("2000000"),
        "insurance_buy_volume": 50,
        "insurance_sell_volume": 40,
        "insurance_net_volume": 10,
        "insurance_buy_amount": Decimal("5000000"),
        "insurance_sell_amount": Decimal("4000000"),
        "insurance_net_amount": Decimal("1000000"),
        "trust_buy_volume": 60,
        "trust_sell_volume": 50,
        "trust_net_volume": 10,
        "trust_buy_amount": Decimal("6000000"),
        "trust_sell_amount": Decimal("5000000"),
        "trust_net_amount": Decimal("1000000"),
        "private_equity_buy_volume": 30,
        "private_equity_sell_volume": 20,
        "private_equity_net_volume": 10,
        "private_equity_buy_amount": Decimal("3000000"),
        "private_equity_sell_amount": Decimal("2000000"),
        "private_equity_net_amount": Decimal("1000000"),
        "bank_buy_volume": 20,
        "bank_sell_volume": 15,
        "bank_net_volume": 5,
        "bank_buy_amount": Decimal("2000000"),
        "bank_sell_amount": Decimal("1500000"),
        "bank_net_amount": Decimal("500000"),
        "other_finance_buy_volume": 10,
        "other_finance_sell_volume": 8,
        "other_finance_net_volume": 2,
        "other_finance_buy_amount": Decimal("1000000"),
        "other_finance_sell_amount": Decimal("800000"),
        "other_finance_net_amount": Decimal("200000"),
        "pension_buy_volume": 40,
        "pension_sell_volume": 35,
        "pension_net_volume": 5,
        "pension_buy_amount": Decimal("4000000"),
        "pension_sell_amount": Decimal("3500000"),
        "pension_net_amount": Decimal("500000"),
        "individual_buy_volume": 300,
        "individual_sell_volume": 250,
        "individual_net_volume": 50,
        "individual_buy_amount": Decimal("30000000"),
        "individual_sell_amount": Decimal("25000000"),
        "individual_net_amount": Decimal("5000000"),
    }


def _short_selling_record(symbol: str = "005930") -> dict:
    return {
        "trade_date": "20240101",
        "symbol": symbol,
        "market": "KOSPI",
        "short_sell_volume": 5000,
        "short_sell_amount": Decimal("500000000"),
        "short_sell_ratio": Decimal("1.23"),
        "short_balance_volume": 10000,
        "short_balance_amount": Decimal("1000000000"),
    }


def _program_trading_record(symbol: str = "005930") -> dict:
    return {
        "trade_date": "20240101",
        "symbol": symbol,
        "market": "KOSPI",
        "arb_buy_volume": 200,
        "arb_sell_volume": 150,
        "arb_net_volume": 50,
        "arb_buy_amount": Decimal("20000000"),
        "arb_sell_amount": Decimal("15000000"),
        "arb_net_amount": Decimal("5000000"),
        "non_arb_buy_volume": 300,
        "non_arb_sell_volume": 250,
        "non_arb_net_volume": 50,
        "non_arb_buy_amount": Decimal("30000000"),
        "non_arb_sell_amount": Decimal("25000000"),
        "non_arb_net_amount": Decimal("5000000"),
    }


def _foreign_holding_record(symbol: str = "005930") -> dict:
    return {
        "trade_date": "20240101",
        "symbol": symbol,
        "market": "KOSPI",
        "holding_limit": 5000000000,
        "holding_volume": 3000000000,
        "exhaustion_ratio": Decimal("60.0"),
    }


# ---------------------------------------------------------------------------
# upsert_investor_flow
# ---------------------------------------------------------------------------

class TestUpsertInvestorFlow:
    def test_empty_records_returns_zero_without_db_call(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        result = repo.upsert_investor_flow([], mock_conn)
        assert result == 0
        mock_cursor.executemany.assert_not_called()

    def test_returns_count_of_records(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        records = [_investor_flow_record("005930"), _investor_flow_record("000660")]
        result = repo.upsert_investor_flow(records, mock_conn)
        assert result == 2
        mock_cursor.executemany.assert_called_once()

    def test_upsert_query_has_on_conflict(self):
        repo = InvestorFlowRepository()
        query = repo._investor_flow_upsert_query()
        assert "ON CONFLICT" in query
        assert "trade_date, symbol, market" in query
        assert "DO UPDATE" in query

    def test_upsert_query_targets_investor_flow_table(self):
        repo = InvestorFlowRepository()
        query = repo._investor_flow_upsert_query()
        assert "investor_flow" in query


# ---------------------------------------------------------------------------
# upsert_short_selling
# ---------------------------------------------------------------------------

class TestUpsertShortSelling:
    def test_empty_records_returns_zero(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        result = repo.upsert_short_selling([], mock_conn)
        assert result == 0

    def test_returns_count_of_records(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        records = [_short_selling_record()]
        result = repo.upsert_short_selling(records, mock_conn)
        assert result == 1

    def test_upsert_query_has_on_conflict(self):
        repo = InvestorFlowRepository()
        query = repo._short_selling_upsert_query()
        assert "ON CONFLICT" in query
        assert "trade_date, symbol, market" in query
        assert "DO UPDATE" in query

    def test_upsert_query_targets_short_selling_table(self):
        repo = InvestorFlowRepository()
        query = repo._short_selling_upsert_query()
        assert "short_selling" in query


# ---------------------------------------------------------------------------
# upsert_program_trading
# ---------------------------------------------------------------------------

class TestUpsertProgramTrading:
    def test_empty_records_returns_zero(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        result = repo.upsert_program_trading([], mock_conn)
        assert result == 0

    def test_returns_count_of_records(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        records = [_program_trading_record()]
        result = repo.upsert_program_trading(records, mock_conn)
        assert result == 1

    def test_upsert_query_has_on_conflict(self):
        repo = InvestorFlowRepository()
        query = repo._program_trading_upsert_query()
        assert "ON CONFLICT" in query
        assert "trade_date, symbol, market" in query
        assert "DO UPDATE" in query

    def test_upsert_query_targets_program_trading_table(self):
        repo = InvestorFlowRepository()
        query = repo._program_trading_upsert_query()
        assert "program_trading" in query


# ---------------------------------------------------------------------------
# upsert_foreign_holding
# ---------------------------------------------------------------------------

class TestUpsertForeignHolding:
    def test_empty_records_returns_zero(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        result = repo.upsert_foreign_holding([], mock_conn)
        assert result == 0

    def test_returns_count_of_records(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        records = [_foreign_holding_record()]
        result = repo.upsert_foreign_holding(records, mock_conn)
        assert result == 1

    def test_upsert_query_has_on_conflict(self):
        repo = InvestorFlowRepository()
        query = repo._foreign_holding_upsert_query()
        assert "ON CONFLICT" in query
        assert "trade_date, symbol, market" in query
        assert "DO UPDATE" in query

    def test_upsert_query_targets_foreign_holding_table(self):
        repo = InvestorFlowRepository()
        query = repo._foreign_holding_upsert_query()
        assert "foreign_holding" in query


# ---------------------------------------------------------------------------
# find_* methods
# ---------------------------------------------------------------------------

class TestFindInvestorFlow:
    def test_returns_list_from_cursor_fetchall(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        mock_cursor.fetchall.return_value = [
            ("20240101", "005930", "KOSPI", 1000, 800, 200,
             Decimal("100000000"), Decimal("80000000"), Decimal("20000000"),
             500, 400, 100,
             Decimal("50000000"), Decimal("40000000"), Decimal("10000000"),
             100, 80, 20, Decimal("10000000"), Decimal("8000000"), Decimal("2000000"),
             50, 40, 10, Decimal("5000000"), Decimal("4000000"), Decimal("1000000"),
             60, 50, 10, Decimal("6000000"), Decimal("5000000"), Decimal("1000000"),
             30, 20, 10, Decimal("3000000"), Decimal("2000000"), Decimal("1000000"),
             20, 15, 5, Decimal("2000000"), Decimal("1500000"), Decimal("500000"),
             10, 8, 2, Decimal("1000000"), Decimal("800000"), Decimal("200000"),
             40, 35, 5, Decimal("4000000"), Decimal("3500000"), Decimal("500000"),
             300, 250, 50, Decimal("30000000"), Decimal("25000000"), Decimal("5000000"))
        ]
        result = repo.find_investor_flow(
            symbol="005930",
            from_date=date(2024, 1, 1),
            to_date=date(2024, 1, 31),
            limit=250,
            conn=mock_conn,
        )
        assert isinstance(result, list)
        assert len(result) == 1

    def test_query_uses_order_by_trade_date_asc(self):
        repo = InvestorFlowRepository()
        query = repo._investor_flow_find_query()
        assert "ORDER BY trade_date ASC" in query

    def test_query_uses_limit(self):
        repo = InvestorFlowRepository()
        query = repo._investor_flow_find_query()
        assert "LIMIT" in query

    def test_market_none_does_not_add_market_condition(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        mock_cursor.fetchall.return_value = []
        repo.find_investor_flow(
            symbol="005930",
            from_date=date(2024, 1, 1),
            to_date=date(2024, 1, 31),
            limit=250,
            conn=mock_conn,
            market=None,
        )
        args = mock_cursor.execute.call_args[0]
        sql, params = args[0], args[1]
        assert "market" not in sql.lower().split("where")[1].split("order")[0] if "where" in sql.lower() else True
        assert len(params) == 4  # symbol, from_date, to_date, limit

    def test_market_specified_adds_market_condition(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        mock_cursor.fetchall.return_value = []
        repo.find_investor_flow(
            symbol="005930",
            from_date=date(2024, 1, 1),
            to_date=date(2024, 1, 31),
            limit=250,
            conn=mock_conn,
            market="KOSPI",
        )
        args = mock_cursor.execute.call_args[0]
        sql, params = args[0], args[1]
        assert "market = %s" in sql
        assert "KOSPI" in params


class TestFindShortSelling:
    def test_returns_list_from_cursor_fetchall(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        mock_cursor.fetchall.return_value = [
            ("20240101", "005930", "KOSPI", 5000, Decimal("500000000"),
             Decimal("1.23"), 10000, Decimal("1000000000"))
        ]
        result = repo.find_short_selling(
            symbol="005930",
            from_date=date(2024, 1, 1),
            to_date=date(2024, 1, 31),
            limit=250,
            conn=mock_conn,
        )
        assert isinstance(result, list)
        assert len(result) == 1

    def test_query_uses_order_by_trade_date_asc(self):
        repo = InvestorFlowRepository()
        query = repo._short_selling_find_query()
        assert "ORDER BY trade_date ASC" in query

    def test_market_none_does_not_add_market_condition(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        mock_cursor.fetchall.return_value = []
        repo.find_short_selling(
            symbol="005930",
            from_date=date(2024, 1, 1),
            to_date=date(2024, 1, 31),
            limit=250,
            conn=mock_conn,
            market=None,
        )
        args = mock_cursor.execute.call_args[0]
        sql, params = args[0], args[1]
        assert len(params) == 4  # symbol, from_date, to_date, limit

    def test_market_specified_adds_market_condition(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        mock_cursor.fetchall.return_value = []
        repo.find_short_selling(
            symbol="005930",
            from_date=date(2024, 1, 1),
            to_date=date(2024, 1, 31),
            limit=250,
            conn=mock_conn,
            market="KOSDAQ",
        )
        args = mock_cursor.execute.call_args[0]
        sql, params = args[0], args[1]
        assert "market = %s" in sql
        assert "KOSDAQ" in params


class TestFindProgramTrading:
    def test_returns_list_from_cursor_fetchall(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        mock_cursor.fetchall.return_value = [
            ("20240101", "005930", "KOSPI",
             200, 150, 50, Decimal("20000000"), Decimal("15000000"), Decimal("5000000"),
             300, 250, 50, Decimal("30000000"), Decimal("25000000"), Decimal("5000000"))
        ]
        result = repo.find_program_trading(
            symbol="005930",
            from_date=date(2024, 1, 1),
            to_date=date(2024, 1, 31),
            limit=250,
            conn=mock_conn,
        )
        assert isinstance(result, list)
        assert len(result) == 1

    def test_query_uses_order_by_trade_date_asc(self):
        repo = InvestorFlowRepository()
        query = repo._program_trading_find_query()
        assert "ORDER BY trade_date ASC" in query

    def test_market_none_does_not_add_market_condition(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        mock_cursor.fetchall.return_value = []
        repo.find_program_trading(
            symbol="005930",
            from_date=date(2024, 1, 1),
            to_date=date(2024, 1, 31),
            limit=250,
            conn=mock_conn,
            market=None,
        )
        args = mock_cursor.execute.call_args[0]
        sql, params = args[0], args[1]
        assert len(params) == 4  # symbol, from_date, to_date, limit

    def test_market_specified_adds_market_condition(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        mock_cursor.fetchall.return_value = []
        repo.find_program_trading(
            symbol="005930",
            from_date=date(2024, 1, 1),
            to_date=date(2024, 1, 31),
            limit=250,
            conn=mock_conn,
            market="KOSPI",
        )
        args = mock_cursor.execute.call_args[0]
        sql, params = args[0], args[1]
        assert "market = %s" in sql
        assert "KOSPI" in params


class TestFindForeignHolding:
    def test_returns_list_from_cursor_fetchall(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        mock_cursor.fetchall.return_value = [
            ("20240101", "005930", "KOSPI", 5000000000, 3000000000, Decimal("60.0"))
        ]
        result = repo.find_foreign_holding(
            symbol="005930",
            from_date=date(2024, 1, 1),
            to_date=date(2024, 1, 31),
            limit=250,
            conn=mock_conn,
        )
        assert isinstance(result, list)
        assert len(result) == 1

    def test_query_uses_order_by_trade_date_asc(self):
        repo = InvestorFlowRepository()
        query = repo._foreign_holding_find_query()
        assert "ORDER BY trade_date ASC" in query

    def test_market_none_does_not_add_market_condition(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        mock_cursor.fetchall.return_value = []
        repo.find_foreign_holding(
            symbol="005930",
            from_date=date(2024, 1, 1),
            to_date=date(2024, 1, 31),
            limit=250,
            conn=mock_conn,
            market=None,
        )
        args = mock_cursor.execute.call_args[0]
        sql, params = args[0], args[1]
        assert len(params) == 4  # symbol, from_date, to_date, limit

    def test_market_specified_adds_market_condition(self):
        repo = InvestorFlowRepository()
        mock_conn, mock_cursor = _make_mock_conn()
        mock_cursor.fetchall.return_value = []
        repo.find_foreign_holding(
            symbol="005930",
            from_date=date(2024, 1, 1),
            to_date=date(2024, 1, 31),
            limit=250,
            conn=mock_conn,
            market="KOSPI",
        )
        args = mock_cursor.execute.call_args[0]
        sql, params = args[0], args[1]
        assert "market = %s" in sql
        assert "KOSPI" in params
