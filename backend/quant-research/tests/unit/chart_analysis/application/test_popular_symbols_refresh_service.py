"""PopularSymbolsRefreshService 단위 테스트."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

from decimal import Decimal
from unittest.mock import MagicMock

import pytest

from chart_analysis.application.popular_symbols_refresh_service import PopularSymbolsRefreshService


def _make_metric(symbol: str, market_cap: str, avg_volume: str) -> dict:
    return {
        "symbol": symbol,
        "market": "KR",
        "market_cap": Decimal(market_cap),
        "avg_volume": Decimal(avg_volume),
    }


class TestPopularSymbolsRefreshService:

    def test_refresh_returns_zero_when_no_metrics(self):
        """metrics 없으면 0 반환 + replace_all([]) 호출."""
        metrics_repo = MagicMock()
        metrics_repo.find_all_metrics.return_value = []

        popular_repo = MagicMock()
        popular_repo.replace_all.return_value = 0

        svc = PopularSymbolsRefreshService(
            symbol_metrics_repository=metrics_repo,
            popular_symbols_repository=popular_repo,
            top_n=10,
        )
        result = svc.refresh()

        assert result == 0
        popular_repo.replace_all.assert_called_once_with([])

    def test_refresh_selects_top_n(self):
        """5개 metrics, top_n=3 → replace_all에 정확히 3개 전달."""
        metrics = [
            _make_metric("SYM1", "100000", "10000"),
            _make_metric("SYM2", "200000", "20000"),
            _make_metric("SYM3", "300000", "30000"),
            _make_metric("SYM4", "400000", "40000"),
            _make_metric("SYM5", "500000", "50000"),
        ]
        metrics_repo = MagicMock()
        metrics_repo.find_all_metrics.return_value = metrics

        popular_repo = MagicMock()
        popular_repo.replace_all.return_value = 3

        svc = PopularSymbolsRefreshService(
            symbol_metrics_repository=metrics_repo,
            popular_symbols_repository=popular_repo,
            top_n=3,
        )
        result = svc.refresh()

        call_args = popular_repo.replace_all.call_args[0][0]
        assert len(call_args) == 3
        assert result == 3

    def test_refresh_uses_z_score_ranking(self):
        """market_cap이 높은 종목이 top에 선택됨을 검증."""
        metrics = [
            _make_metric("LOW", "10000", "1000"),
            _make_metric("HIGH", "9000000", "900000"),
            _make_metric("MED", "100000", "10000"),
        ]
        metrics_repo = MagicMock()
        metrics_repo.find_all_metrics.return_value = metrics

        popular_repo = MagicMock()
        popular_repo.replace_all.return_value = 1

        svc = PopularSymbolsRefreshService(
            symbol_metrics_repository=metrics_repo,
            popular_symbols_repository=popular_repo,
            top_n=1,
        )
        svc.refresh()

        rows = popular_repo.replace_all.call_args[0][0]
        assert len(rows) == 1
        assert rows[0]["symbol"] == "HIGH"
