from __future__ import annotations

from datetime import date
from decimal import Decimal
from unittest.mock import patch

import pytest
from fastapi.testclient import TestClient

from src.interfaces.api.app import app


def _client():
    return TestClient(app)


def _investor_flow_row():
    return {
        "trade_date": date(2024, 1, 5),
        "symbol": "005930",
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


def _short_selling_row():
    return {
        "trade_date": date(2024, 1, 5),
        "symbol": "005930",
        "market": "KOSPI",
        "short_sell_volume": 5000,
        "short_sell_amount": Decimal("500000000"),
        "short_sell_ratio": Decimal("1.23"),
        "short_balance_volume": 10000,
        "short_balance_amount": Decimal("1000000000"),
    }


def _program_trading_row():
    return {
        "trade_date": date(2024, 1, 5),
        "symbol": "005930",
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


def _foreign_holding_row():
    return {
        "trade_date": date(2024, 1, 5),
        "symbol": "005930",
        "market": "KOSPI",
        "holding_limit": 5000000000,
        "holding_volume": 3000000000,
        "exhaustion_ratio": Decimal("60.0"),
    }


# ---------------------------------------------------------------------------
# GET /investor-flow/{symbol}
# ---------------------------------------------------------------------------

class TestInvestorFlowEndpoint:
    def test_returns_200_with_data(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_investor_flow_records") as mock_fetch:
            mock_fetch.return_value = [_investor_flow_row()]
            response = client.get("/investor-flow/005930")
        assert response.status_code == 200
        data = response.json()
        assert isinstance(data, list)
        assert len(data) == 1

    def test_returns_200_with_empty_list_when_no_data(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_investor_flow_records") as mock_fetch:
            mock_fetch.return_value = []
            response = client.get("/investor-flow/005930")
        assert response.status_code == 200
        assert response.json() == []

    def test_rejects_blank_symbol(self):
        client = _client()
        response = client.get("/investor-flow/%20%20")
        assert response.status_code == 400

    def test_rejects_from_greater_than_to(self):
        client = _client()
        response = client.get("/investor-flow/005930?from=2024-02-01&to=2024-01-01")
        assert response.status_code == 400

    def test_amount_fields_serialized_as_strings(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_investor_flow_records") as mock_fetch:
            mock_fetch.return_value = [_investor_flow_row()]
            response = client.get("/investor-flow/005930")
        data = response.json()[0]
        assert isinstance(data["foreign_buy_amount"], str)
        assert isinstance(data["foreign_net_amount"], str)
        assert isinstance(data["individual_net_amount"], str)

    def test_amount_fields_not_float(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_investor_flow_records") as mock_fetch:
            mock_fetch.return_value = [_investor_flow_row()]
            response = client.get("/investor-flow/005930")
        data = response.json()[0]
        for key, val in data.items():
            if key.endswith("_amount"):
                assert not isinstance(val, float), f"{key} must not be float in JSON"

    def test_symbol_normalized_to_uppercase(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_investor_flow_records") as mock_fetch:
            mock_fetch.return_value = []
            response = client.get("/investor-flow/005930")
        assert response.status_code == 200
        call_kwargs = mock_fetch.call_args.kwargs
        assert call_kwargs["symbol"] == "005930"

    def test_default_limit_is_250(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_investor_flow_records") as mock_fetch:
            mock_fetch.return_value = []
            client.get("/investor-flow/005930")
        call_kwargs = mock_fetch.call_args.kwargs
        assert call_kwargs["limit"] == 250

    def test_limit_clamped_to_1000(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_investor_flow_records") as mock_fetch:
            mock_fetch.return_value = []
            client.get("/investor-flow/005930?limit=9999")
        call_kwargs = mock_fetch.call_args.kwargs
        assert call_kwargs["limit"] == 1000


# ---------------------------------------------------------------------------
# GET /short-selling/{symbol}
# ---------------------------------------------------------------------------

class TestShortSellingEndpoint:
    def test_returns_200_with_data(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_short_selling_records") as mock_fetch:
            mock_fetch.return_value = [_short_selling_row()]
            response = client.get("/short-selling/005930")
        assert response.status_code == 200
        data = response.json()
        assert len(data) == 1

    def test_returns_200_empty_list_when_no_data(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_short_selling_records") as mock_fetch:
            mock_fetch.return_value = []
            response = client.get("/short-selling/005930")
        assert response.status_code == 200
        assert response.json() == []

    def test_rejects_blank_symbol(self):
        client = _client()
        response = client.get("/short-selling/%20%20")
        assert response.status_code == 400

    def test_amount_fields_serialized_as_strings(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_short_selling_records") as mock_fetch:
            mock_fetch.return_value = [_short_selling_row()]
            response = client.get("/short-selling/005930")
        data = response.json()[0]
        assert isinstance(data["short_sell_amount"], str)
        assert isinstance(data["short_balance_amount"], str)
        assert isinstance(data["short_sell_ratio"], str)


# ---------------------------------------------------------------------------
# GET /program-trading/{symbol}
# ---------------------------------------------------------------------------

class TestProgramTradingEndpoint:
    def test_returns_200_with_data(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_program_trading_records") as mock_fetch:
            mock_fetch.return_value = [_program_trading_row()]
            response = client.get("/program-trading/005930")
        assert response.status_code == 200
        assert len(response.json()) == 1

    def test_returns_200_empty_list_when_no_data(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_program_trading_records") as mock_fetch:
            mock_fetch.return_value = []
            response = client.get("/program-trading/005930")
        assert response.status_code == 200
        assert response.json() == []

    def test_rejects_blank_symbol(self):
        client = _client()
        response = client.get("/program-trading/%20%20")
        assert response.status_code == 400

    def test_amount_fields_serialized_as_strings(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_program_trading_records") as mock_fetch:
            mock_fetch.return_value = [_program_trading_row()]
            response = client.get("/program-trading/005930")
        data = response.json()[0]
        assert isinstance(data["arb_buy_amount"], str)
        assert isinstance(data["non_arb_net_amount"], str)


# ---------------------------------------------------------------------------
# GET /foreign-holding/{symbol}
# ---------------------------------------------------------------------------

class TestForeignHoldingEndpoint:
    def test_returns_200_with_data(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_foreign_holding_records") as mock_fetch:
            mock_fetch.return_value = [_foreign_holding_row()]
            response = client.get("/foreign-holding/005930")
        assert response.status_code == 200
        assert len(response.json()) == 1

    def test_returns_200_empty_list_when_no_data(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_foreign_holding_records") as mock_fetch:
            mock_fetch.return_value = []
            response = client.get("/foreign-holding/005930")
        assert response.status_code == 200
        assert response.json() == []

    def test_rejects_blank_symbol(self):
        client = _client()
        response = client.get("/foreign-holding/%20%20")
        assert response.status_code == 400

    def test_exhaustion_ratio_serialized_as_string(self):
        client = _client()
        with patch("src.interfaces.api.app.fetch_foreign_holding_records") as mock_fetch:
            mock_fetch.return_value = [_foreign_holding_row()]
            response = client.get("/foreign-holding/005930")
        data = response.json()[0]
        assert isinstance(data["exhaustion_ratio"], str)
