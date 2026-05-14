from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime
from typing import Iterable

from src.catalog.models import CatalogSymbol


@dataclass(frozen=True)
class DbConfig:
    host: str
    port: int
    database: str
    user: str
    password: str


def connect(config: DbConfig):
    """Open a psycopg connection from a DbConfig. Shared by all repositories."""
    try:
        import psycopg
    except ImportError as exc:
        raise RuntimeError(
            "psycopg is required. Install with `pip install psycopg[binary]`."
        ) from exc
    return psycopg.connect(
        host=config.host,
        port=config.port,
        dbname=config.database,
        user=config.user,
        password=config.password,
    )


class PostgresSymbolCatalogRepository:
    """Read provider symbol catalogs from PostgreSQL."""

    TABLE_BY_PROVIDER = {
        "yfinance": "yfinance_symbol_catalog",
        "pykrx": "pykrx_symbol_catalog",
    }

    def __init__(self, config: DbConfig) -> None:
        self._config = config

    def list_symbols(self, provider: str, only_default: bool = False) -> list[CatalogSymbol]:
        table_name = self._table_name(provider)
        where = "enabled = TRUE"
        if only_default:
            where += " AND is_default = TRUE"

        query = (
            f"SELECT "
            f"{self._symbol_column(provider)} AS symbol, "
            f"name, market, enabled, is_default, fetched_until_date, last_collected_at "
            f"FROM {table_name} "
            f"WHERE {where} "
            f"ORDER BY id ASC"
        )

        rows = self._fetch_all(query, [])
        return [self._row_to_symbol(row) for row in rows]

    def update_collection_status(
        self,
        provider: str,
        symbol: str,
        fetched_until_date: date,
        collected_at: datetime,
    ) -> None:
        table_name = self._table_name(provider)
        symbol_column = self._symbol_column(provider)
        query = (
            f"UPDATE {table_name} "
            f"SET fetched_until_date = GREATEST(COALESCE(fetched_until_date, %s), %s), "
            f"last_collected_at = %s "
            f"WHERE {symbol_column} = %s"
        )
        baseline = date(1900, 1, 1)
        self._execute(query, [baseline, fetched_until_date, collected_at, symbol])

    def _table_name(self, provider: str) -> str:
        if provider not in self.TABLE_BY_PROVIDER:
            raise ValueError(f"Unsupported provider: {provider}")
        return self.TABLE_BY_PROVIDER[provider]

    def _symbol_column(self, provider: str) -> str:
        return "ticker" if provider == "yfinance" else "symbol"

    def _fetch_all(self, query: str, params: Iterable[object]) -> list[tuple[object, ...]]:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(query, params)
                return cursor.fetchall()

    def _execute(self, query: str, params: Iterable[object]) -> None:
        with self._connect() as connection:
            with connection.cursor() as cursor:
                cursor.execute(query, params)
            connection.commit()

    def _connect(self):
        return connect(self._config)

    def _row_to_symbol(self, row: tuple[object, ...]) -> CatalogSymbol:
        symbol, name, market, enabled, is_default, fetched_until_date, last_collected_at = row
        return CatalogSymbol(
            symbol=str(symbol),
            name=str(name),
            market=str(market),
            enabled=bool(enabled),
            is_default=bool(is_default),
            fetched_until_date=fetched_until_date,
            last_collected_at=last_collected_at,
        )
