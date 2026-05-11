"""Substep 5-6: request-llm-report endpoint 통합 테스트."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src'))

from typing import List, Optional

import pytest
import httpx


# ---------------------------------------------------------------------------
# Fake 큐 레포지터리
# ---------------------------------------------------------------------------

class FakeQueueRepository:
    def __init__(self) -> None:
        self._items: List[dict] = []

    def enqueue(self, symbol: str, window: str, interval: str) -> None:
        for item in self._items:
            if (item["symbol"] == symbol
                    and item["window"] == window
                    and item["interval"] == interval
                    and item["status"] == "pending"):
                item["requested_count"] += 1
                return
        self._items.append({
            "symbol": symbol,
            "window": window,
            "interval": interval,
            "status": "pending",
            "requested_count": 1,
        })

    def fetch_pending(self, limit: int) -> List[dict]:
        return [i for i in self._items if i["status"] == "pending"][:limit]

    def mark_processed(self, id: int, status: str) -> None:
        pass

    def count_pending(self, symbol: str, window: str, interval: str) -> int:
        for item in self._items:
            if (item["symbol"] == symbol
                    and item["window"] == window
                    and item["interval"] == interval
                    and item["status"] == "pending"):
                return item["requested_count"]
        return 0

    def pending_position(self, symbol: str, window: str, interval: str) -> int:
        """큐 위치 (1-indexed)."""
        pending = [i for i in self._items if i["status"] == "pending"]
        for idx, item in enumerate(pending, start=1):
            if (item["symbol"] == symbol
                    and item["window"] == window
                    and item["interval"] == interval):
                return idx
        return len(pending)


def _build_queue_app():
    from fastapi import FastAPI
    from chart_analysis.interfaces.chart_analysis_router import (
        chart_analysis_router,
        get_queue_repository,
    )

    app = FastAPI()
    app.include_router(chart_analysis_router)

    queue_repo = FakeQueueRepository()
    app.dependency_overrides[get_queue_repository] = lambda: queue_repo
    return app, queue_repo


@pytest.mark.asyncio
class TestRequestLlmReportEndpoint:

    async def test_new_request_returns_201_with_queue_position(self):
        """새 요청 → 201 + queue_position."""
        app, _ = _build_queue_app()

        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app),
            base_url="http://testserver",
        ) as client:
            response = await client.post(
                "/chart-analysis/request-llm-report",
                json={"symbol": "005930", "window": "3M", "interval": "D"},
            )

        assert response.status_code == 201
        data = response.json()
        assert "queue_position" in data
        assert data["status"] == "queued"

    async def test_duplicate_request_returns_200_with_increased_count(self):
        """중복 요청 → 200 + requested_count 증가."""
        app, queue_repo = _build_queue_app()

        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app),
            base_url="http://testserver",
        ) as client:
            # 첫 번째 요청
            await client.post(
                "/chart-analysis/request-llm-report",
                json={"symbol": "005930", "window": "3M", "interval": "D"},
            )
            # 두 번째 중복 요청
            response = await client.post(
                "/chart-analysis/request-llm-report",
                json={"symbol": "005930", "window": "3M", "interval": "D"},
            )

        assert response.status_code == 200
        assert queue_repo.count_pending("005930", "3M", "D") == 2

    async def test_invalid_window_returns_400(self):
        """잘못된 window → 400."""
        app, _ = _build_queue_app()

        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app),
            base_url="http://testserver",
        ) as client:
            response = await client.post(
                "/chart-analysis/request-llm-report",
                json={"symbol": "005930", "window": "INVALID", "interval": "D"},
            )

        assert response.status_code == 400

    async def test_invalid_interval_returns_400(self):
        """잘못된 interval → 400."""
        app, _ = _build_queue_app()

        async with httpx.AsyncClient(
            transport=httpx.ASGITransport(app=app),
            base_url="http://testserver",
        ) as client:
            response = await client.post(
                "/chart-analysis/request-llm-report",
                json={"symbol": "005930", "window": "1M", "interval": "INVALID"},
            )

        assert response.status_code == 400