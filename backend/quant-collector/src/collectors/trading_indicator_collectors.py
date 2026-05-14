from __future__ import annotations

from datetime import date

import pandas as pd


class YFinanceTradingIndicatorCollector:
    def collect(self) -> dict[str, list[dict[str, object]]]:
        return {
            "microstructure": [],
            "session_ohlcv": [],
            "relative_strength": [],
            "alternative_flow": [],
            "metadata": [],
        }

    def normalize_relative_strength(self, raw: pd.DataFrame, symbol: str) -> pd.DataFrame:
        frame = raw.rename(columns={"Date": "trade_date", "Close": "close_price", "Volume": "volume"}).copy()
        frame["trade_date"] = pd.to_datetime(frame["trade_date"]).dt.date
        frame["symbol"] = symbol
        frame["source"] = "yfinance"
        return frame[["trade_date", "symbol", "close_price", "volume", "source"]]


class PykrxTradingIndicatorCollector:
    def collect(self) -> dict[str, list[dict[str, object]]]:
        return {
            "microstructure": [],
            "session_ohlcv": [],
            "relative_strength": [],
            "alternative_flow": [],
            "metadata": [],
        }

    def normalize_alternative_flow(self, raw: pd.DataFrame, symbol: str) -> pd.DataFrame:
        frame = raw.rename(
            columns={
                "날짜": "trade_date",
                "개인": "retail_flow",
                "기관합계": "institution_flow",
                "외국인합계": "foreign_flow",
            }
        ).copy()
        frame["trade_date"] = pd.to_datetime(frame["trade_date"]).dt.date
        frame["symbol"] = symbol
        frame["source"] = "pykrx"
        return frame[["trade_date", "symbol", "retail_flow", "institution_flow", "foreign_flow", "source"]]

    def normalize_metadata(self, raw: pd.DataFrame) -> pd.DataFrame:
        frame = raw.copy()
        frame["source"] = "pykrx"
        return frame[["symbol", "name", "market", "currency", "source"]]
