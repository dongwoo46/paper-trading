from __future__ import annotations

from dataclasses import dataclass
from datetime import date, timedelta

import pandas as pd

from src.catalog.models import CatalogSymbol
from src.collectors.pykrx_weekly_collector import (
    PykrxWeeklyCollector,
    WeeklyCollectRequest as PykrxCollectRequest,
)
from src.collectors.kis_weekly_collector import (
    KisWeeklyCollector,
    WeeklyCollectRequest as KisCollectRequest,
)
from src.collectors.yfinance_weekly_collector import (
    WeeklyCollectRequest as YFinanceCollectRequest,
    YFinanceWeeklyCollector,
)
from src.repositories.market_weekly_ohlcv_repository import (
    MarketWeeklyOhlcvRepository,
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


class CatalogWeeklyFetchJob:
    def __init__(
        self,
        yfinance_collector: YFinanceWeeklyCollector,
        pykrx_collector: PykrxWeeklyCollector,
        ohlcv_repository: MarketWeeklyOhlcvRepository,
        kis_collector: KisWeeklyCollector | None = None,
    ) -> None:
        self._yfinance_collector = yfinance_collector
        self._pykrx_collector = pykrx_collector
        self._kis_collector = kis_collector
        self._ohlcv_repository = ohlcv_repository

    def run_for_yfinance(
        self,
        symbols: list[CatalogSymbol],
        window: FetchWindow,
        auto_adjust: bool,
    ) -> list[FetchResult]:
        weekly_last_dates = self._ohlcv_repository.find_max_trade_dates("yfinance")
        results: list[FetchResult] = []
        for item in symbols:
            effective_start = self._effective_start(window, weekly_last_dates.get(item.symbol.upper()))
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
                inserted = self._ohlcv_repository.upsert_weekly_rows(
                    frame,
                    OhlcvUpsertContext(
                        source="yfinance",
                        symbol=request.symbol,
                        market=item.market,
                        provider="yfinance",
                        interval="1wk",
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

    def run_for_kis(
        self,
        symbols: list[CatalogSymbol],
        window: FetchWindow,
        adjusted: bool,
    ) -> list[FetchResult]:
        if self._kis_collector is None:
            raise RuntimeError("KIS collector is not configured")

        weekly_last_dates = self._ohlcv_repository.find_max_trade_dates("kis")
        results: list[FetchResult] = []
        for item in symbols:
            effective_start = self._effective_start(window, weekly_last_dates.get(item.symbol))
            if effective_start > window.end_date:
                results.append(
                    FetchResult(
                        provider="kis",
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
            request = KisCollectRequest(
                symbol=item.symbol,
                start_date=effective_start,
                end_date=window.end_date,
                adjusted=adjusted,
            )
            try:
                frame = self._kis_collector.fetch(request)
                inserted = self._ohlcv_repository.upsert_weekly_rows(
                    frame,
                    OhlcvUpsertContext(
                        source="kis",
                        symbol=request.symbol,
                        market=item.market,
                        provider="kis",
                        interval="1wk",
                        is_adjusted=adjusted,
                    ),
                )
                results.append(
                    FetchResult(
                        provider="kis",
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
                        provider="kis",
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
        weekly_last_dates = self._ohlcv_repository.find_max_trade_dates("pykrx")
        results: list[FetchResult] = []
        for item in symbols:
            effective_start = self._effective_start(window, weekly_last_dates.get(item.symbol.upper()))
            if effective_start > window.end_date:
                results.append(
                    FetchResult(
                        provider="pykrx",
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
            request = PykrxCollectRequest(
                symbol=item.symbol.upper(),
                start_date=effective_start,
                end_date=window.end_date,
                adjusted=adjusted,
            )
            try:
                frame = self._pykrx_collector.fetch(request)
                inserted = self._ohlcv_repository.upsert_weekly_rows(
                    frame,
                    OhlcvUpsertContext(
                        source="pykrx",
                        symbol=request.symbol,
                        market=item.market,
                        provider="pykrx",
                        interval="1wk",
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

    def _effective_start(self, window: FetchWindow, last_weekly_date: date | None) -> date:
        if last_weekly_date is None:
            return window.start_date
        next_date = last_weekly_date + timedelta(days=1)
        return max(next_date, window.start_date)


def _max_trade_date(frame: pd.DataFrame) -> date | None:
    if frame.empty:
        return None
    return frame["date"].max()
