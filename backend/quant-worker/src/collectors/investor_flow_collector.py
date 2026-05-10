from __future__ import annotations

import logging
from decimal import Decimal

import pandas as pd
from pykrx import stock

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# pykrx column name → internal standard column name mappings
# ---------------------------------------------------------------------------

_INVESTOR_FLOW_COLUMNS = {
    "티커": "symbol",
    # 외국인
    "외국인_매수": "foreign_buy_volume",
    "외국인_매도": "foreign_sell_volume",
    "외국인_순매수": "foreign_net_volume",
    "외국인_매수금": "foreign_buy_amount",
    "외국인_매도금": "foreign_sell_amount",
    "외국인_순매수금": "foreign_net_amount",
    # 기관합계
    "기관합계_매수": "institution_buy_volume",
    "기관합계_매도": "institution_sell_volume",
    "기관합계_순매수": "institution_net_volume",
    "기관합계_매수금": "institution_buy_amount",
    "기관합계_매도금": "institution_sell_amount",
    "기관합계_순매수금": "institution_net_amount",
    # 금융투자
    "금융투자_매수": "finance_invest_buy_volume",
    "금융투자_매도": "finance_invest_sell_volume",
    "금융투자_순매수": "finance_invest_net_volume",
    "금융투자_매수금": "finance_invest_buy_amount",
    "금융투자_매도금": "finance_invest_sell_amount",
    "금융투자_순매수금": "finance_invest_net_amount",
    # 보험
    "보험_매수": "insurance_buy_volume",
    "보험_매도": "insurance_sell_volume",
    "보험_순매수": "insurance_net_volume",
    "보험_매수금": "insurance_buy_amount",
    "보험_매도금": "insurance_sell_amount",
    "보험_순매수금": "insurance_net_amount",
    # 투신
    "투신_매수": "trust_buy_volume",
    "투신_매도": "trust_sell_volume",
    "투신_순매수": "trust_net_volume",
    "투신_매수금": "trust_buy_amount",
    "투신_매도금": "trust_sell_amount",
    "투신_순매수금": "trust_net_amount",
    # 사모
    "사모_매수": "private_equity_buy_volume",
    "사모_매도": "private_equity_sell_volume",
    "사모_순매수": "private_equity_net_volume",
    "사모_매수금": "private_equity_buy_amount",
    "사모_매도금": "private_equity_sell_amount",
    "사모_순매수금": "private_equity_net_amount",
    # 은행
    "은행_매수": "bank_buy_volume",
    "은행_매도": "bank_sell_volume",
    "은행_순매수": "bank_net_volume",
    "은행_매수금": "bank_buy_amount",
    "은행_매도금": "bank_sell_amount",
    "은행_순매수금": "bank_net_amount",
    # 기타금융
    "기타금융_매수": "other_finance_buy_volume",
    "기타금융_매도": "other_finance_sell_volume",
    "기타금융_순매수": "other_finance_net_volume",
    "기타금융_매수금": "other_finance_buy_amount",
    "기타금융_매도금": "other_finance_sell_amount",
    "기타금융_순매수금": "other_finance_net_amount",
    # 연기금
    "연기금_매수": "pension_buy_volume",
    "연기금_매도": "pension_sell_volume",
    "연기금_순매수": "pension_net_volume",
    "연기금_매수금": "pension_buy_amount",
    "연기금_매도금": "pension_sell_amount",
    "연기금_순매수금": "pension_net_amount",
    # 개인
    "개인_매수": "individual_buy_volume",
    "개인_매도": "individual_sell_volume",
    "개인_순매수": "individual_net_volume",
    "개인_매수금": "individual_buy_amount",
    "개인_매도금": "individual_sell_amount",
    "개인_순매수금": "individual_net_amount",
}

