from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal

import pandas as pd

from src.catalog.postgres_symbol_catalog import DbConfig, connect


@dataclass(frozen=True)
class OhlcvUpsertContext:
    source: str
    symbol: str
    market: str
    provider: str
    interval: str = "1d"
    is_adjusted: bool = False


class MarketDailyOhlcvRepository:
    def __init__(self, config: DbConfig) -> None:
        self._config = config

    def upsert_daily_rows(
        self,
        frame: pd.DataFrame,
        context: OhlcvUpsertContext,
        chunk_size: int = 500,
    ) -> int:
        if frame.empty:
            return 0

        collected_at = datetime.now()
        rows = [self._to_row(item, context, collected_at) for item in frame.to_dict(orient="records")]
        self._execute_chunks(self._upsert_query(), rows, chunk_size)
        return len(rows)

    def _to_row(self, item: dict[str, object], context: OhlcvUpsertContext, collected_at: datetime) -> tuple[object, ...]:
        trade_date = item["date"]
        open_price = self._to_decimal(item["open"])
        high_price = self._to_decimal(item["high"])
        low_price = self._to_decimal(item["low"])
        close_price = self._to_decimal(item["close"])
        volume = self._to_decimal(item["volume"])
        adj_close = self._to_optional_decimal(item.get("adj_close"))

        return (
            context.source,
            context.symbol,
            context.market,
            trade_date,
            open_price,
            high_price,
            low_price,
            close_price,
            volume,
            adj_close,
            context.provider,
            context.interval,
            context.is_adjusted,
            collected_at,
        )

    def _upsert_query(self) -> str:
        return (
            "INSERT INTO market_daily_ohlcv ("
            "source, symbol, market, trade_date, "
            "open_price, high_price, low_price, close_price, volume, adj_close_price, "
            'provider, "interval", is_adjusted, collected_at'
            ") VALUES ("
            "%s, %s, %s, %s, "
            "%s, %s, %s, %s, %s, %s, "
            "%s, %s, %s, %s"
            ") "
            "ON CONFLICT (source, symbol, trade_date) DO UPDATE SET "
            "market = EXCLUDED.market, "
            "open_price = EXCLUDED.open_price, "
            "high_price = EXCLUDED.high_price, "
            "low_price = EXCLUDED.low_price, "
            "close_price = EXCLUDED.close_price, "
            "volume = EXCLUDED.volume, "
            "adj_close_price = EXCLUDED.adj_close_price, "
            "provider = EXCLUDED.provider, "
            '"interval" = EXCLUDED."interval", '
            "is_adjusted = EXCLUDED.is_adjusted, "
            "collected_at = EXCLUDED.collected_at, "
            "updated_at = CURRENT_TIMESTAMP"
        )

    def _execute_chunks(self, query: str, rows: list[tuple[object, ...]], chunk_size: int) -> None:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                for i in range(0, len(rows), chunk_size):
                    cursor.executemany(query, rows[i : i + chunk_size])
            connection.commit()

    def _connect(self):
        return connect(self._config)

    def _to_decimal(self, value: object) -> Decimal:
        return Decimal(str(value))

    def _to_optional_decimal(self, value: object) -> Decimal | None:
        if value is None:
            return None
        if pd.isna(value):
            return None
        return Decimal(str(value))
