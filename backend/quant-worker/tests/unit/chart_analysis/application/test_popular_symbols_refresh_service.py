"""PopularSymbolsRefreshService 단위 테스트 (TDD — Red 먼저 작성)."""
from __future__ import annotations

from decimal import Decimal
from unittest.mock import MagicMock, patch

import pytest


# ---------------------------------------------------------------------------
# Fake 의존성
# ---------------------------------------------------------------------------

class FakeSymbolMetricsRepository:
    """시가총액 + 60일 평균 거래대금 반환 Fake."""

    def __init__(self, rows: list[dict] | None = None) -> None:
        self._rows = rows or []

    def find_all_metrics(self) -> list[dict]:
        """symbol, market, market_cap, avg_volume 반환."""
        return list(self._rows)


class FakePopularSymbolsRepository:
    """popular_symbols 테이블 Fake (TRUNCATE+INSERT 트랜잭션)."""

    def __init__(self) -> None:
        self.replace_calls: list[list[dict]] = []

    def replace_all(self, rows: list[dict]) -> int:
        """전체 교체 (TRUNCATE+INSERT). 삽입된 row 수 반환."""
        self.replace_calls.append(list(rows))
        return len(rows)


# ---------------------------------------------------------------------------
# 픽스처 헬퍼
# ---------------------------------------------------------------------------

def _make_service(
    metrics_repo: FakeSymbolMetricsRepository | None = None,
    popular_repo: FakePopularSymbolsRepository | None = None,
    top_n: int = 5,
):
    from src.chart_analysis.application.popular_symbols_refresh_service import (
        PopularSymbolsRefreshService,
    )
    return PopularSymbolsRefreshService(
        symbol_metrics_repository=metrics_repo or FakeSymbolMetricsRepository(),
        popular_symbols_repository=popular_repo or FakePopularSymbolsRepository(),
        top_n=top_n,
    )


def _make_metrics(n: int) -> list[dict]:
    rows = []
    for i in range(1, n + 1):
        rows.append({
            "symbol": f"SYM{i:04d}",
            "market": "KRX" if i <= n // 2 else "US",
            "market_cap": Decimal(str(i * 1_000_000_000)),
            "avg_volume": Decimal(str(i * 10_000_000)),
        })
    return rows


# ---------------------------------------------------------------------------
# 테스트 케이스
# ---------------------------------------------------------------------------


def test_refresh_returns_inserted_count():
    """refresh()는 삽입된 row 수를 반환한다."""
    metrics = _make_metrics(10)
    metrics_repo = FakeSymbolMetricsRepository(metrics)
    popular_repo = FakePopularSymbolsRepository()

    service = _make_service(metrics_repo=metrics_repo, popular_repo=popular_repo, top_n=5)
    count = service.refresh()

    assert count == 5, f"top_n=5이면 5개 row 반환해야 함, 실제: {count}"


def test_refresh_selects_top_n_by_score():
    """z-score 가중치 합산 → 상위 top_n 종목만 선정된다."""
    metrics = _make_metrics(10)
    metrics_repo = FakeSymbolMetricsRepository(metrics)
    popular_repo = FakePopularSymbolsRepository()

    service = _make_service(metrics_repo=metrics_repo, popular_repo=popular_repo, top_n=3)
    service.refresh()

    inserted = popular_repo.replace_calls[0]
    assert len(inserted) == 3
    # score가 높은 순서 (market_cap + avg_volume 모두 높은 SYM0010, SYM0009, SYM0008)
    symbols = [r["symbol"] for r in inserted]
    assert "SYM0010" in symbols, "가장 높은 score 종목이 포함되어야 함"


def test_refresh_score_uses_05_05_weights():
    """z-score 가중치 0.5/0.5 합산 검증.

    동일한 market_cap이지만 avg_volume이 다른 두 종목 비교.
    """
    from src.chart_analysis.application.popular_symbols_refresh_service import (
        PopularSymbolsRefreshService,
    )

    metrics = [
        {
            "symbol": "A",
            "market": "KRX",
            "market_cap": Decimal("1000000000"),
            "avg_volume": Decimal("100000000"),  # 높은 거래대금
        },
        {
            "symbol": "B",
            "market": "KRX",
            "market_cap": Decimal("1000000000"),  # 동일 시총
            "avg_volume": Decimal("10000000"),   # 낮은 거래대금
        },
    ]

    scores = PopularSymbolsRefreshService._compute_scores(metrics)
    # A의 avg_volume z-score가 B보다 높으므로 score_A > score_B
    score_a = scores["A"]
    score_b = scores["B"]
    assert score_a > score_b, f"A의 score({score_a}) > B의 score({score_b}) 이어야 함"


def test_refresh_truncate_insert_transaction():
    """refresh()는 TRUNCATE+INSERT(replace_all)를 1회만 호출한다."""
    metrics = _make_metrics(6)
    metrics_repo = FakeSymbolMetricsRepository(metrics)
    popular_repo = FakePopularSymbolsRepository()

    service = _make_service(metrics_repo=metrics_repo, popular_repo=popular_repo, top_n=3)
    service.refresh()

    # replace_all은 정확히 1회 호출
    assert len(popular_repo.replace_calls) == 1


def test_refresh_empty_metrics_returns_zero():
    """종목 메트릭이 없으면 0을 반환한다."""
    metrics_repo = FakeSymbolMetricsRepository([])
    popular_repo = FakePopularSymbolsRepository()

    service = _make_service(metrics_repo=metrics_repo, popular_repo=popular_repo, top_n=5)
    count = service.refresh()

    assert count == 0


def test_refresh_inserts_rank_field():
    """선정된 row에 rank 필드가 포함되어야 한다 (1-indexed)."""
    metrics = _make_metrics(5)
    metrics_repo = FakeSymbolMetricsRepository(metrics)
    popular_repo = FakePopularSymbolsRepository()

    service = _make_service(metrics_repo=metrics_repo, popular_repo=popular_repo, top_n=5)
    service.refresh()

    inserted = popular_repo.replace_calls[0]
    ranks = [r["rank"] for r in inserted]
    assert sorted(ranks) == list(range(1, 6)), f"rank 1~5 포함해야 함, 실제: {ranks}"


def test_refresh_top_n_env_default():
    """CHART_ANALYSIS_POPULAR_TOP_N 환경변수 기본값 300 확인."""
    from src.chart_analysis.application.popular_symbols_refresh_service import (
        PopularSymbolsRefreshService,
    )
    import os

    env_backup = os.environ.pop("CHART_ANALYSIS_POPULAR_TOP_N", None)
    try:
        svc = PopularSymbolsRefreshService.__new__(PopularSymbolsRefreshService)
        default = PopularSymbolsRefreshService._default_top_n()
        assert default == 300
    finally:
        if env_backup is not None:
            os.environ["CHART_ANALYSIS_POPULAR_TOP_N"] = env_backup