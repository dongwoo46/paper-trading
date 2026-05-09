from __future__ import annotations

import pandas as pd

from src.repositories._indicator_base import IndicatorBaseRepository


class MarketAlternativeFlowRepository(IndicatorBaseRepository):
    def upsert_rows(self, frame: pd.DataFrame, provider: str) -> int:
        now = self._now()
        rows = [
            (
                x["source"],
                x["symbol"],
                x["trade_date"],
                self._to_decimal(x["retail_flow"]),
                self._to_decimal(x["institution_flow"]),
                self._to_decimal(x["foreign_flow"]),
                provider,
                now,
            )
            for x in self._to_records(frame)
        ]
        return self._run_upsert(self._query(), rows)

    def _query(self) -> str:
        return (
            "INSERT INTO market_alternative_flow (source, symbol, trade_date, retail_flow, institution_flow, foreign_flow, provider, collected_at) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s, %s) "
            "ON CONFLICT (source, symbol, trade_date) DO UPDATE SET "
            "retail_flow=EXCLUDED.retail_flow, institution_flow=EXCLUDED.institution_flow, foreign_flow=EXCLUDED.foreign_flow, "
            "provider=EXCLUDED.provider, collected_at=EXCLUDED.collected_at, updated_at=CURRENT_TIMESTAMP"
        )
