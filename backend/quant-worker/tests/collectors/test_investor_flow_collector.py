from __future__ import annotations

from decimal import Decimal
from unittest.mock import patch

import pandas as pd
import pytest

from src.collectors.investor_flow_collector import InvestorFlowCollector


# ---------------------------------------------------------------------------
# Fixtures / helpers
# ---------------------------------------------------------------------------

def _make_investor_flow_df() -> pd.DataFrame:
    """Wide DataFrame mimicking pykrx get_market_trading_volume_by_investor output."""
    columns = [
        "티커",
        # 외국인
        "외국인_매수", "외국인_매도", "외국인_순매수",
        "외국인_매수금", "외국인_매도금", "외국인_순매수금",
        # 기관합계
        "기관합계_매수", "기관합계_매도", "기관합계_순매수",
        "기관합계_매수금", "기관합계_매도금", "기관합계_순매수금",
        # 금융투자
        "금융투자_매수", "금융투자_매도", "금융투자_순매수",
        "금융투자_매수금", "금융투자_매도금", "금융투자_순매수금",
        # 보험
        "보험_매수", "보험_매도", "보험_순매수",
        "보험_매수금", "보험_매도금", "보험_순매수금",
        # 투신
        "투신_매수", "투신_매도", "투신_순매수",
        "투신_매수금", "투신_매도금", "투신_순매수금",
        # 사모
        "사모_매수", "사모_매도", "사모_순매수",
        "사모_매수금", "사모_매도금", "사모_순매수금",
        # 은행
        "은행_매수", "은행_매도", "은행_순매수",
        "은행_매수금", "은행_매도금", "은행_순매수금",
        # 기타금융
        "기타금융_매수", "기타금융_매도", "기타금융_순매수",
        "기타금융_매수금", "기타금융_매도금", "기타금융_순매수금",
        # 연기금
        "연기금_매수", "연기금_매도", "연기금_순매수",
        "연기금_매수금", "연기금_매도금", "연기금_순매수금",
        # 개인
        "개인_매수", "개인_매도", "개인_순매수",
        "개인_매수금", "개인_매도금", "개인_순매수금",
    ]
    data = [[
        "005930",
        1000, 800, 200,
        100000000, 80000000, 20000000,
        500, 400, 100,
        50000000, 40000000, 10000000,
        100, 80, 20,
        10000000, 8000000, 2000000,
        50, 40, 10,
        5000000, 4000000, 1000000,
        60, 50, 10,
        6000000, 5000000, 1000000,
        30, 20, 10,
        3000000, 2000000, 1000000,
        20, 15, 5,
        2000000, 1500000, 500000,
        10, 8, 2,
        1000000, 800000, 200000,
        40, 35, 5,
        4000000, 3500000, 500000,
        300, 250, 50,
        30000000, 25000000, 5000000,
    ]]
    return pd.DataFrame(data, columns=columns)


def _make_short_selling_df() -> pd.DataFrame:
    """Wide DataFrame mimicking pykrx short selling output."""
    columns = [
        "티커",
        "공매도거래량", "공매도거래대금", "공매도비율",
        "공매도잔고수량", "공매도잔고금액",
    ]
    data = [["005930", 5000, 500000000, 1.23, 10000, 1000000000]]
    return pd.DataFrame(data, columns=columns)


def _make_program_trading_df() -> pd.DataFrame:
    """Wide DataFrame mimicking pykrx program trading output."""
    columns = [
        "티커",
        "차익매수", "차익매도", "차익순매수",
        "차익매수금", "차익매도금", "차익순매수금",
        "비차익매수", "비차익매도", "비차익순매수",
        "비차익매수금", "비차익매도금", "비차익순매수금",
    ]
    data = [["005930", 200, 150, 50, 20000000, 15000000, 5000000,
             300, 250, 50, 30000000, 25000000, 5000000]]
    return pd.DataFrame(data, columns=columns)


def _make_foreign_holding_df() -> pd.DataFrame:
    """Wide DataFrame mimicking pykrx foreign holding output."""
    columns = [
        "티커",
        "외국인보유한도", "외국인보유수량", "외국인한도소진율",
    ]
    data = [["005930", 5000000000, 3000000000, 60.0]]
    return pd.DataFrame(data, columns=columns)


# ---------------------------------------------------------------------------
# InvestorFlowCollector.fetch_investor_flow
# ---------------------------------------------------------------------------

