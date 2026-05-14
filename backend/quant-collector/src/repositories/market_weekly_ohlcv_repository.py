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
    interval: str = "1wk"
    is_adjusted: bool = False


class MarketWeeklyOhlcvRepository:
    def __init__(self, config: DbConfig) -> None:
        self._config = config

    def upsert_weekly_rows(
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

    def _to_row(
        self,
        item: dict[str, object],
        context: OhlcvUpsertContext,
        collected_at: datetime,
    ) -> tuple[object, ...]:
        return (
            context.source,
            context.symbol,
            context.market,
            item["date"],
            self._to_decimal(item["open"]),
            self._to_decimal(item["high"]),
            self._to_decimal(item["low"]),
            self._to_decimal(item["close"]),
            self._to_decimal(item["volume"]),
            self._to_optional_decimal(item.get("adj_close")),
            context.provider,
            context.interval,
            context.is_adjusted,
            collected_at,
        )

    def _upsert_query(self) -> str:
        return (
            "INSERT INTO market_weekly_ohlcv ("
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

    def find_max_trade_dates(self, source: str) -> dict[str, date]:
        """source별 종목의 마지막 주봉 날짜를 반환한다.

        반환: {symbol: max(trade_date)}
        """
        from datetime import date as date_cls
        query = (
            "SELECT symbol, MAX(trade_date) FROM market_weekly_ohlcv "
            "WHERE source = %s GROUP BY symbol"
        )
        with self._connect() as conn:
            with conn.cursor() as cur:
                cur.execute(query, (source,))
                return {row[0]: row[1] for row in cur.fetchall() if row[1] is not None}

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
