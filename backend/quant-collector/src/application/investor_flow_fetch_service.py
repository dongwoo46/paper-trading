from __future__ import annotations

import logging
from datetime import date, timedelta
from typing import Callable

logger = logging.getLogger(__name__)


class InvestorFlowFetchService:
    """Orchestrate investor flow data collection for a single date and market.

    Collector exceptions are isolated per dataset — a failure in one dataset
    does not abort the remaining datasets.
    """

    def __init__(
        self,
        collector,
        repository,
        conn_factory: Callable,
    ) -> None:
        self._collector = collector
        self._repository = repository
        self._conn_factory = conn_factory

    def execute(self, date_: date | None, market: str) -> dict:
        """Collect and persist all four investor flow datasets.

        Args:
            date_: Target date. If None, the previous business day is used.
            market: Market name, e.g. "KOSPI" or "KOSDAQ".

        Returns:
            Dict with keys: trade_date, market, investor_flow, short_selling,
            program_trading, foreign_holding (row counts), errors (list of error dicts).
        """
        if date_ is None:
            target = self._resolve_business_date(None)
        else:
            target = date_
        date_str = target.strftime("%Y%m%d")

        errors: list[dict] = []
        counts: dict[str, int] = {
            "investor_flow": 0,
            "short_selling": 0,
            "program_trading": 0,
            "foreign_holding": 0,
        }

        datasets = [
            ("investor_flow", self._collector.fetch_investor_flow, self._repository.upsert_investor_flow),
            ("short_selling", self._collector.fetch_short_selling, self._repository.upsert_short_selling),
            ("program_trading", self._collector.fetch_program_trading, self._repository.upsert_program_trading),
            ("foreign_holding", self._collector.fetch_foreign_holding, self._repository.upsert_foreign_holding),
        ]

        conn = self._conn_factory()
        try:
            for dataset_name, fetch_fn, upsert_fn in datasets:
                try:
                    records = fetch_fn(date_str, market)
                    if records:
                        inserted = upsert_fn(records, conn)
                        counts[dataset_name] = inserted
                    # empty → count stays 0, no error
                except Exception as exc:  # noqa: BLE001
                    logger.warning(
                        "investor_flow_fetch_service:error dataset=%s date=%s market=%s error=%s",
                        dataset_name,
                        date_str,
                        market,
                        str(exc),
                    )
                    errors.append({"dataset": dataset_name, "error": str(exc)})

            # Commit once after all upserts
            if hasattr(conn, "commit"):
                conn.commit()
        finally:
            if hasattr(conn, "close"):
                conn.close()

        return {
            "trade_date": date_str,
            "market": market,
            "investor_flow": counts["investor_flow"],
            "short_selling": counts["short_selling"],
            "program_trading": counts["program_trading"],
            "foreign_holding": counts["foreign_holding"],
            "errors": errors,
        }

    def _resolve_business_date(self, target: date | None) -> date:
        """Return the most recent business day on or before target (today if None).

        Business day: Monday–Friday. No holiday calendar applied.
        - Weekday (Mon–Fri): return target itself
        - Saturday: return previous Friday
        - Sunday: return previous Friday
        """
        if target is None:
            target = date.today()

        weekday = target.weekday()  # 0=Monday … 6=Sunday
        if weekday < 5:
            return target
        # Saturday (5) → go back 1 day to Friday; Sunday (6) → go back 2 days to Friday
        days_back = weekday - 4  # Saturday→1, Sunday→2
        return target - timedelta(days=days_back)
