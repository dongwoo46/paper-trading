"""단위 테스트: PostgresOhlcvRepository."""
from __future__ import annotations

import os
import sys

# src 패키지를 sys.path에 추가 (기존 테스트 관례)
sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "..", "src"))

from datetime import date
from decimal import Decimal
from unittest.mock import MagicMock

import pytest

from chart_analysis.infrastructure.ohlcv_repository import PostgresOhlcvRepository


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_db_row(
    trade_date=date(2026, 1, 2),
    open_price="100.00",
    high_price="105.00",
    low_price="99.00",
    close_price="103.00",
    volume="1000000.00",
):
    """psycopg 커서 fetchall이 반환하는 튜플 모사."""
    return (
        trade_date,
        Decimal(open_price),
        Decimal(high_price),
        Decimal(low_price),
        Decimal(close_price),
        Decimal(volume),
    )


def _make_repo(rows: list[tuple]) -> tuple[PostgresOhlcvRepository, MagicMock]:
    """DB 연결을 mock한 repo 인스턴스 반환."""
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_cursor.__enter__ = MagicMock(return_value=mock_cursor)
    mock_cursor.__exit__ = MagicMock(return_value=False)
    mock_cursor.fetchall.return_value = rows
    mock_conn.cursor.return_value = mock_cursor
    mock_conn.__enter__ = MagicMock(return_value=mock_conn)
    mock_conn.__exit__ = MagicMock(return_value=False)

    repo = PostgresOhlcvRepository.__new__(PostgresOhlcvRepository)
    repo._connect = MagicMock(return_value=mock_conn)
    return repo, mock_cursor


# ---------------------------------------------------------------------------
# 윈도우 → from_date 변환 테스트
# ---------------------------------------------------------------------------

class TestWindowToFromDate:
    """window 파라미터 → SQL WHERE trade_date >= from_date 변환 검증."""

    @pytest.mark.parametrize("window,expected_days", [
        ("1M", 30),
        ("3M", 90),
        ("6M", 180),
        ("1Y", 365),
        ("2Y", 730),
    ])
    def test_window_maps_to_correct_days(self, window: str, expected_days: int):
        repo, mock_cursor = _make_repo([])
        repo.find_window("005930", window, "D")

        call_args = mock_cursor.execute.call_args
        query: str = call_args[0][0]
        params: tuple = call_args[0][1]

        # from_date 파라미터가 존재해야 함
        assert len(params) >= 2, "쿼리 파라미터에 from_date가 없습니다."
        from_date = params[1]
        assert from_date is not None, "from_date가 None입니다."

        from datetime import date as date_cls, timedelta
        today = date_cls.today()
        expected_from = today - timedelta(days=expected_days)
        # 1일 오차 허용 (테스트 실행 타이밍)
        delta = abs((from_date - expected_from).days)
        assert delta <= 1, f"window={window} from_date 오차: {delta}일"

    def test_max_window_uses_no_date_param(self):
        repo, mock_cursor = _make_repo([])
        repo.find_window("005930", "MAX", "W")

        call_args = mock_cursor.execute.call_args
        params: tuple = call_args[0][1]
        # MAX 윈도우는 from_date 조건 없음 → symbol 1개만
        assert len(params) == 1, f"MAX 윈도우 params는 (symbol,) 1개이어야 합니다, 실제: {params}"
        assert params[0] == "005930"


# ---------------------------------------------------------------------------
# interval → 테이블 분기 테스트
# ---------------------------------------------------------------------------

class TestIntervalTableRouting:
    """interval=D → market_daily_ohlcv, W → market_weekly_ohlcv 분기 검증."""

    def test_daily_interval_queries_daily_table(self):
        repo, mock_cursor = _make_repo([])
        repo.find_window("005930", "1M", "D")

        query: str = mock_cursor.execute.call_args[0][0]
        assert "market_daily_ohlcv" in query, \
            "interval=D일 때 market_daily_ohlcv를 조회해야 합니다."
        assert "market_weekly_ohlcv" not in query

    def test_weekly_interval_queries_weekly_table(self):
        repo, mock_cursor = _make_repo([])
        repo.find_window("005930", "1Y", "W")

        query: str = mock_cursor.execute.call_args[0][0]
        assert "market_weekly_ohlcv" in query, \
            "interval=W일 때 market_weekly_ohlcv를 조회해야 합니다."
        assert "market_daily_ohlcv" not in query


# ---------------------------------------------------------------------------
# DB row → list[Candle] 변환 테스트
# ---------------------------------------------------------------------------

class TestRowToCandle:
    """DB row → Candle 변환 — Decimal 타입 검증."""

    def test_returns_candle_list(self):
        from chart_analysis.domain.value_objects import Candle
        rows = [_make_db_row()]
        repo, _ = _make_repo(rows)

        result = repo.find_window("005930", "1M", "D")

        assert len(result) == 1
        assert isinstance(result[0], Candle)

    def test_candle_prices_are_decimal(self):
        rows = [_make_db_row()]
        repo, _ = _make_repo(rows)

        result = repo.find_window("005930", "1M", "D")
        c = result[0]

        for field_name in ("open", "high", "low", "close", "volume"):
            val = getattr(c, field_name)
            assert isinstance(val, Decimal), \
                f"Candle.{field_name}은 Decimal이어야 합니다, 실제: {type(val).__name__}"

    def test_multiple_rows_returned(self):
        rows = [
            _make_db_row(trade_date=date(2026, 1, i + 1)) for i in range(5)
        ]
        repo, _ = _make_repo(rows)

        result = repo.find_window("005930", "1M", "D")
        assert len(result) == 5

    def test_empty_result_returns_empty_list(self):
        repo, _ = _make_repo([])
        result = repo.find_window("AAPL", "MAX", "W")
        assert result == []

    def test_candle_date_preserved(self):
        expected_date = date(2026, 3, 15)
        rows = [_make_db_row(trade_date=expected_date)]
        repo, _ = _make_repo(rows)

        result = repo.find_window("005930", "1M", "D")
        assert result[0].date == expected_date