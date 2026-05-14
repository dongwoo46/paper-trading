from __future__ import annotations

import logging
from dataclasses import dataclass, field
from datetime import date, datetime

from src.application.daily_fetch_service import load_db_config_from_env
from src.catalog.postgres_symbol_catalog import PostgresSymbolCatalogRepository
from src.collectors.pykrx_weekly_collector import PykrxWeeklyCollector
from src.collectors.kis_weekly_collector import KisWeeklyCollector
from src.collectors.yfinance_weekly_collector import YFinanceWeeklyCollector
from src.jobs.catalog_weekly_fetch_job import CatalogWeeklyFetchJob, FetchResult, FetchWindow
from src.repositories.market_weekly_ohlcv_repository import MarketWeeklyOhlcvRepository

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class WeeklyFetchOptions:
    provider: str = "all"
    start: str = "2010-01-01"
    end: str = field(default_factory=lambda: datetime.now().date().isoformat())
    only_default: bool = False
    auto_adjust: bool = False
    adjusted: bool = False


def execute(options: WeeklyFetchOptions) -> dict[str, object]:
    window = FetchWindow(start_date=date.fromisoformat(options.start), end_date=date.fromisoformat(options.end))
    if window.start_date > window.end_date:
        raise ValueError("start date must be <= end date")
    if options.provider not in ("yfinance", "pykrx", "kis", "all"):
        raise ValueError("provider must be yfinance, pykrx, kis, or all")

    db_config = load_db_config_from_env()
    repository = PostgresSymbolCatalogRepository(db_config)
    ohlcv_repository = MarketWeeklyOhlcvRepository(db_config)
    job = CatalogWeeklyFetchJob(
        yfinance_collector=YFinanceWeeklyCollector(),
        pykrx_collector=PykrxWeeklyCollector(),
        kis_collector=KisWeeklyCollector() if options.provider in ("kis", "all") else None,
        ohlcv_repository=ohlcv_repository,
    )

    results: list[FetchResult] = []
    if options.provider in ("yfinance", "all"):
        symbols = repository.list_symbols(provider="yfinance", only_default=options.only_default)
        yfinance_results = job.run_for_yfinance(symbols=symbols, window=window, auto_adjust=options.auto_adjust)
        results.extend(yfinance_results)
        _sync_collection_status(repository, yfinance_results)

    if options.provider in ("pykrx", "all"):
        symbols = repository.list_symbols(provider="pykrx", only_default=options.only_default)
        pykrx_results = job.run_for_pykrx(symbols=symbols, window=window, adjusted=options.adjusted)
        results.extend(pykrx_results)
        _sync_collection_status(repository, pykrx_results)

    if options.provider in ("kis", "all"):
        symbols = repository.list_symbols(provider="kis", only_default=options.only_default)
        kis_results = job.run_for_kis(symbols=symbols, window=window, adjusted=options.adjusted)
        results.extend(kis_results)
        _sync_collection_status(repository, kis_results)

    return {
        "provider": options.provider,
        "symbols": len(results),
        "success_symbols": len([r for r in results if r.success and r.rows_inserted > 0]),
        "failed_symbols": len([r for r in results if not r.success]),
        "skipped_symbols": len([r for r in results if r.skipped or (r.success and r.rows_inserted == 0)]),
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