class TestFetchInvestorFlow:
    def test_returns_list_of_dicts(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_trading_volume_by_investor.return_value = _make_investor_flow_df()
            result = collector.fetch_investor_flow("20240101", "KOSPI")
        assert isinstance(result, list)
        assert len(result) == 1

    def test_amount_columns_are_decimal(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_trading_volume_by_investor.return_value = _make_investor_flow_df()
            result = collector.fetch_investor_flow("20240101", "KOSPI")
        rec = result[0]
        amount_fields = [k for k in rec if k.endswith("_amount")]
        assert len(amount_fields) > 0
        for field in amount_fields:
            assert isinstance(rec[field], Decimal), f"{field} must be Decimal, got {type(rec[field])}"

    def test_column_names_are_english(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_trading_volume_by_investor.return_value = _make_investor_flow_df()
            result = collector.fetch_investor_flow("20240101", "KOSPI")
        rec = result[0]
        assert "foreign_buy_volume" in rec
        assert "foreign_net_amount" in rec
        assert "institution_buy_volume" in rec
        assert "individual_net_volume" in rec
        assert "symbol" in rec
        assert "market" in rec
        assert "trade_date" in rec

    def test_market_injected_correctly(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_trading_volume_by_investor.return_value = _make_investor_flow_df()
            result = collector.fetch_investor_flow("20240101", "KOSPI")
        assert result[0]["market"] == "KOSPI"

    def test_empty_dataframe_returns_empty_list(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_trading_volume_by_investor.return_value = pd.DataFrame()
            result = collector.fetch_investor_flow("20240101", "KOSPI")
        assert result == []

    def test_no_float_in_amount_fields(self):
        """Ensure no float type sneaks through (financial safety rule)."""
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_trading_volume_by_investor.return_value = _make_investor_flow_df()
            result = collector.fetch_investor_flow("20240101", "KOSPI")
        rec = result[0]
        for k, v in rec.items():
            if k.endswith("_amount"):
                assert not isinstance(v, float), f"{k} must not be float"


# ---------------------------------------------------------------------------
# InvestorFlowCollector.fetch_short_selling
# ---------------------------------------------------------------------------

class TestFetchShortSelling:
    def test_returns_list_of_dicts(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_short_selling_by_ticker.return_value = _make_short_selling_df()
            result = collector.fetch_short_selling("20240101", "KOSPI")
        assert isinstance(result, list)
        assert len(result) == 1

    def test_amount_columns_are_decimal(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_short_selling_by_ticker.return_value = _make_short_selling_df()
            result = collector.fetch_short_selling("20240101", "KOSPI")
        rec = result[0]
        assert isinstance(rec["short_sell_amount"], Decimal)
        assert isinstance(rec["short_balance_amount"], Decimal)

    def test_ratio_column_is_decimal(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_short_selling_by_ticker.return_value = _make_short_selling_df()
            result = collector.fetch_short_selling("20240101", "KOSPI")
        rec = result[0]
        assert isinstance(rec["short_sell_ratio"], Decimal)

    def test_column_names_are_english(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_short_selling_by_ticker.return_value = _make_short_selling_df()
            result = collector.fetch_short_selling("20240101", "KOSPI")
        rec = result[0]
        assert "short_sell_volume" in rec
        assert "short_sell_amount" in rec
        assert "short_sell_ratio" in rec
        assert "short_balance_volume" in rec
        assert "short_balance_amount" in rec
        assert "symbol" in rec
        assert "market" in rec

    def test_empty_dataframe_returns_empty_list(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_short_selling_by_ticker.return_value = pd.DataFrame()
            result = collector.fetch_short_selling("20240101", "KOSPI")
        assert result == []


# ---------------------------------------------------------------------------
# InvestorFlowCollector.fetch_program_trading
# ---------------------------------------------------------------------------

class TestFetchProgramTrading:
    def test_returns_list_of_dicts(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_program_trading_by_date.return_value = _make_program_trading_df()
            result = collector.fetch_program_trading("20240101", "KOSPI")
        assert isinstance(result, list)
        assert len(result) == 1

    def test_amount_columns_are_decimal(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_program_trading_by_date.return_value = _make_program_trading_df()
            result = collector.fetch_program_trading("20240101", "KOSPI")
        rec = result[0]
        for field in ["arb_buy_amount", "arb_sell_amount", "arb_net_amount",
                      "non_arb_buy_amount", "non_arb_sell_amount", "non_arb_net_amount"]:
            assert isinstance(rec[field], Decimal), f"{field} must be Decimal"

    def test_column_names_are_english(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_program_trading_by_date.return_value = _make_program_trading_df()
            result = collector.fetch_program_trading("20240101", "KOSPI")
        rec = result[0]
        for col in ["arb_buy_volume", "arb_sell_volume", "arb_net_volume",
                    "non_arb_buy_volume", "non_arb_sell_volume", "non_arb_net_volume",
                    "symbol", "market", "trade_date"]:
            assert col in rec

    def test_empty_dataframe_returns_empty_list(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_program_trading_by_date.return_value = pd.DataFrame()
            result = collector.fetch_program_trading("20240101", "KOSPI")
        assert result == []


# ---------------------------------------------------------------------------
# InvestorFlowCollector.fetch_foreign_holding
# ---------------------------------------------------------------------------

class TestFetchForeignHolding:
    def test_returns_list_of_dicts(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_cap_by_date.return_value = _make_foreign_holding_df()
            result = collector.fetch_foreign_holding("20240101", "KOSPI")
        assert isinstance(result, list)
        assert len(result) == 1

    def test_ratio_column_is_decimal(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_cap_by_date.return_value = _make_foreign_holding_df()
            result = collector.fetch_foreign_holding("20240101", "KOSPI")
        rec = result[0]
        assert isinstance(rec["exhaustion_ratio"], Decimal)

    def test_column_names_are_english(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_cap_by_date.return_value = _make_foreign_holding_df()
            result = collector.fetch_foreign_holding("20240101", "KOSPI")
        rec = result[0]
        for col in ["holding_limit", "holding_volume", "exhaustion_ratio", "symbol", "market", "trade_date"]:
            assert col in rec

    def test_empty_dataframe_returns_empty_list(self):
        collector = InvestorFlowCollector()
        with patch("src.collectors.investor_flow_collector.stock") as mock_stock:
            mock_stock.get_market_cap_by_date.return_value = pd.DataFrame()
            result = collector.fetch_foreign_holding("20240101", "KOSPI")
        assert result == []
