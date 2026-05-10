from __future__ import annotations

import logging
import os
from contextlib import asynccontextmanager
from datetime import date, datetime, timedelta
from typing import Literal

from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel, Field

from src.application.daily_fetch_service import DailyFetchOptions, execute
from src.application.daily_fetch_service import load_db_config_from_env
from src.application.weekly_fetch_service import WeeklyFetchOptions, execute as execute_weekly
from src.catalog.postgres_symbol_catalog import connect
from src.jobs.batch_schedule import start_batch_scheduler, stop_batch_scheduler


class CollectDailyRequest(BaseModel):
    provider: Literal["yfinance", "pykrx", "all"] = "all"
    start: str = "2010-01-01"
    end: str = Field(default_factory=lambda: datetime.now().date().isoformat())
    only_default: bool = False
    auto_adjust: bool = False
    adjusted: bool = False


class CollectDailyResponse(BaseModel):
    provider: str
    symbols: int
    success_symbols: int
    failed_symbols: int
    total_rows_inserted: int
    start: str
    end: str


class CollectWeeklyRequest(BaseModel):
    provider: Literal["yfinance", "pykrx", "all"] = "all"
    start: str = "2010-01-01"
    end: str = Field(default_factory=lambda: datetime.now().date().isoformat())
    only_default: bool = False
    auto_adjust: bool = False
    adjusted: bool = False


class CollectWeeklyResponse(BaseModel):
    provider: str
    symbols: int
    success_symbols: int
    failed_symbols: int
    total_rows_inserted: int
    start: str
    end: str


logger = logging.getLogger(__name__)
_batch_scheduler = None


@asynccontextmanager
async def lifespan(application: FastAPI):
    _configure_logging()
    global _batch_scheduler
    _batch_scheduler = start_batch_scheduler()
    yield
    stop_batch_scheduler(_batch_scheduler)
    _batch_scheduler = None
    logger.info("batch_scheduler:stopped via lifespan")


app = FastAPI(title="Collector Worker API", version="1.0.0", lifespan=lifespan)


class _ColorFormatter(logging.Formatter):
    RESET = "\x1b[0m"
    COLORS = {
        logging.DEBUG: "\x1b[36m",     # cyan
        logging.INFO: "\x1b[32m",      # green
        logging.WARNING: "\x1b[33m",   # yellow
        logging.ERROR: "\x1b[31m",     # red
        logging.CRITICAL: "\x1b[35m",  # magenta
    }

    def format(self, record: logging.LogRecord) -> str:
        color = self.COLORS.get(record.levelno, self.RESET)
        original_levelname = record.levelname
        record.levelname = f"{color}{original_levelname}{self.RESET}"
        try:
            return super().format(record)
        finally:
            record.levelname = original_levelname


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@app.post("/collect/daily", response_model=CollectDailyResponse)
def collect_daily(request: CollectDailyRequest) -> CollectDailyResponse:
    try:
        result = execute(
            DailyFetchOptions(
                provider=request.provider,
                start=request.start,
                end=request.end,
                only_default=request.only_default,
                auto_adjust=request.auto_adjust,
                adjusted=request.adjusted,
            )
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc

    return CollectDailyResponse(**result)


@app.post("/collect/weekly", response_model=CollectWeeklyResponse)
def collect_weekly(request: CollectWeeklyRequest) -> CollectWeeklyResponse:
    try:
        result = execute_weekly(
            WeeklyFetchOptions(
                provider=request.provider,
                start=request.start,
                end=request.end,
                only_default=request.only_default,
                auto_adjust=request.auto_adjust,
                adjusted=request.adjusted,
            )
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    return CollectWeeklyResponse(**result)


@app.get("/market/weekly/{symbol}")
def market_weekly(
    symbol: str,
    source: str = "yfinance",
    from_date: date | None = Query(default=None, alias="from"),
    to: date | None = None,
    limit: int = 260,
) -> list[dict[str, object]]:
    trimmed_symbol = symbol.strip().upper()
    if not trimmed_symbol:
        raise HTTPException(status_code=400, detail="symbol must not be blank")

    safe_to = to or datetime.now().date()
    safe_from = from_date or (safe_to - timedelta(days=365))
    if safe_from > safe_to:
        raise HTTPException(status_code=400, detail="from must be <= to")

    safe_limit = max(1, min(520, limit))
    try:
        return fetch_market_weekly_bars(
            source=source.lower(),
            symbol=trimmed_symbol,
            from_date=safe_from,
            to_date=safe_to,
            limit=safe_limit,
        )
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc


def _configure_logging() -> None:
    root = logging.getLogger()
    if not root.handlers:
        logging.basicConfig(
            level=logging.WARNING,
            format="%(asctime)s %(levelname)s [%(name)s] %(message)s",
        )
    else:
        root.setLevel(logging.WARNING)

    if os.getenv("NO_COLOR", "").lower() not in ("1", "true", "yes", "on"):
        formatter = _ColorFormatter("%(asctime)s %(levelname)s [%(name)s] %(message)s")
        for handler in root.handlers:
            handler.setFormatter(formatter)

    # Keep app/service logs visible without enabling noisy/broken third-party INFO logs.
    logging.getLogger("src").setLevel(logging.INFO)
    logging.getLogger("src.interfaces.api.app").setLevel(logging.INFO)
    logging.getLogger("src.application.daily_fetch_service").setLevel(logging.INFO)


def fetch_market_weekly_bars(
    source: str,
    symbol: str,
    from_date: date,
    to_date: date,
    limit: int,
) -> list[dict[str, object]]:
    db = load_db_config_from_env()
    query = (
        "SELECT source, symbol, market, trade_date, open_price, high_price, low_price, close_price, "
        'volume, adj_close_price, provider, "interval", is_adjusted, collected_at '
        "FROM market_weekly_ohlcv "
        "WHERE source = %s AND symbol = %s AND trade_date BETWEEN %s AND %s "
        "ORDER BY trade_date ASC "
        "LIMIT %s"
    )
    with connect(db) as conn:
        with conn.cursor() as cursor:
            cursor.execute(query, [source, symbol, from_date, to_date, limit])
            rows = cursor.fetchall()
    return [
        {
            "source": row[0],
            "symbol": row[1],
            "market": row[2],
            "trade_date": row[3],
            "open_price": row[4],
            "high_price": row[5],
            "low_price": row[6],
            "close_price": row[7],
            "volume": row[8],
            "adj_close_price": row[9],
            "provider": row[10],
            "interval": row[11],
            "is_adjusted": row[12],
            "collected_at": row[13],
        }
        for row in rows
    ]
