"""chart-analysis 라우터 엔드포인트 제거 확인 테스트.

Step 3에서 삭제된 분석 파이프라인 엔드포인트가 실제로 존재하지 않음을 검증한다.
"""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src'))

import pytest
from fastapi.testclient import TestClient


@pytest.fixture(scope="module")
def client():
    from src.interfaces.api.app import app
    return TestClient(app, raise_server_exceptions=False)


class TestChartAnalysisRouterNoAnalyzeEndpoint:

    def test_post_chart_analysis_symbol_not_found(self, client):
        """POST /chart-analysis/{symbol} — 라우트 제거됨 → 404 또는 405."""
        resp = client.post("/chart-analysis/SOME")
        assert resp.status_code in (404, 405), (
            f"Expected 404 or 405, got {resp.status_code}"
        )

    def test_admin_symbols_not_found(self, client):
        """GET /admin/symbols — 라우트 제거됨 → 404."""
        resp = client.get("/admin/symbols")
        assert resp.status_code == 404, (
            f"Expected 404, got {resp.status_code}"
        )

    def test_admin_run_analysis_not_found(self, client):
        """POST /admin/run-analysis/TEST — 라우트 제거됨 → 404."""
        resp = client.post("/admin/run-analysis/TEST")
        assert resp.status_code == 404, (
            f"Expected 404, got {resp.status_code}"
        )
