from __future__ import annotations

from dataclasses import dataclass
from datetime import date, timedelta
from pathlib import Path

import pandas as pd
import yfinance as yf


@dataclass(frozen=True)
class DailyCollectRequest:
    symbol: str
    start_date: date
    end_date: date
    auto_adjust: bool = False


class YFinanceDailyCollector:
    """Collect daily OHLCV data from yfinance and normalize schema."""

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

    def fetch(self, request: DailyCollectRequest) -> pd.DataFrame:
        if request.start_date > request.end_date:
            raise ValueError("start_date must be <= end_date")

        raw = yf.download(
            tickers=request.symbol,
            start=request.start_date.isoformat(),
            end=(request.end_date + timedelta(days=1)).isoformat(),
            interval="1d",
            auto_adjust=request.auto_adjust,
            progress=False,
            threads=False,
        )

        if raw.empty:
            return pd.DataFrame(columns=self.OUTPUT_COLUMNS)

        normalized = self._normalize(raw, request.symbol)
        return normalized

    def save_csv(self, dataframe: pd.DataFrame, output_path: str | Path) -> Path:
        path = Path(output_path)
        path.parent.mkdir(parents=True, exist_ok=True)
        dataframe.to_csv(path, index=False)
        return path

    def _normalize(self, raw: pd.DataFrame, symbol: str) -> pd.DataFrame:
        frame = raw.copy()
        frame = frame.reset_index()
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
        frame["symbol"] = symbol
        frame["source"] = "yfinance"

        frame = frame[self.OUTPUT_COLUMNS].sort_values("date").reset_index(drop=True)
        return frame


