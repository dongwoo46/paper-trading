from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import date, datetime

from src.application.daily_fetch_service import load_db_config_from_env
from src.catalog.postgres_symbol_catalog import PostgresSymbolCatalogRepository
from src.collectors.yfinance_weekly_collector import YFinanceWeeklyCollector
from src.jobs.catalog_weekly_fetch_job import CatalogWeeklyFetchJob, FetchResult, FetchWindow
from src.repositories.market_weekly_ohlcv_repository import MarketWeeklyOhlcvRepository

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class WeeklyFetchOptions:
    provider: str = "yfinance"
    start: str = "2010-01-01"
    end: str = field(default_factory=lambda: datetime.now().date().isoformat())
    only_default: bool = False
    auto_adjust: bool = False


def execute(options: WeeklyFetchOptions) -> dict[str, object]:
    window = FetchWindow(start_date=date.fromisoformat(options.start), end_date=date.fromisoformat(options.end))
    if window.start_date > window.end_date:
        raise ValueError("start date must be <= end date")
    if options.provider not in ("yfinance", "all"):
        raise ValueError("provider must be yfinance or all")

    db_config = load_db_config_from_env()
    repository = PostgresSymbolCatalogRepository(db_config)
    ohlcv_repository = MarketWeeklyOhlcvRepository(db_config)
    job = CatalogWeeklyFetchJob(yfinance_collector=YFinanceWeeklyCollector(), ohlcv_repository=ohlcv_repository)

    symbols = repository.list_symbols(provider="yfinance", only_default=options.only_default)
    results = job.run_for_yfinance(symbols=symbols, window=window, auto_adjust=options.auto_adjust)
    _sync_collection_status(repository, results)

    return {
        "provider": options.provider,
        "symbols": len(results),
        "success_symbols": len([r for r in results if r.success]),
        "failed_symbols": len([r for r in results if not r.success]),
        "total_rows_inserted": sum(r.rows_inserted for r in results if r.success),
        "start": window.start_date.isoformat(),
        "end": window.end_date.isoformat(),
    }


def _sync_collection_status(repository: PostgresSymbolCatalogRepository, results: list[FetchResult]) -> None:
    collected_at = datetime.now()
    for row in results:
        if not row.success or row.fetched_until_date is None:
            continue
        repository.update_collection_status(
            provider=row.provider,
            symbol=row.symbol,
            fetched_until_date=row.fetched_until_date,
            collected_at=collected_at,
        )
