from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from pathlib import Path

import pandas as pd
from pykrx import stock


@dataclass(frozen=True)
class DailyCollectRequest:
    symbol: str
    start_date: date
    end_date: date
    adjusted: bool = False


class PykrxDailyCollector:
    """Collect daily OHLCV data from pykrx and normalize schema."""

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

        from_date = request.start_date.strftime("%Y%m%d")
        to_date = request.end_date.strftime("%Y%m%d")

        # Some pykrx versions support `adjusted`, some do not.
        try:
            raw = stock.get_market_ohlcv_by_date(
                fromdate=from_date,
                todate=to_date,
                ticker=request.symbol,
                freq="d",
                adjusted=request.adjusted,
            )
        except TypeError:
            raw = stock.get_market_ohlcv_by_date(
                fromdate=from_date,
                todate=to_date,
                ticker=request.symbol,
                freq="d",
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
        frame = raw.copy().reset_index()

        frame = frame.rename(
            columns={
                "날짜": "date",
                "시가": "open",
                "고가": "high",
                "저가": "low",
                "종가": "close",
                "거래량": "volume",
            }
        )

        if "date" not in frame.columns:
            raise ValueError("pykrx response does not include date column")

        frame["date"] = pd.to_datetime(frame["date"]).dt.date
        frame["symbol"] = symbol
        frame["adj_close"] = frame["close"]
        frame["source"] = "pykrx"

        frame = frame[self.OUTPUT_COLUMNS].sort_values("date").reset_index(drop=True)
        return frame


