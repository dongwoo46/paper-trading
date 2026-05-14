"""Substep 5-7: app.py에 chart-analysis 라우터 포함 여부 확인."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', 'src'))

import pytest


class TestAppIncludesRouter:

    def test_chart_analysis_route_exists(self):
        """app.py에 /chart-analysis/{symbol} 라우트가 존재해야 한다."""
        from src.interfaces.api.app import app
        routes = [r.path for r in app.routes]
        assert any("/chart-analysis/{symbol}" in path for path in routes), (
            f"chart-analysis route not found. Routes: {routes}"
        )