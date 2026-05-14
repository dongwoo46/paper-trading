"""PopularSymbolsRefreshService — 인기 종목 TOP N 주 1회 갱신 서비스.

선정 기준: score = 0.5 * z(market_cap) + 0.5 * z(60d_avg_amount)
미장/국장 통합 TOP N (CHART_ANALYSIS_POPULAR_TOP_N, 기본 300).
TRUNCATE+INSERT 트랜잭션으로 구버전 row 완전 교체.
"""
from __future__ import annotations

import logging
import os
from decimal import Decimal
from datetime import datetime, timezone

logger = logging.getLogger(__name__)


class PopularSymbolsRefreshService:
    """인기 종목 TOP N 갱신 서비스.

    의존성 (생성자 주입):
        symbol_metrics_repository: find_all_metrics() → list[dict{symbol, market, market_cap, avg_volume}]
        popular_symbols_repository: replace_all(rows) → int
        top_n: 선정할 종목 수 (기본: 환경변수 CHART_ANALYSIS_POPULAR_TOP_N, 없으면 300)
    """

    def __init__(
        self,
        symbol_metrics_repository,
        popular_symbols_repository,
        top_n: int | None = None,
    ) -> None:
        self._metrics_repo = symbol_metrics_repository
        self._popular_repo = popular_symbols_repository
        self._top_n = top_n if top_n is not None else self._default_top_n()

    @staticmethod
    def _default_top_n() -> int:
        return int(os.getenv("CHART_ANALYSIS_POPULAR_TOP_N", "300"))

    def refresh(self) -> int:
        """인기 종목 전체 교체. 삽입된 row 수 반환."""
        metrics = self._metrics_repo.find_all_metrics()
        if not metrics:
            logger.warning("popular_symbols_refresh:no_metrics found")
            self._popular_repo.replace_all([])
            return 0

        scores = self._compute_scores(metrics)

        # score 내림차순 정렬 → top_n 선택
        sorted_metrics = sorted(metrics, key=lambda m: scores.get(m["symbol"], Decimal("0")), reverse=True)
        top = sorted_metrics[: self._top_n]

        now = datetime.now(timezone.utc)
        rows = [
            {
                "symbol": m["symbol"],
                "market": m["market"],
                "rank": rank,
                "market_cap": m["market_cap"],
                "avg_volume": m["avg_volume"],
                "score": scores.get(m["symbol"], Decimal("0")),
                "updated_at": now,
            }
            for rank, m in enumerate(top, start=1)
        ]

        inserted = self._popular_repo.replace_all(rows)
        logger.info(
            "popular_symbols_refresh:done inserted=%d top_n=%d",
            inserted, self._top_n,
        )
        return inserted

    @staticmethod
    def _compute_scores(metrics: list[dict]) -> dict[str, Decimal]:
        """z-score 정규화 + 가중치 0.5/0.5 합산 → {symbol: score}."""
        if not metrics:
            return {}

        caps = [Decimal(str(m["market_cap"])) for m in metrics]
        vols = [Decimal(str(m["avg_volume"])) for m in metrics]

        z_caps = _z_normalize(caps)
        z_vols = _z_normalize(vols)

        result: dict[str, Decimal] = {}
        for i, m in enumerate(metrics):
            score = Decimal("0.5") * z_caps[i] + Decimal("0.5") * z_vols[i]
            result[m["symbol"]] = score

        return result


# ---------------------------------------------------------------------------
# 순수 헬퍼
# ---------------------------------------------------------------------------

def _z_normalize(values: list[Decimal]) -> list[Decimal]:
    """z-score 정규화. std=0이면 모두 Decimal("0") 반환."""
    n = len(values)
    if n == 0:
        return []
    mean = sum(values, Decimal("0")) / Decimal(n)
    variance = sum((v - mean) ** 2 for v in values) / Decimal(n)
    std = variance ** Decimal("0.5")
    if std == Decimal("0"):
        return [Decimal("0")] * n
    return [(v - mean) / std for v in values]
