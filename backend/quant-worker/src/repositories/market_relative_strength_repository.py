from __future__ import annotations

import pandas as pd

from src.repositories._indicator_base import IndicatorBaseRepository


class MarketRelativeStrengthRepository(IndicatorBaseRepository):
    def upsert_rows(self, frame: pd.DataFrame, provider: str) -> int:
        now = self._now()
        rows = [
            (
                x["source"],
                x["symbol"],
                x["trade_date"],
                self._to_decimal(x["close_price"]),
                self._to_decimal(x["volume"]),
                provider,
                now,
            )
            for x in self._to_records(frame)
        ]
        return self._run_upsert(self._query(), rows)

    def _query(self) -> str:
        return (
            "INSERT INTO market_relative_strength (source, symbol, trade_date, close_price, volume, provider, collected_at) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s) "
            "ON CONFLICT (source, symbol, trade_date) DO UPDATE SET "
            "close_price=EXCLUDED.close_price, volume=EXCLUDED.volume, provider=EXCLUDED.provider, "
            "collected_at=EXCLUDED.collected_at, updated_at=CURRENT_TIMESTAMP"
        )
