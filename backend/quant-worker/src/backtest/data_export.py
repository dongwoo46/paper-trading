from __future__ import annotations

import re
from collections.abc import Mapping
from dataclasses import dataclass
from datetime import date, datetime
from decimal import Decimal
from enum import Enum
from pathlib import Path
from typing import ClassVar, Protocol
from zipfile import ZIP_DEFLATED, ZipFile, ZipInfo

from src.backtest.domain import Market
from src.backtest.path_safety import (
    UnsafeBacktestPathError,
    create_confined_directory,
    require_confined_directory,
)
from src.catalog.postgres_symbol_catalog import DbConfig, connect


class OhlcvResolution(str, Enum):
    DAILY = "daily"
    WEEKLY = "weekly"


@dataclass(frozen=True)
class OhlcvQuery:
    market: Market
    symbol: str
    start_date: date
    end_date: date
    resolution: OhlcvResolution


class OhlcvReadRepository(Protocol):
    def fetch_bars(self, query: OhlcvQuery) -> list[Mapping[str, object]]: ...


class InvalidOhlcvRowError(ValueError):
    pass


class DuplicateTradeDateError(ValueError):
    pass


class NoOhlcvDataError(ValueError):
    pass


@dataclass(frozen=True)
class ExportedLeanData:
    path: Path
    row_count: int
    query: OhlcvQuery


class PostgresBacktestOhlcvRepository:
    _TABLE_BY_RESOLUTION: ClassVar[dict[OhlcvResolution, str]] = {
        OhlcvResolution.DAILY: "market_daily_ohlcv",
        OhlcvResolution.WEEKLY: "market_weekly_ohlcv",
    }
    _SOURCE_BY_MARKET: ClassVar[dict[Market, str]] = {
        Market.KR: "pykrx",
        Market.US: "yfinance",
    }

    def __init__(self, config: DbConfig) -> None:
        self._config = config

    def fetch_bars(self, query: OhlcvQuery) -> list[Mapping[str, object]]:
        table = self._TABLE_BY_RESOLUTION[query.resolution]
        if query.market is Market.KR:
            market_predicate = "market IN (%s, %s)"
            params: list[object] = ["KOSPI", "KOSDAQ"]
        else:
            market_predicate = "market = %s"
            params = ["US"]
        sql = (
            "SELECT trade_date, open_price, high_price, low_price, close_price, volume "
            f"FROM {table} "
            f"WHERE {market_predicate} AND symbol = %s "
            "AND source = %s AND is_adjusted = FALSE "
            "AND trade_date BETWEEN %s AND %s "
            "ORDER BY trade_date ASC"
        )
        params.extend(
            [
                query.symbol,
                self._SOURCE_BY_MARKET[query.market],
                query.start_date,
                query.end_date,
            ]
        )
        with connect(self._config) as connection, connection.cursor() as cursor:
            cursor.execute(sql, params)
            rows = cursor.fetchall()
        return [
            {
                "trade_date": row[0],
                "open_price": row[1],
                "high_price": row[2],
                "low_price": row[3],
                "close_price": row[4],
                "volume": row[5],
            }
            for row in rows
        ]


