from __future__ import annotations

from dataclasses import dataclass
from datetime import date, timedelta

import pandas as pd
import yfinance as yf


@dataclass(frozen=True)
class WeeklyCollectRequest:
    symbol: str
    start_date: date
    end_date: date
    auto_adjust: bool = False


class YFinanceWeeklyCollector:
    OUTPUT_COLUMNS = [
        "date",
        "symbol",
        "open",
        "high",
        "low",
        "close",
        "adj_close",
        "volume",
        "source",
    ]

    def fetch(self, request: WeeklyCollectRequest) -> pd.DataFrame:
        if request.start_date > request.end_date:
            raise ValueError("start_date must be <= end_date")

        raw = yf.download(
            tickers=request.symbol,
            start=request.start_date.isoformat(),
            end=(request.end_date + timedelta(days=1)).isoformat(),
            interval="1wk",
            auto_adjust=request.auto_adjust,
            progress=False,
            threads=False,
        )

        if raw.empty:
            return pd.DataFrame(columns=self.OUTPUT_COLUMNS)

        frame = raw.reset_index()
        if isinstance(frame.columns, pd.MultiIndex):
            frame.columns = [col[0] for col in frame.columns]
        if "Date" not in frame.columns:
            raise ValueError("yfinance response does not include Date column")
        frame = frame.rename(
            columns={
                "Date": "date",
                "Open": "open",
                "High": "high",
                "Low": "low",
                "Close": "close",
                "Adj Close": "adj_close",
                "Volume": "volume",
            }
        )
        if "adj_close" not in frame.columns:
            frame["adj_close"] = frame["close"]
        frame["date"] = pd.to_datetime(frame["date"]).dt.date
        frame["symbol"] = request.symbol
        frame["source"] = "yfinance"
        return frame[self.OUTPUT_COLUMNS].sort_values("date").reset_index(drop=True)
