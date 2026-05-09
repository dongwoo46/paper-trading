from __future__ import annotations

from datetime import datetime
from decimal import Decimal

import pandas as pd

from src.catalog.postgres_symbol_catalog import DbConfig, connect


class IndicatorBaseRepository:
    def __init__(self, config: DbConfig) -> None:
        self._config = config

    def _connect(self):
        return connect(self._config)

    def _to_decimal(self, value: object) -> Decimal:
        return Decimal(str(value))

    def _run_upsert(self, query: str, rows: list[tuple[object, ...]], chunk_size: int = 500) -> int:
        if not rows:
            return 0
        with self._connect() as connection:
            with connection.cursor() as cursor:
                for i in range(0, len(rows), chunk_size):
                    cursor.executemany(query, rows[i : i + chunk_size])
            connection.commit()
        return len(rows)

    def _now(self) -> datetime:
        return datetime.now()

    def _to_records(self, frame: pd.DataFrame) -> list[dict[str, object]]:
        if frame.empty:
            return []
        return frame.to_dict(orient="records")
