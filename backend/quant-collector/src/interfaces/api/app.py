from __future__ import annotations

import logging
import os
from contextlib import asynccontextmanager
from datetime import date, datetime, timedelta
from decimal import Decimal
from typing import Literal

from fastapi import FastAPI, HTTPException, Query
from pydantic import BaseModel, Field

from src.application.daily_fetch_service import DailyFetchOptions, execute
from src.application.daily_fetch_service import load_db_config_from_env
from src.application.weekly_fetch_service import WeeklyFetchOptions, execute as execute_weekly
from src.catalog.postgres_symbol_catalog import connect
from src.jobs.batch_schedule import start_batch_scheduler, stop_batch_scheduler
from src.jobs.investor_flow_schedule import start_investor_flow_scheduler, stop_investor_flow_scheduler


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
_investor_flow_scheduler = None


@asynccontextmanager
async def lifespan(application: FastAPI):
    _configure_logging()
    global _batch_scheduler, _investor_flow_scheduler
    _batch_scheduler = start_batch_scheduler()
    _investor_flow_scheduler = start_investor_flow_scheduler()
    yield
    stop_batch_scheduler(_batch_scheduler)
    _batch_scheduler = None
    stop_investor_flow_scheduler(_investor_flow_scheduler)
    _investor_flow_scheduler = None
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


@app.get("/symbols")
def list_catalog_symbols(provider: str = "all") -> dict:
    """카탈로그에 등록된 종목 목록 반환 (symbol, name, market, provider)."""
    from src.catalog.postgres_symbol_catalog import PostgresSymbolCatalogRepository

    db = load_db_config_from_env()
    repo = PostgresSymbolCatalogRepository(config=db)

    providers = ["yfinance", "pykrx"] if provider == "all" else [provider]
    result = []
    for p in providers:
        try:
            for s in repo.list_symbols(p):
                result.append({
                    "symbol": s.symbol,
                    "name": s.name,
                    "market": s.market,
                    "provider": p,
                })
        except Exception:  # noqa: BLE001
            pass

    return {"symbols": result}


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


# ---------------------------------------------------------------------------
# Investor flow — Pydantic response models
# ---------------------------------------------------------------------------


class InvestorFlowRecord(BaseModel):
    trade_date: str
    symbol: str
    market: str
    foreign_buy_volume: int
    foreign_sell_volume: int
    foreign_net_volume: int
    foreign_buy_amount: str
    foreign_sell_amount: str
    foreign_net_amount: str
    institution_buy_volume: int
    institution_sell_volume: int
    institution_net_volume: int
    institution_buy_amount: str
    institution_sell_amount: str
    institution_net_amount: str
    finance_invest_buy_volume: int
    finance_invest_sell_volume: int
    finance_invest_net_volume: int
    finance_invest_buy_amount: str
    finance_invest_sell_amount: str
    finance_invest_net_amount: str
    insurance_buy_volume: int
    insurance_sell_volume: int
    insurance_net_volume: int
    insurance_buy_amount: str
    insurance_sell_amount: str
    insurance_net_amount: str
    trust_buy_volume: int
    trust_sell_volume: int
    trust_net_volume: int
    trust_buy_amount: str
    trust_sell_amount: str
    trust_net_amount: str
    private_equity_buy_volume: int
    private_equity_sell_volume: int
    private_equity_net_volume: int
    private_equity_buy_amount: str
    private_equity_sell_amount: str
    private_equity_net_amount: str
    bank_buy_volume: int
    bank_sell_volume: int
    bank_net_volume: int
    bank_buy_amount: str
    bank_sell_amount: str
    bank_net_amount: str
    other_finance_buy_volume: int
    other_finance_sell_volume: int
    other_finance_net_volume: int
    other_finance_buy_amount: str
    other_finance_sell_amount: str
    other_finance_net_amount: str
    pension_buy_volume: int
    pension_sell_volume: int
    pension_net_volume: int
    pension_buy_amount: str
    pension_sell_amount: str
    pension_net_amount: str
    individual_buy_volume: int
    individual_sell_volume: int
    individual_net_volume: int
    individual_buy_amount: str
    individual_sell_amount: str
    individual_net_amount: str


class ShortSellingRecord(BaseModel):
    trade_date: str
    symbol: str
    market: str
    short_sell_volume: int
    short_sell_amount: str
    short_sell_ratio: str
    short_balance_volume: int
    short_balance_amount: str


