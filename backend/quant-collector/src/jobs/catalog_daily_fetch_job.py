from __future__ import annotations

from dataclasses import dataclass
from datetime import date, timedelta

import pandas as pd

from src.catalog.models import CatalogSymbol
from src.collectors.pykrx_daily_collector import (
    DailyCollectRequest as PykrxCollectRequest,
    PykrxDailyCollector,
)
from src.collectors.yfinance_daily_collector import (
    DailyCollectRequest as YFinanceCollectRequest,
    YFinanceDailyCollector,
)
from src.repositories.market_daily_ohlcv_repository import (
    MarketDailyOhlcvRepository,
    OhlcvUpsertContext,
)


@dataclass(frozen=True)
class FetchWindow:
    start_date: date
    end_date: date


@dataclass(frozen=True)
class FetchResult:
    provider: str
    symbol: str
    requested_start: date
    requested_end: date
    fetched_until_date: date | None
    rows_inserted: int
    skipped: bool
    success: bool
    error: str | None


class CatalogDailyFetchJob:
    def __init__(
        self,
        yfinance_collector: YFinanceDailyCollector,
        pykrx_collector: PykrxDailyCollector,
        ohlcv_repository: MarketDailyOhlcvRepository,
    ) -> None:
        self._yfinance_collector = yfinance_collector
        self._pykrx_collector = pykrx_collector
        self._ohlcv_repository = ohlcv_repository

    def run_for_yfinance(
        self,
        symbols: list[CatalogSymbol],
        window: FetchWindow,
        auto_adjust: bool,
    ) -> list[FetchResult]:
        results: list[FetchResult] = []
        for item in symbols:
            effective_start = self._effective_start(item, window)
            if effective_start > window.end_date:
                results.append(
                    FetchResult(
                        provider="yfinance",
                        symbol=item.symbol.upper(),
                        requested_start=effective_start,
                        requested_end=window.end_date,
                        fetched_until_date=None,
                        rows_inserted=0,
                        skipped=True,
                        success=True,
                        error=None,
                    )
                )
                continue

            request = YFinanceCollectRequest(
                symbol=item.symbol.upper(),
                start_date=effective_start,
                end_date=window.end_date,
                auto_adjust=auto_adjust,
            )
            try:
                frame = self._yfinance_collector.fetch(request)
                inserted = self._ohlcv_repository.upsert_daily_rows(
                    frame,
                    OhlcvUpsertContext(
                        source="yfinance",
                        symbol=request.symbol,
                        market=item.market,
                        provider="yfinance",
                        interval="1d",
                        is_adjusted=auto_adjust,
                    ),
                )
                results.append(
                    FetchResult(
                        provider="yfinance",
                        symbol=request.symbol,
                        requested_start=effective_start,
                        requested_end=window.end_date,
                        fetched_until_date=_max_trade_date(frame),
                        rows_inserted=inserted,
                        skipped=False,
                        success=True,
                        error=None,
                    )
                )
            except Exception as exc:  # noqa: BLE001
                results.append(
                    FetchResult(
                        provider="yfinance",
                        symbol=request.symbol,
                        requested_start=effective_start,
                        requested_end=window.end_date,
                        fetched_until_date=None,
                        rows_inserted=0,
                        skipped=False,
                        success=False,
                        error=str(exc),
                    )
                )
        return results

    def run_for_pykrx(
        self,
        symbols: list[CatalogSymbol],
        window: FetchWindow,
        adjusted: bool,
    ) -> list[FetchResult]:
        results: list[FetchResult] = []
        for item in symbols:
            effective_start = self._effective_start(item, window)
            if effective_start > window.end_date:
                results.append(
                    FetchResult(
                        provider="pykrx",
                        symbol=item.symbol,
                        requested_start=effective_start,
                        requested_end=window.end_date,
                        fetched_until_date=None,
                        rows_inserted=0,
                        skipped=True,
                        success=True,
                        error=None,
                    )
                )
                continue

            request = PykrxCollectRequest(
                symbol=item.symbol,
                start_date=effective_start,
                end_date=window.end_date,
                adjusted=adjusted,
            )
            try:
                frame = self._pykrx_collector.fetch(request)
                inserted = self._ohlcv_repository.upsert_daily_rows(
                    frame,
                    OhlcvUpsertContext(
                        source="pykrx",
                        symbol=request.symbol,
                        market=item.market,
                        provider="pykrx",
                        interval="1d",
                        is_adjusted=adjusted,
                    ),
                )
                results.append(
                    FetchResult(
                        provider="pykrx",
                        symbol=request.symbol,
                        requested_start=effective_start,
                        requested_end=window.end_date,
                        fetched_until_date=_max_trade_date(frame),
                        rows_inserted=inserted,
                        skipped=False,
                        success=True,
                        error=None,
                    )
                )
            except Exception as exc:  # noqa: BLE001
                results.append(
                    FetchResult(
                        provider="pykrx",
                        symbol=request.symbol,
                        requested_start=effective_start,
                        requested_end=window.end_date,
                        fetched_until_date=None,
                        rows_inserted=0,
                        skipped=False,
                        success=False,
                        error=str(exc),
                    )
                )
        return results

    def _effective_start(self, symbol: CatalogSymbol, window: FetchWindow) -> date:
        if symbol.fetched_until_date is None:
            return window.start_date
        next_date = symbol.fetched_until_date + timedelta(days=1)
        if next_date < window.start_date:
            return window.start_date
        return next_date


def to_summary_frame(results: list[FetchResult]) -> pd.DataFrame:
    return pd.DataFrame(
        [
            {
                "provider": row.provider,
                "symbol": row.symbol,
                "requested_start": row.requested_start.isoformat(),
                "requested_end": row.requested_end.isoformat(),
                "fetched_until_date": (
                    "" if row.fetched_until_date is None else row.fetched_until_date.isoformat()
                ),
                "rows_inserted": row.rows_inserted,
                "skipped": row.skipped,
                "success": row.success,
                "error": "" if row.error is None else row.error,
            }
            for row in results
        ]
    )


def _max_trade_date(frame: pd.DataFrame) -> date | None:
    if frame.empty:
        return None
    return frame["date"].max()