class BacktestDataExporter:
    _REQUIRED_FIELDS = (
        "trade_date",
        "open_price",
        "high_price",
        "low_price",
        "close_price",
        "volume",
    )
    _PRICE_FIELDS = (
        "open_price",
        "high_price",
        "low_price",
        "close_price",
    )
    _PRICE_SCALE = Decimal(10000)
    _LEAN_MARKET_BY_MARKET: ClassVar[dict[Market, str]] = {
        Market.KR: "krx",
        Market.US: "usa",
    }

    def __init__(self, repository: OhlcvReadRepository) -> None:
        self._repository = repository

    def load_bars(self, query: OhlcvQuery) -> list[Mapping[str, object]]:
        return self._repository.fetch_bars(query)

    def export_daily(
        self,
        *,
        market: Market,
        symbol: str,
        start_date: date,
        end_date: date,
        data_root: Path,
    ) -> ExportedLeanData:
        normalized_symbol = self._normalize_symbol(market, symbol)
        if start_date > end_date:
            raise ValueError("start_date must be on or before end_date")
        query = OhlcvQuery(
            market=market,
            symbol=normalized_symbol,
            start_date=start_date,
            end_date=end_date,
            resolution=OhlcvResolution.DAILY,
        )
        rows = self.load_bars(query)
        normalized_rows = [self._normalize_row(row) for row in rows]
        if not normalized_rows:
            raise NoOhlcvDataError(
                f"no daily OHLCV data for {market.value}:{normalized_symbol}"
            )
        normalized_rows.sort(key=lambda row: row["trade_date"])
        trade_dates = [row["trade_date"] for row in normalized_rows]
        if len(trade_dates) != len(set(trade_dates)):
            raise DuplicateTradeDateError(
                f"duplicate trade_date for {market.value}:{normalized_symbol}"
            )

        ticker = normalized_symbol.lower()
        lean_market = self._LEAN_MARKET_BY_MARKET[market]
        if data_root.exists():
            require_confined_directory(data_root, data_root)
        else:
            data_root.mkdir(parents=True, exist_ok=False)
            require_confined_directory(data_root, data_root)
        destination = data_root / "equity" / lean_market / "daily" / f"{ticker}.zip"
        create_confined_directory(data_root, destination.parent)
        if destination.exists() or destination.is_symlink():
            raise UnsafeBacktestPathError(
                f"LEAN data destination already exists: {destination}"
            )
        payload = "".join(self._lean_csv_line(row) for row in normalized_rows).encode(
            "utf-8"
        )
        archive_member = ZipInfo(f"{ticker}.csv", date_time=(1980, 1, 1, 0, 0, 0))
        archive_member.compress_type = ZIP_DEFLATED
        archive_member.create_system = 3
        archive_member.external_attr = 0o600 << 16
        with ZipFile(
            destination,
            mode="x",
            compression=ZIP_DEFLATED,
            compresslevel=9,
        ) as archive:
            archive.writestr(
                archive_member,
                payload,
                compress_type=ZIP_DEFLATED,
                compresslevel=9,
            )
        return ExportedLeanData(
            path=destination,
            row_count=len(normalized_rows),
            query=query,
        )

    @classmethod
    def _normalize_row(
        cls,
        row: Mapping[str, object],
    ) -> dict[str, date | Decimal]:
        missing = [field for field in cls._REQUIRED_FIELDS if row.get(field) is None]
        if missing:
            raise InvalidOhlcvRowError(
                f"OHLCV row has missing fields: {', '.join(missing)}"
            )

        trade_date = row["trade_date"]
        if not isinstance(trade_date, date) or isinstance(trade_date, datetime):
            raise InvalidOhlcvRowError("trade_date must be a date")

        normalized: dict[str, date | Decimal] = {"trade_date": trade_date}
        for field in (*cls._PRICE_FIELDS, "volume"):
            value = row[field]
            if not isinstance(value, Decimal):
                raise InvalidOhlcvRowError(f"{field} must be Decimal")
            if value < 0:
                raise InvalidOhlcvRowError(f"{field} must not be negative")
            normalized[field] = value
        return normalized

    @classmethod
    def _lean_csv_line(cls, row: Mapping[str, date | Decimal]) -> str:
        trade_date = row["trade_date"]
        if not isinstance(trade_date, date):
            raise InvalidOhlcvRowError("trade_date must be a date")
        prices = [
            cls._scaled_integer(row[field], field)
            for field in cls._PRICE_FIELDS
        ]
        volume = cls._whole_number(row["volume"], "volume")
        return (
            f"{trade_date:%Y%m%d} 00:00,"
            f"{','.join([*prices, volume])}\n"
        )

    @classmethod
    def _scaled_integer(cls, value: date | Decimal, field: str) -> str:
        if not isinstance(value, Decimal):
            raise InvalidOhlcvRowError(f"{field} must be Decimal")
        return cls._whole_number(value * cls._PRICE_SCALE, field)

    @staticmethod
    def _whole_number(value: date | Decimal, field: str) -> str:
        if not isinstance(value, Decimal):
            raise InvalidOhlcvRowError(f"{field} must be Decimal")
        integral = value.to_integral_value()
        if integral != value:
            raise InvalidOhlcvRowError(
                f"{field} cannot be represented as a LEAN integer without rounding"
            )
        return format(integral, "f")

    @staticmethod
    def _normalize_symbol(market: Market, symbol: str) -> str:
        normalized = symbol.strip().upper()
        if market is Market.KR:
            if re.fullmatch(r"\d{6}", normalized) is None:
                raise ValueError("KR symbols must contain exactly six digits")
        elif re.fullmatch(r"[A-Z][A-Z0-9.-]{0,9}", normalized) is None:
            raise ValueError("US symbols must be a valid uppercase ticker")
        return normalized