_INVESTOR_FLOW_AMOUNT_COLS = {
    "foreign_buy_amount", "foreign_sell_amount", "foreign_net_amount",
    "institution_buy_amount", "institution_sell_amount", "institution_net_amount",
    "finance_invest_buy_amount", "finance_invest_sell_amount", "finance_invest_net_amount",
    "insurance_buy_amount", "insurance_sell_amount", "insurance_net_amount",
    "trust_buy_amount", "trust_sell_amount", "trust_net_amount",
    "private_equity_buy_amount", "private_equity_sell_amount", "private_equity_net_amount",
    "bank_buy_amount", "bank_sell_amount", "bank_net_amount",
    "other_finance_buy_amount", "other_finance_sell_amount", "other_finance_net_amount",
    "pension_buy_amount", "pension_sell_amount", "pension_net_amount",
    "individual_buy_amount", "individual_sell_amount", "individual_net_amount",
}

_SHORT_SELLING_COLUMNS = {
    "티커": "symbol",
    "공매도거래량": "short_sell_volume",
    "공매도거래대금": "short_sell_amount",
    "공매도비율": "short_sell_ratio",
    "공매도잔고수량": "short_balance_volume",
    "공매도잔고금액": "short_balance_amount",
}

_SHORT_SELLING_AMOUNT_COLS = {"short_sell_amount", "short_balance_amount"}
_SHORT_SELLING_DECIMAL_COLS = {"short_sell_amount", "short_balance_amount", "short_sell_ratio"}

_PROGRAM_TRADING_COLUMNS = {
    "티커": "symbol",
    "차익매수": "arb_buy_volume",
    "차익매도": "arb_sell_volume",
    "차익순매수": "arb_net_volume",
    "차익매수금": "arb_buy_amount",
    "차익매도금": "arb_sell_amount",
    "차익순매수금": "arb_net_amount",
    "비차익매수": "non_arb_buy_volume",
    "비차익매도": "non_arb_sell_volume",
    "비차익순매수": "non_arb_net_volume",
    "비차익매수금": "non_arb_buy_amount",
    "비차익매도금": "non_arb_sell_amount",
    "비차익순매수금": "non_arb_net_amount",
}

_PROGRAM_TRADING_AMOUNT_COLS = {
    "arb_buy_amount", "arb_sell_amount", "arb_net_amount",
    "non_arb_buy_amount", "non_arb_sell_amount", "non_arb_net_amount",
}

_FOREIGN_HOLDING_COLUMNS = {
    "티커": "symbol",
    "외국인보유한도": "holding_limit",
    "외국인보유수량": "holding_volume",
    "외국인한도소진율": "exhaustion_ratio",
}

_FOREIGN_HOLDING_DECIMAL_COLS = {"exhaustion_ratio"}


def _to_decimal(value: object) -> Decimal:
    """Convert a numeric value to Decimal without passing through float."""
    if isinstance(value, Decimal):
        return value
    return Decimal(str(value))


