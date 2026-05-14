from __future__ import annotations

import logging
import os
from dataclasses import dataclass, field
from datetime import date, datetime

from src.catalog.postgres_symbol_catalog import DbConfig, PostgresSymbolCatalogRepository
from src.collectors.pykrx_daily_collector import PykrxDailyCollector
from src.collectors.yfinance_daily_collector import YFinanceDailyCollector
from src.jobs.catalog_daily_fetch_job import CatalogDailyFetchJob, FetchResult, FetchWindow, to_summary_frame
from src.repositories.market_daily_ohlcv_repository import MarketDailyOhlcvRepository

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class DailyFetchOptions:
    provider: str = "all"
    start: str = "2010-01-01"
    end: str = field(default_factory=lambda: datetime.now().date().isoformat())
    only_default: bool = False
    auto_adjust: bool = False
    adjusted: bool = False


def execute(options: DailyFetchOptions) -> dict[str, object]:
    logger.info(
        "collect_daily:start provider=%s start=%s end=%s only_default=%s auto_adjust=%s adjusted=%s",
        options.provider,
        options.start,
        options.end,
        options.only_default,
        options.auto_adjust,
        options.adjusted,
    )

    window = FetchWindow(
        start_date=date.fromisoformat(options.start),
        end_date=date.fromisoformat(options.end),
    )
    if window.start_date > window.end_date:
        raise ValueError("start date must be <= end date")

    db_config = load_db_config_from_env()
    repository = PostgresSymbolCatalogRepository(db_config)
    ohlcv_repository = MarketDailyOhlcvRepository(db_config)
    job = CatalogDailyFetchJob(
        yfinance_collector=YFinanceDailyCollector(),
        pykrx_collector=PykrxDailyCollector(),
        ohlcv_repository=ohlcv_repository,
    )

    results: list[FetchResult] = []

    if options.provider in ("yfinance", "all"):
        yfinance_symbols = repository.list_symbols(
            provider="yfinance",
            only_default=options.only_default,
        )
        logger.info("collect_daily:yfinance targets=%d", len(yfinance_symbols))
        yfinance_results = job.run_for_yfinance(
            symbols=yfinance_symbols,
            window=window,
            auto_adjust=options.auto_adjust,
        )
        results.extend(yfinance_results)
        _sync_collection_status(repository, yfinance_results)
        _log_provider_result("yfinance", yfinance_results)

    if options.provider in ("pykrx", "all"):
        pykrx_symbols = repository.list_symbols(
            provider="pykrx",
            only_default=options.only_default,
        )
        logger.info("collect_daily:pykrx targets=%d", len(pykrx_symbols))
        pykrx_results = job.run_for_pykrx(
            symbols=pykrx_symbols,
            window=window,
            adjusted=options.adjusted,
        )
        results.extend(pykrx_results)
        _sync_collection_status(repository, pykrx_results)
        _log_provider_result("pykrx", pykrx_results)

    total_rows_inserted = sum(r.rows_inserted for r in results if r.success)

    response = {
        "provider": options.provider,
        "symbols": len(results),
        "success_symbols": len([r for r in results if r.success]),
        "failed_symbols": len([r for r in results if not r.success]),
        "total_rows_inserted": total_rows_inserted,
        "start": window.start_date.isoformat(),
        "end": window.end_date.isoformat(),
    }
    logger.info(
        "collect_daily:done provider=%s symbols=%d success=%d failed=%d rows_inserted=%d",
        response["provider"],
        response["symbols"],
        response["success_symbols"],
        response["failed_symbols"],
        response["total_rows_inserted"],
    )
    return response


def load_db_config_from_env() -> DbConfig:
    host = os.getenv("PG_HOST", "localhost")
    port = int(os.getenv("PG_PORT", "5432"))
    database = os.getenv("PG_DATABASE", "paper")
    user = os.getenv("PG_USER", "paper")
    password = os.getenv("PG_PASSWORD", "paper")
    return DbConfig(host=host, port=port, database=database, user=user, password=password)


def _sync_collection_status(
    repository: PostgresSymbolCatalogRepository,
    results: list[FetchResult],
) -> None:
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


def _log_provider_result(provider: str, results: list[FetchResult]) -> None:
    success_count = len([r for r in results if r.success])
    failed = [r for r in results if not r.success]
    skipped_count = len([r for r in results if r.skipped])
    logger.info(
        "collect_daily:%s result success=%d failed=%d skipped=%d",
        provider,
        success_count,
        len(failed),
        skipped_count,
    )
    if failed:
        failed_symbols = ", ".join([r.symbol for r in failed][:20])
        logger.warning("collect_daily:%s failed_symbols=%s", provider, failed_symbols)
