from __future__ import annotations

import pandas as pd

from src.repositories._indicator_base import IndicatorBaseRepository


class MarketMetadataRepository(IndicatorBaseRepository):
    def upsert_rows(self, frame: pd.DataFrame, provider: str) -> int:
        now = self._now()
        rows = [(x["source"], x["symbol"], x["name"], x["market"], x["currency"], provider, now) for x in self._to_records(frame)]
        return self._run_upsert(self._query(), rows)

    def _query(self) -> str:
        return (
            "INSERT INTO market_symbol_metadata (source, symbol, name, market, currency, provider, collected_at) "
            "VALUES (%s, %s, %s, %s, %s, %s, %s) "
            "ON CONFLICT (source, symbol) DO UPDATE SET "
            "name=EXCLUDED.name, market=EXCLUDED.market, currency=EXCLUDED.currency, provider=EXCLUDED.provider, "
            "collected_at=EXCLUDED.collected_at, updated_at=CURRENT_TIMESTAMP"
        )