class InvestorFlowCollector:
    """Collect investor flow data from pykrx and normalize to domain dicts."""

    def fetch_investor_flow(self, date: str, market: str) -> list[dict]:
        """Fetch investor trading volume by investor type for a given date and market.

        Args:
            date: Date string in YYYYMMDD format.
            market: Market name, e.g. "KOSPI" or "KOSDAQ".

        Returns:
            List of dicts with standardized English column names.
            Returns empty list if no data is available.
        """
        df = stock.get_market_trading_volume_by_investor(date, market, detail=True)
        if df is None or df.empty:
            return []
        return self._normalize_investor_flow(df, date, market)

    def fetch_short_selling(self, date: str, market: str) -> list[dict]:
        """Fetch short selling data for a given date and market.

        Args:
            date: Date string in YYYYMMDD format.
            market: Market name, e.g. "KOSPI" or "KOSDAQ".

        Returns:
            List of dicts with standardized English column names.
            Returns empty list if no data is available.
        """
        df = stock.get_market_short_selling_by_ticker(date, market)
        if df is None or df.empty:
            return []
        return self._normalize_short_selling(df, date, market)

    def fetch_program_trading(self, date: str, market: str) -> list[dict]:
        """Fetch program trading data for a given date and market.

        Args:
            date: Date string in YYYYMMDD format.
            market: Market name, e.g. "KOSPI" or "KOSDAQ".

        Returns:
            List of dicts with standardized English column names.
            Returns empty list if no data is available.
        """
        df = stock.get_market_program_trading_by_date(date, market)
        if df is None or df.empty:
            return []
        return self._normalize_program_trading(df, date, market)

    def fetch_foreign_holding(self, date: str, market: str) -> list[dict]:
        """Fetch foreign holding data for a given date and market.

        Args:
            date: Date string in YYYYMMDD format.
            market: Market name, e.g. "KOSPI" or "KOSDAQ".

        Returns:
            List of dicts with standardized English column names.
            Returns empty list if no data is available.
        """
        df = stock.get_market_cap_by_date(date, market)
        if df is None or df.empty:
            return []
        return self._normalize_foreign_holding(df, date, market)

    # ------------------------------------------------------------------
    # Private normalization methods
    # ------------------------------------------------------------------

    def _normalize_investor_flow(self, df: pd.DataFrame, date: str, market: str) -> list[dict]:
        df = df.reset_index()
        # Rename pykrx columns that exist in the mapping
        rename_map = {k: v for k, v in _INVESTOR_FLOW_COLUMNS.items() if k in df.columns}
        df = df.rename(columns=rename_map)

        records = []
        for row in df.to_dict(orient="records"):
            record: dict = {"trade_date": date, "market": market}
            for col, val in row.items():
                if col not in _INVESTOR_FLOW_COLUMNS.values():
                    continue
                if col in _INVESTOR_FLOW_AMOUNT_COLS:
                    record[col] = _to_decimal(val)
                else:
                    record[col] = int(val) if col.endswith(("_volume",)) else val
            records.append(record)
        return records

    def _normalize_short_selling(self, df: pd.DataFrame, date: str, market: str) -> list[dict]:
        df = df.reset_index()
        rename_map = {k: v for k, v in _SHORT_SELLING_COLUMNS.items() if k in df.columns}
        df = df.rename(columns=rename_map)

        records = []
        for row in df.to_dict(orient="records"):
            record: dict = {"trade_date": date, "market": market}
            for col, val in row.items():
                if col not in _SHORT_SELLING_COLUMNS.values():
                    continue
                if col in _SHORT_SELLING_DECIMAL_COLS:
                    record[col] = _to_decimal(val)
                else:
                    record[col] = val
            records.append(record)
        return records

    def _normalize_program_trading(self, df: pd.DataFrame, date: str, market: str) -> list[dict]:
        df = df.reset_index()
        rename_map = {k: v for k, v in _PROGRAM_TRADING_COLUMNS.items() if k in df.columns}
        df = df.rename(columns=rename_map)

        records = []
        for row in df.to_dict(orient="records"):
            record: dict = {"trade_date": date, "market": market}
            for col, val in row.items():
                if col not in _PROGRAM_TRADING_COLUMNS.values():
                    continue
                if col in _PROGRAM_TRADING_AMOUNT_COLS:
                    record[col] = _to_decimal(val)
                else:
                    record[col] = val
            records.append(record)
        return records

    def _normalize_foreign_holding(self, df: pd.DataFrame, date: str, market: str) -> list[dict]:
        df = df.reset_index()
        rename_map = {k: v for k, v in _FOREIGN_HOLDING_COLUMNS.items() if k in df.columns}
        df = df.rename(columns=rename_map)

        records = []
        for row in df.to_dict(orient="records"):
            record: dict = {"trade_date": date, "market": market}
            for col, val in row.items():
                if col not in _FOREIGN_HOLDING_COLUMNS.values():
                    continue
                if col in _FOREIGN_HOLDING_DECIMAL_COLS:
                    record[col] = _to_decimal(val)
                else:
                    record[col] = val
            records.append(record)
        return records
