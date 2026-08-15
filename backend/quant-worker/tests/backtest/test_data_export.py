from __future__ import annotations

from datetime import date
from decimal import Decimal
from pathlib import Path
from unittest.mock import MagicMock, patch
from zipfile import ZipFile

import pytest

from src.backtest.data_export import (
    BacktestDataExporter,
    DuplicateTradeDateError,
    InvalidOhlcvRowError,
    OhlcvQuery,
    OhlcvResolution,
    PostgresBacktestOhlcvRepository,
)
from src.backtest.domain import Market
from src.backtest.path_safety import UnsafeBacktestPathError
from src.catalog.postgres_symbol_catalog import DbConfig


class RecordingOhlcvRepository:
    def __init__(self, rows: list[dict[str, object]]) -> None:
        self.rows = rows
        self.queries: list[OhlcvQuery] = []

    def fetch_bars(self, query: OhlcvQuery) -> list[dict[str, object]]:
        self.queries.append(query)
        return list(self.rows)


def row(
    trade_date: date,
    *,
    open_price: object = Decimal("100.1234"),
    high_price: object = Decimal("110.25"),
    low_price: object = Decimal(90),
    close_price: object = Decimal("105.5"),
    volume: object = Decimal(1000),
) -> dict[str, object]:
    return {
        "trade_date": trade_date,
        "open_price": open_price,
        "high_price": high_price,
        "low_price": low_price,
        "close_price": close_price,
        "volume": volume,
    }


def export(
    tmp_path: Path,
    repository: RecordingOhlcvRepository,
    *,
    market: Market = Market.US,
    symbol: str = "AAPL",
):
    return BacktestDataExporter(repository).export_daily(
        market=market,
        symbol=symbol,
        start_date=date(2024, 1, 1),
        end_date=date(2024, 1, 31),
        data_root=tmp_path,
    )


def test_daily_export_queries_market_symbol_and_inclusive_date_range(
    tmp_path: Path,
) -> None:
    repository = RecordingOhlcvRepository([row(date(2024, 1, 2))])

    export(tmp_path, repository)

    assert repository.queries == [
        OhlcvQuery(
            market=Market.US,
            symbol="AAPL",
            start_date=date(2024, 1, 1),
            end_date=date(2024, 1, 31),
            resolution=OhlcvResolution.DAILY,
        )
    ]


@pytest.mark.parametrize(
    ("market", "symbol", "market_clause", "expected_params"),
    [
        (
            Market.KR,
            "005930",
            "market IN (%s, %s)",
            [
                "KOSPI",
                "KOSDAQ",
                "005930",
                "pykrx",
                date(2024, 1, 1),
                date(2024, 1, 31),
            ],
        ),
        (
            Market.US,
            "AAPL",
            "market = %s",
            [
                "US",
                "AAPL",
                "yfinance",
                date(2024, 1, 1),
                date(2024, 1, 31),
            ],
        ),
    ],
)
def test_postgres_reader_enforces_raw_source_and_physical_market_policy(
    market: Market,
    symbol: str,
    market_clause: str,
    expected_params: list[object],
) -> None:
    connection = MagicMock()
    cursor = MagicMock()
    connection.__enter__.return_value = connection
    connection.cursor.return_value.__enter__.return_value = cursor
    cursor.fetchall.return_value = []
    repository = PostgresBacktestOhlcvRepository(
        DbConfig(
            host="localhost",
            port=5432,
            database="paper",
            user="paper",
            password="paper",
        )
    )
    query = OhlcvQuery(
        market=market,
        symbol=symbol,
        start_date=date(2024, 1, 1),
        end_date=date(2024, 1, 31),
        resolution=OhlcvResolution.DAILY,
    )

    with patch("src.backtest.data_export.connect", return_value=connection):
        repository.fetch_bars(query)

    sql, params = cursor.execute.call_args.args
    assert market_clause in sql
    assert "source = %s" in sql
    assert "is_adjusted = FALSE" in sql
    assert params == expected_params