class ProgramTradingRecord(BaseModel):
    trade_date: str
    symbol: str
    market: str
    arb_buy_volume: int
    arb_sell_volume: int
    arb_net_volume: int
    arb_buy_amount: str
    arb_sell_amount: str
    arb_net_amount: str
    non_arb_buy_volume: int
    non_arb_sell_volume: int
    non_arb_net_volume: int
    non_arb_buy_amount: str
    non_arb_sell_amount: str
    non_arb_net_amount: str


class ForeignHoldingRecord(BaseModel):
    trade_date: str
    symbol: str
    market: str
    holding_limit: int
    holding_volume: int
    exhaustion_ratio: str


# ---------------------------------------------------------------------------
# Investor flow — helper to validate / normalize common query parameters
# ---------------------------------------------------------------------------


def _validate_symbol_and_dates(
    symbol: str,
    from_date: date | None,
    to_date: date | None,
    limit: int,
) -> tuple[str, date, date, int]:
    trimmed = symbol.strip().upper()
    if not trimmed:
        raise HTTPException(status_code=400, detail="symbol must not be blank")

    safe_to = to_date or datetime.now().date()
    safe_from = from_date or (safe_to - timedelta(days=365))
    if safe_from > safe_to:
        raise HTTPException(status_code=400, detail="from must be <= to")

    safe_limit = max(1, min(1000, limit))
    return trimmed, safe_from, safe_to, safe_limit


def _decimal_to_str(value: object) -> str:
    """Serialize Decimal (or any numeric) to string without float conversion."""
    if isinstance(value, Decimal):
        return str(value)
    return str(value)


def _serialize_investor_flow(row: dict) -> InvestorFlowRecord:
    amount_keys = {k for k in row if k.endswith("_amount")}
    data = {
        k: (_decimal_to_str(v) if k in amount_keys else v)
        for k, v in row.items()
    }
    if isinstance(data.get("trade_date"), date):
        data["trade_date"] = data["trade_date"].isoformat()
    return InvestorFlowRecord(**data)


def _serialize_short_selling(row: dict) -> ShortSellingRecord:
    decimal_keys = {"short_sell_amount", "short_balance_amount", "short_sell_ratio"}
    data = {
        k: (_decimal_to_str(v) if k in decimal_keys else v)
        for k, v in row.items()
    }
    if isinstance(data.get("trade_date"), date):
        data["trade_date"] = data["trade_date"].isoformat()
    return ShortSellingRecord(**data)


def _serialize_program_trading(row: dict) -> ProgramTradingRecord:
    amount_keys = {k for k in row if k.endswith("_amount")}
    data = {
        k: (_decimal_to_str(v) if k in amount_keys else v)
        for k, v in row.items()
    }
    if isinstance(data.get("trade_date"), date):
        data["trade_date"] = data["trade_date"].isoformat()
    return ProgramTradingRecord(**data)


def _serialize_foreign_holding(row: dict) -> ForeignHoldingRecord:
    decimal_keys = {"exhaustion_ratio"}
    data = {
        k: (_decimal_to_str(v) if k in decimal_keys else v)
        for k, v in row.items()
    }
    if isinstance(data.get("trade_date"), date):
        data["trade_date"] = data["trade_date"].isoformat()
    return ForeignHoldingRecord(**data)


# ---------------------------------------------------------------------------
# Investor flow — DB fetch helpers (patchable in tests)
# ---------------------------------------------------------------------------


def fetch_investor_flow_records(
    *,
    symbol: str,
    from_date: date,
    to_date: date,
    market: str | None,
    limit: int,
) -> list[dict]:
    from src.repositories.investor_flow_repository import InvestorFlowRepository
    db = load_db_config_from_env()
    repo = InvestorFlowRepository()
    with connect(db) as conn:
        return repo.find_investor_flow(symbol, from_date, to_date, limit, conn, market=market)


def fetch_short_selling_records(
    *,
    symbol: str,
    from_date: date,
    to_date: date,
    market: str | None,
    limit: int,
) -> list[dict]:
    from src.repositories.investor_flow_repository import InvestorFlowRepository
    db = load_db_config_from_env()
    repo = InvestorFlowRepository()
    with connect(db) as conn:
        return repo.find_short_selling(symbol, from_date, to_date, limit, conn, market=market)


