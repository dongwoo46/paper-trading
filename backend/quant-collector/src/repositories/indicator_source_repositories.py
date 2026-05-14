from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal

import pandas as pd

from src.catalog.postgres_symbol_catalog import DbConfig, connect


class _BaseRepository:
    table_name = ""
    columns: list[str] = []

    def __init__(self, config: DbConfig) -> None:
        self._config = config

    def upsert(self, frame: pd.DataFrame, chunk_size: int = 500) -> int:
        if frame.empty:
            return 0
        collected_at = datetime.now()
        rows = [self._to_row(item, collected_at) for item in frame.to_dict(orient="records")]
        with connect(self._config) as conn:
            with conn.cursor() as cur:
                for i in range(0, len(rows), chunk_size):
                    cur.executemany(self._query(), rows[i : i + chunk_size])
            conn.commit()
        return len(rows)

    def _to_decimal(self, value: object) -> Decimal:
        return Decimal(str(value))

    def _to_optional_decimal(self, value: object) -> Decimal | None:
        if value is None or pd.isna(value):
            return None
        return Decimal(str(value))


class MicrostructureSourceRepository(_BaseRepository):
    def _to_row(self, item: dict[str, object], collected_at: datetime):
        return (
            item["source"], item["symbol"], item["as_of"], self._to_optional_decimal(item.get("bid")), self._to_optional_decimal(item.get("ask")),
            self._to_optional_decimal(item.get("bid_size")), self._to_optional_decimal(item.get("ask_size")), self._to_optional_decimal(item.get("spread")),
            item["bid_ask_available"], item["depth_available"], item.get("availability_note"), collected_at
        )

    def _query(self) -> str:
        return "INSERT INTO market_microstructure_source (source, symbol, as_of, bid_price, ask_price, bid_size, ask_size, spread, bid_ask_available, depth_available, availability_note, collected_at) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) ON CONFLICT (source, symbol, as_of) DO UPDATE SET bid_price=EXCLUDED.bid_price, ask_price=EXCLUDED.ask_price, bid_size=EXCLUDED.bid_size, ask_size=EXCLUDED.ask_size, spread=EXCLUDED.spread, bid_ask_available=EXCLUDED.bid_ask_available, depth_available=EXCLUDED.depth_available, availability_note=EXCLUDED.availability_note, collected_at=EXCLUDED.collected_at, updated_at=CURRENT_TIMESTAMP"


class SessionOhlcvSourceRepository(_BaseRepository):
    def _to_row(self, item: dict[str, object], collected_at: datetime):
        return (item["source"], item["symbol"], item["session"], item["as_of"], self._to_decimal(item["open"]), self._to_decimal(item["high"]), self._to_decimal(item["low"]), self._to_decimal(item["close"]), self._to_decimal(item["volume"]), collected_at)

    def _query(self) -> str:
        return "INSERT INTO market_session_ohlcv_source (source, symbol, session, as_of, open_price, high_price, low_price, close_price, volume, collected_at) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s) ON CONFLICT (source, symbol, session, as_of) DO UPDATE SET open_price=EXCLUDED.open_price, high_price=EXCLUDED.high_price, low_price=EXCLUDED.low_price, close_price=EXCLUDED.close_price, volume=EXCLUDED.volume, collected_at=EXCLUDED.collected_at, updated_at=CURRENT_TIMESTAMP"


class RelativeStrengthSourceRepository(_BaseRepository):
    def _to_row(self, item: dict[str, object], collected_at: datetime):
        return (item["source"], item["symbol"], item["benchmark_symbol"], item["as_of"], self._to_decimal(item["symbol_return"]), self._to_decimal(item["benchmark_return"]), self._to_optional_decimal(item.get("relative_strength")), collected_at)

    def _query(self) -> str:
        return "INSERT INTO market_relative_strength_source (source, symbol, benchmark_symbol, as_of, symbol_return, benchmark_return, relative_strength, collected_at) VALUES (%s,%s,%s,%s,%s,%s,%s,%s) ON CONFLICT (source, symbol, benchmark_symbol, as_of) DO UPDATE SET symbol_return=EXCLUDED.symbol_return, benchmark_return=EXCLUDED.benchmark_return, relative_strength=EXCLUDED.relative_strength, collected_at=EXCLUDED.collected_at, updated_at=CURRENT_TIMESTAMP"


class AlternativeFlowSourceRepository(_BaseRepository):
    def _to_row(self, item: dict[str, object], collected_at: datetime):
        return (item["source"], item["symbol"], item["as_of"], self._to_optional_decimal(item.get("short_interest")), self._to_optional_decimal(item.get("days_to_cover")), self._to_optional_decimal(item.get("shares_outstanding")), self._to_optional_decimal(item.get("float_shares")), collected_at)

    def _query(self) -> str:
        return "INSERT INTO market_flow_alternative_source (source, symbol, as_of, short_interest, days_to_cover, shares_outstanding, float_shares, collected_at) VALUES (%s,%s,%s,%s,%s,%s,%s,%s) ON CONFLICT (source, symbol, as_of) DO UPDATE SET short_interest=EXCLUDED.short_interest, days_to_cover=EXCLUDED.days_to_cover, shares_outstanding=EXCLUDED.shares_outstanding, float_shares=EXCLUDED.float_shares, collected_at=EXCLUDED.collected_at, updated_at=CURRENT_TIMESTAMP"


class MetadataSourceRepository(_BaseRepository):
    def _to_row(self, item: dict[str, object], collected_at: datetime):
        return (item["source"], item["symbol"], item["as_of"], item["exchange"], item["currency"], item["timezone"], self._to_optional_decimal(item.get("market_cap")), self._to_optional_decimal(item.get("free_float")), collected_at)

    def _query(self) -> str:
        return "INSERT INTO market_symbol_metadata (source, symbol, as_of, exchange, currency, timezone, market_cap, free_float, collected_at) VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s) ON CONFLICT (source, symbol, as_of) DO UPDATE SET exchange=EXCLUDED.exchange, currency=EXCLUDED.currency, timezone=EXCLUDED.timezone, market_cap=EXCLUDED.market_cap, free_float=EXCLUDED.free_float, collected_at=EXCLUDED.collected_at, updated_at=CURRENT_TIMESTAMP"


@dataclass(frozen=True)
class RepositoryBundle:
    microstructure: MicrostructureSourceRepository
    session_ohlcv: SessionOhlcvSourceRepository
    relative_strength: RelativeStrengthSourceRepository
    alternative_flow: AlternativeFlowSourceRepository
    metadata: MetadataSourceRepository