def test_us_daily_export_sorts_rows_and_writes_official_scaled_zip_format(
    tmp_path: Path,
) -> None:
    repository = RecordingOhlcvRepository(
        [
            row(
                date(2024, 1, 3),
                open_price=Decimal(200),
                high_price=Decimal(210),
                low_price=Decimal(190),
                close_price=Decimal(205),
                volume=Decimal(2000),
            ),
            row(date(2024, 1, 2)),
        ]
    )

    exported = export(tmp_path, repository)

    assert exported.path == tmp_path / "equity" / "usa" / "daily" / "aapl.zip"
    assert exported.row_count == 2
    with ZipFile(exported.path) as archive:
        assert archive.namelist() == ["aapl.csv"]
        assert archive.getinfo("aapl.csv").date_time == (1980, 1, 1, 0, 0, 0)
        assert archive.read("aapl.csv").decode("utf-8") == (
            "20240102 00:00,1001234,1102500,900000,1055000,1000\n"
            "20240103 00:00,2000000,2100000,1900000,2050000,2000\n"
        )


def test_kr_daily_export_uses_official_krx_market_path(tmp_path: Path) -> None:
    repository = RecordingOhlcvRepository(
        [
            row(
                date(2024, 1, 2),
                open_price=Decimal(70000),
                high_price=Decimal(71000),
                low_price=Decimal(69000),
                close_price=Decimal(70500),
                volume=Decimal(123456),
            )
        ]
    )

    exported = export(
        tmp_path,
        repository,
        market=Market.KR,
        symbol="005930",
    )

    assert exported.path == tmp_path / "equity" / "krx" / "daily" / "005930.zip"
    with ZipFile(exported.path) as archive:
        assert archive.namelist() == ["005930.csv"]


def test_zip_bytes_are_deterministic_across_export_roots(tmp_path: Path) -> None:
    rows = [row(date(2024, 1, 2))]

    first = export(tmp_path / "first", RecordingOhlcvRepository(rows))
    second = export(tmp_path / "second", RecordingOhlcvRepository(rows))

    assert first.path.read_bytes() == second.path.read_bytes()


def test_duplicate_trade_dates_are_rejected_without_writing_zip(tmp_path: Path) -> None:
    repository = RecordingOhlcvRepository(
        [row(date(2024, 1, 2)), row(date(2024, 1, 2))]
    )

    with pytest.raises(DuplicateTradeDateError):
        export(tmp_path, repository)

    assert not (tmp_path / "equity" / "usa" / "daily" / "aapl.zip").exists()


def test_export_rejects_symlinked_parent_without_writing_outside_data_root(
    tmp_path: Path,
) -> None:
    data_root = tmp_path / "data"
    data_root.mkdir()
    outside = tmp_path / "outside"
    outside.mkdir()
    (data_root / "equity").symlink_to(outside, target_is_directory=True)

    with pytest.raises(UnsafeBacktestPathError):
        export(data_root, RecordingOhlcvRepository([row(date(2024, 1, 2))]))

    assert list(outside.iterdir()) == []


@pytest.mark.parametrize(
    "missing_field",
    ["trade_date", "open_price", "high_price", "low_price", "close_price", "volume"],
)
def test_missing_or_null_ohlcv_field_is_rejected(
    tmp_path: Path,
    missing_field: str,
) -> None:
    invalid_row = row(date(2024, 1, 2))
    invalid_row[missing_field] = None

    with pytest.raises(InvalidOhlcvRowError):
        export(tmp_path, RecordingOhlcvRepository([invalid_row]))


def test_float_price_is_rejected_before_lean_boundary(tmp_path: Path) -> None:
    invalid_row = row(date(2024, 1, 2), open_price=100.1234)

    with pytest.raises(InvalidOhlcvRowError):
        export(tmp_path, RecordingOhlcvRepository([invalid_row]))


def test_weekly_resolution_is_preserved_at_repository_extension_seam() -> None:
    repository = RecordingOhlcvRepository([row(date(2024, 1, 5))])
    exporter = BacktestDataExporter(repository)
    query = OhlcvQuery(
        market=Market.US,
        symbol="AAPL",
        start_date=date(2024, 1, 1),
        end_date=date(2024, 1, 31),
        resolution=OhlcvResolution.WEEKLY,
    )

    loaded = exporter.load_bars(query)

    assert loaded[0]["trade_date"] == date(2024, 1, 5)
    assert repository.queries == [query]