def fetch_program_trading_records(
    *,
    symbol: str,
    from_date: date,
    to_date: date,
    market: str | None,
    limit: int,
) -> list[dict]:
    from src.repositories.investor_flow_repository import InvestorFlowRepository
    db = load_db_config_from_env()
    repo = InvestorFlowRepository()
    with connect(db) as conn:
        return repo.find_program_trading(symbol, from_date, to_date, limit, conn, market=market)


def fetch_foreign_holding_records(
    *,
    symbol: str,
    from_date: date,
    to_date: date,
    market: str | None,
    limit: int,
) -> list[dict]:
    from src.repositories.investor_flow_repository import InvestorFlowRepository
    db = load_db_config_from_env()
    repo = InvestorFlowRepository()
    with connect(db) as conn:
        return repo.find_foreign_holding(symbol, from_date, to_date, limit, conn, market=market)


# ---------------------------------------------------------------------------
# Investor flow — REST endpoints
# ---------------------------------------------------------------------------


@app.get("/investor-flow/{symbol}", response_model=list[InvestorFlowRecord])
def get_investor_flow(
    symbol: str,
    from_date: date | None = Query(default=None, alias="from"),
    to: date | None = None,
    market: str | None = None,
    limit: int = 250,
) -> list[InvestorFlowRecord]:
    trimmed, safe_from, safe_to, safe_limit = _validate_symbol_and_dates(symbol, from_date, to, limit)
    try:
        rows = fetch_investor_flow_records(
            symbol=trimmed,
            from_date=safe_from,
            to_date=safe_to,
            market=market,
            limit=safe_limit,
        )
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    return [_serialize_investor_flow(row) for row in rows]


@app.get("/short-selling/{symbol}", response_model=list[ShortSellingRecord])
def get_short_selling(
    symbol: str,
    from_date: date | None = Query(default=None, alias="from"),
    to: date | None = None,
    market: str | None = None,
    limit: int = 250,
) -> list[ShortSellingRecord]:
    trimmed, safe_from, safe_to, safe_limit = _validate_symbol_and_dates(symbol, from_date, to, limit)
    try:
        rows = fetch_short_selling_records(
            symbol=trimmed,
            from_date=safe_from,
            to_date=safe_to,
            market=market,
            limit=safe_limit,
        )
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    return [_serialize_short_selling(row) for row in rows]


@app.get("/program-trading/{symbol}", response_model=list[ProgramTradingRecord])
def get_program_trading(
    symbol: str,
    from_date: date | None = Query(default=None, alias="from"),
    to: date | None = None,
    market: str | None = None,
    limit: int = 250,
) -> list[ProgramTradingRecord]:
    trimmed, safe_from, safe_to, safe_limit = _validate_symbol_and_dates(symbol, from_date, to, limit)
    try:
        rows = fetch_program_trading_records(
            symbol=trimmed,
            from_date=safe_from,
            to_date=safe_to,
            market=market,
            limit=safe_limit,
        )
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    return [_serialize_program_trading(row) for row in rows]


@app.get("/foreign-holding/{symbol}", response_model=list[ForeignHoldingRecord])
def get_foreign_holding(
    symbol: str,
    from_date: date | None = Query(default=None, alias="from"),
    to: date | None = None,
    market: str | None = None,
    limit: int = 250,
) -> list[ForeignHoldingRecord]:
    trimmed, safe_from, safe_to, safe_limit = _validate_symbol_and_dates(symbol, from_date, to, limit)
    try:
        rows = fetch_foreign_holding_records(
            symbol=trimmed,
            from_date=safe_from,
            to_date=safe_to,
            market=market,
            limit=safe_limit,
        )
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc
    return [_serialize_foreign_holding(row) for row in rows]


# ---------------------------------------------------------------------------
# Symbol-level single-ticker collect endpoints
# ---------------------------------------------------------------------------


class CollectSymbolDailyRequest(BaseModel):
    symbol: str
    provider: Literal["yfinance", "pykrx"]
    start: str = "2020-01-01"
    end: str = Field(default_factory=lambda: datetime.now().date().isoformat())
    market: str = ""


class CollectSymbolDailyResponse(BaseModel):
    symbol: str
    provider: str
    rows_inserted: int
    start: str
    end: str
    success: bool
    error: str | None = None


