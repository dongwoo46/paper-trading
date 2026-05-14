"""PostgresOhlcvRepository — OhlcvRepository 포트 어댑터.

interval=D → market_daily_ohlcv
interval=W → market_weekly_ohlcv
window → 봉 개수 변환: 1M-D=20, 3M-D=60, 6M-D=120, 1Y-D=240, 1Y-W=52, 2Y-W=104, MAX-W=전체
"""
from __future__ import annotations

from decimal import Decimal
from typing import Optional

from src.chart_analysis.domain.value_objects import Candle

# ---------------------------------------------------------------------------
# 윈도우/간격 → 최근 봉 개수 매핑 (MAX = None → 제한 없음)
# ---------------------------------------------------------------------------
_WINDOW_TO_BAR_LIMIT: dict[tuple[str, str], Optional[int]] = {
    ("1M", "D"): 20,
    ("3M", "D"): 60,
    ("6M", "D"): 120,
    ("1Y", "D"): 240,
    ("1Y", "W"): 52,
    ("2Y", "W"): 104,
    ("MAX", "W"): None,
}

_INTERVAL_TO_TABLE: dict[str, str] = {
    "D": "market_daily_ohlcv",
    "W": "market_weekly_ohlcv",
}


class PostgresOhlcvRepository:
    """OhlcvRepository 포트를 구현하는 psycopg 어댑터."""

    def __init__(self, connect_fn) -> None:
        """connect_fn: 호출 시 psycopg 연결 컨텍스트 매니저를 반환하는 callable."""
        self._connect = connect_fn

    def find_window(self, symbol: str, window: str, interval: str) -> list[Candle]:
        """지정 종목/윈도우/간격의 OHLCV 봉 목록을 반환한다.

        Args:
            symbol: 종목코드 (예: '005930', 'AAPL')
            window: '1M' | '3M' | '6M' | '1Y' | '2Y' | 'MAX'
            interval: 'D' (일봉) | 'W' (주봉)

        Returns:
            list[Candle] — 날짜 오름차순 정렬, 모든 가격 Decimal.
        """
        window_key = (window, interval)
        if window_key not in _WINDOW_TO_BAR_LIMIT:
            raise ValueError(f"지원하지 않는 window/interval: {window!r}/{interval!r}")
        if interval not in _INTERVAL_TO_TABLE:
            raise ValueError(f"지원하지 않는 interval: {interval!r}. 가능: D, W")

        table = _INTERVAL_TO_TABLE[interval]
        limit = _WINDOW_TO_BAR_LIMIT[window_key]

        query, params = self._build_query(table, symbol, limit)

        with self._connect() as conn:
            with conn.cursor() as cur:
                cur.execute(query, params)
                rows = cur.fetchall()

        return [self._row_to_candle(row) for row in rows]

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    @staticmethod
    def _build_query(table: str, symbol: str, limit: Optional[int]) -> tuple[str, tuple]:
        if limit is not None:
            query = (
                "SELECT trade_date, open_price, high_price, low_price, close_price, volume "
                "FROM ("
                f"  SELECT trade_date, open_price, high_price, low_price, close_price, volume "
                f"  FROM {table} "
                f"  WHERE symbol = %s "
                f"  ORDER BY trade_date DESC "
                f"  LIMIT %s"
                ") recent "
                "ORDER BY trade_date ASC"
            )
            return query, (symbol, limit)
        else:
            # MAX 윈도우: 전체 기간
            query = (
                f"SELECT trade_date, open_price, high_price, low_price, close_price, volume "
                f"FROM {table} "
                f"WHERE symbol = %s "
                f"ORDER BY trade_date ASC"
            )
            return query, (symbol,)

    @staticmethod
    def _row_to_candle(row: tuple) -> Candle:
        trade_date, open_price, high_price, low_price, close_price, volume = row
        return Candle(
            date=trade_date,
            open=Decimal(str(open_price)),
            high=Decimal(str(high_price)),
            low=Decimal(str(low_price)),
            close=Decimal(str(close_price)),
            volume=Decimal(str(volume)),
        )
