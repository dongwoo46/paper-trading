from __future__ import annotations

from dataclasses import dataclass
from datetime import date

import pandas as pd
from pykrx import stock


@dataclass(frozen=True)
class WeeklyCollectRequest:
    symbol: str
    start_date: date
    end_date: date
    adjusted: bool = False


class PykrxWeeklyCollector:
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

        from_date = request.start_date.strftime("%Y%m%d")
        to_date = request.end_date.strftime("%Y%m%d")

        try:
            raw = stock.get_market_ohlcv_by_date(
                fromdate=from_date,
                todate=to_date,
                ticker=request.symbol,
                freq="w",
                adjusted=request.adjusted,
            )
        except TypeError:
            raw = stock.get_market_ohlcv_by_date(
                fromdate=from_date,
                todate=to_date,
                ticker=request.symbol,
                freq="w",
            )

        if raw.empty:
            return pd.DataFrame(columns=self.OUTPUT_COLUMNS)

        frame = raw.copy().reset_index().rename(
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
        frame["symbol"] = request.symbol
        frame["adj_close"] = frame["close"]
        frame["source"] = "pykrx"
        return frame[self.OUTPUT_COLUMNS].sort_values("date").reset_index(drop=True)