class CollectSymbolWeeklyRequest(BaseModel):
    symbol: str
    provider: Literal["yfinance", "pykrx"]
    start: str = "2015-01-01"
    end: str = Field(default_factory=lambda: datetime.now().date().isoformat())
    market: str = ""


class CollectSymbolWeeklyResponse(BaseModel):
    symbol: str
    provider: str
    rows_inserted: int
    start: str
    end: str
    success: bool
    error: str | None = None


@app.post("/collect/symbol/daily", response_model=CollectSymbolDailyResponse)
def collect_symbol_daily(request: CollectSymbolDailyRequest) -> CollectSymbolDailyResponse:
    from src.collectors.yfinance_daily_collector import DailyCollectRequest as YFRequest, YFinanceDailyCollector
    from src.collectors.pykrx_daily_collector import DailyCollectRequest as PykrxRequest, PykrxDailyCollector
    from src.repositories.market_daily_ohlcv_repository import MarketDailyOhlcvRepository, OhlcvUpsertContext

    sym = request.symbol.strip().upper()
    start_d = date.fromisoformat(request.start)
    end_d = date.fromisoformat(request.end)
    if start_d > end_d:
        raise HTTPException(status_code=400, detail="start must be <= end")

    db = load_db_config_from_env()
    repo = MarketDailyOhlcvRepository(db)

    try:
        if request.provider == "yfinance":
            market = request.market or "US"
            collector = YFinanceDailyCollector()
            frame = collector.fetch(YFRequest(symbol=sym, start_date=start_d, end_date=end_d, auto_adjust=False))
            rows = repo.upsert_daily_rows(frame, OhlcvUpsertContext(
                source="yfinance", symbol=sym, market=market,
                provider="yfinance", interval="1d", is_adjusted=False,
            ))
        else:
            market = request.market or "KRX"
            collector = PykrxDailyCollector()
            frame = collector.fetch(PykrxRequest(symbol=sym, start_date=start_d, end_date=end_d, adjusted=False))
            rows = repo.upsert_daily_rows(frame, OhlcvUpsertContext(
                source="pykrx", symbol=sym, market=market,
                provider="pykrx", interval="1d", is_adjusted=False,
            ))
        return CollectSymbolDailyResponse(
            symbol=sym, provider=request.provider,
            rows_inserted=rows, start=request.start, end=request.end, success=True,
        )
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc


@app.post("/collect/symbol/weekly", response_model=CollectSymbolWeeklyResponse)
def collect_symbol_weekly(request: CollectSymbolWeeklyRequest) -> CollectSymbolWeeklyResponse:
    from src.collectors.yfinance_weekly_collector import WeeklyCollectRequest as YFRequest, YFinanceWeeklyCollector
    from src.collectors.pykrx_weekly_collector import WeeklyCollectRequest as PykrxRequest, PykrxWeeklyCollector
    from src.repositories.market_weekly_ohlcv_repository import MarketWeeklyOhlcvRepository, OhlcvUpsertContext

    sym = request.symbol.strip().upper()
    start_d = date.fromisoformat(request.start)
    end_d = date.fromisoformat(request.end)
    if start_d > end_d:
        raise HTTPException(status_code=400, detail="start must be <= end")

    db = load_db_config_from_env()
    repo = MarketWeeklyOhlcvRepository(db)

    try:
        if request.provider == "yfinance":
            market = request.market or "US"
            collector = YFinanceWeeklyCollector()
            frame = collector.fetch(YFRequest(symbol=sym, start_date=start_d, end_date=end_d, auto_adjust=False))
            rows = repo.upsert_weekly_rows(frame, OhlcvUpsertContext(
                source="yfinance", symbol=sym, market=market,
                provider="yfinance", interval="1wk", is_adjusted=False,
            ))
        else:
            market = request.market or "KRX"
            collector = PykrxWeeklyCollector()
            frame = collector.fetch(PykrxRequest(symbol=sym, start_date=start_d, end_date=end_d, adjusted=False))
            rows = repo.upsert_weekly_rows(frame, OhlcvUpsertContext(
                source="pykrx", symbol=sym, market=market,
                provider="pykrx", interval="1wk", is_adjusted=False,
            ))
        return CollectSymbolWeeklyResponse(
            symbol=sym, provider=request.provider,
            rows_inserted=rows, start=request.start, end=request.end, success=True,
        )
    except Exception as exc:  # noqa: BLE001
        raise HTTPException(status_code=500, detail=str(exc)) from exc
