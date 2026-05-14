from __future__ import annotations

from dataclasses import dataclass
from datetime import date

import pandas as pd

from src.collectors.kis_client import KisClient, iter_date_chunks


@dataclass(frozen=True)
class WeeklyCollectRequest:
    symbol: str
    start_date: date
    end_date: date
    adjusted: bool = False


class KisWeeklyCollector:
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

    def __init__(self, client: KisClient | None = None, chunk_days: int = 700) -> None:
        self._client = client or KisClient()
        self._chunk_days = chunk_days

    def fetch(self, request: WeeklyCollectRequest) -> pd.DataFrame:
        if request.start_date > request.end_date:
            raise ValueError("start_date must be <= end_date")

        rows: list[dict[str, object]] = []
        for start, end in iter_date_chunks(request.start_date, request.end_date, self._chunk_days):
            rows.extend(
                self._client.fetch_period_price(
                    symbol=request.symbol,
                    start_date=start,
                    end_date=end,
                    period_div_code="W",
                    adjusted=request.adjusted,
                )
            )
        return self._normalize(rows, request.symbol)

    def _normalize(self, rows: list[dict[str, object]], symbol: str) -> pd.DataFrame:
        if not rows:
            return pd.DataFrame(columns=self.OUTPUT_COLUMNS)
        frame = pd.DataFrame(rows)
        frame = frame.rename(
            columns={
                "stck_bsop_date": "date",
                "stck_oprc": "open",
                "stck_hgpr": "high",
                "stck_lwpr": "low",
                "stck_clpr": "close",
                "acml_vol": "volume",
            }
        )
        required = ["date", "open", "high", "low", "close", "volume"]
        missing = [col for col in required if col not in frame.columns]
        if missing:
            raise RuntimeError(f"KIS weekly response missing columns: {', '.join(missing)}")

        frame["date"] = pd.to_datetime(frame["date"], format="%Y%m%d", errors="coerce").dt.date
        for col in ("open", "high", "low", "close", "volume"):
            frame[col] = pd.to_numeric(frame[col], errors="coerce")

        frame = frame.dropna(subset=required)
        frame["symbol"] = symbol
        frame["adj_close"] = frame["close"]
        frame["source"] = "kis"
        return (
            frame[self.OUTPUT_COLUMNS]
            .drop_duplicates(subset=["date"], keep="last")
            .sort_values("date")
            .reset_index(drop=True)
        )
