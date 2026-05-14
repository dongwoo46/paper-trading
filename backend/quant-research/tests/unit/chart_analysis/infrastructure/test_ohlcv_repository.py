"""OhlcvRepository 단위 테스트 — DB 연결 없는 순수 mock 테스트."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

from datetime import date
from decimal import Decimal
from unittest.mock import MagicMock

import pytest

from chart_analysis.infrastructure.ohlcv_repository import PostgresOhlcvRepository
from chart_analysis.domain.value_objects import Candle


def _make_connect_fn(rows: list):
    """mock connect_fn — cursor.fetchall() returns rows.

    Returns a MagicMock callable so .return_value chaining works in assertions.
    """
    mock_cur = MagicMock()
    mock_cur.__enter__ = lambda s: s
    mock_cur.__exit__ = MagicMock(return_value=False)
    mock_cur.fetchall.return_value = rows

    mock_conn = MagicMock()
    mock_conn.__enter__ = lambda s: s
    mock_conn.__exit__ = MagicMock(return_value=False)
    mock_conn.cursor.return_value = mock_cur

    connect_fn = MagicMock()
    connect_fn.return_value = mock_conn
    return connect_fn


class TestPostgresOhlcvRepository:

    def test_find_window_daily_returns_candles(self):
        """find_window('D', '1M') — DB row → Candle 목록 변환 검증."""
        row = (date(2024, 1, 2), 70000, 71000, 69500, 70500, 1000000)
        connect_fn = _make_connect_fn([row])

        repo = PostgresOhlcvRepository(connect_fn)
        candles = repo.find_window("005930", "1M", "D")

        assert len(candles) == 1
        c = candles[0]
        # isinstance 체크는 클래스 identity 문제 회피 — 속성으로 검증
        assert c.date == date(2024, 1, 2)
        assert c.open == Decimal("70000")
        assert c.high == Decimal("71000")
        assert c.low == Decimal("69500")
        assert c.close == Decimal("70500")
        assert c.volume == Decimal("1000000")

    def test_find_window_invalid_window_raises(self):
        """잘못된 window 값 → ValueError."""
        connect_fn = _make_connect_fn([])
        repo = PostgresOhlcvRepository(connect_fn)

        with pytest.raises(ValueError, match="지원하지 않는 window"):
            repo.find_window("005930", "INVALID", "D")

    def test_find_window_invalid_interval_raises(self):
        """잘못된 interval 값 → ValueError."""
        connect_fn = _make_connect_fn([])
        repo = PostgresOhlcvRepository(connect_fn)

        with pytest.raises(ValueError, match="지원하지 않는 interval"):
            repo.find_window("005930", "1M", "X")

    def test_find_window_max_has_no_date_filter(self):
        """MAX window → SQL 파라미터에 from_date 없음 (symbol만)."""
        connect_fn = _make_connect_fn([])
        repo = PostgresOhlcvRepository(connect_fn)

        repo.find_window("005930", "MAX", "W")

        # mock_cur.__enter__ is a lambda returning mock_cur itself
        mock_conn = connect_fn.return_value
        mock_cur = mock_conn.cursor.return_value
        # mock_cur is returned by the lambda __enter__, so execute is on mock_cur directly
        call_args = mock_cur.execute.call_args
        params = call_args[0][1]
        # MAX 윈도우: 파라미터는 (symbol,) 만
        assert len(params) == 1
        assert params[0] == "005930"
