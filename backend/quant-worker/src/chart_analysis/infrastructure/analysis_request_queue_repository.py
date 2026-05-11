"""PostgresAnalysisRequestQueueRepository — AnalysisRequestQueueRepository 포트 어댑터.

enqueue: INSERT ... ON CONFLICT ... DO UPDATE SET requested_count = requested_count + 1
fetch_pending: status='pending' FIFO 정렬, LIMIT N
mark_processed: UPDATE status + processed_at
"""
from __future__ import annotations


from chart_analysis.domain.ports import QueueItem


class PostgresAnalysisRequestQueueRepository:
    """AnalysisRequestQueueRepository 포트를 구현하는 psycopg 어댑터."""

    def __init__(self, connect_fn) -> None:
        self._connect = connect_fn

    # ------------------------------------------------------------------
    # enqueue
    # ------------------------------------------------------------------

    def enqueue(self, symbol: str, window: str, interval: str) -> None:
        """분석 요청을 큐에 등록한다.

        동일 (symbol, window, interval) 조합이 pending/processing 상태이면
        새 row 대신 requested_count를 증가시킨다.
        """
        query = _ENQUEUE_SQL
        with self._connect() as conn:
            with conn.cursor() as cur:
                cur.execute(query, (symbol, window, interval))
            conn.commit()

    # ------------------------------------------------------------------
    # fetch_pending
    # ------------------------------------------------------------------

    def fetch_pending(self, limit: int) -> list[QueueItem]:
        """pending 상태 아이템을 requested_at ASC 정렬로 limit개 반환한다."""
        query = (
            "SELECT id, symbol, window, interval, status, requested_at "
            "FROM analysis_request_queue "
            "WHERE status = 'pending' "
            "ORDER BY requested_at ASC "
            "LIMIT %s"
        )
        with self._connect() as conn:
            with conn.cursor() as cur:
                cur.execute(query, (limit,))
                rows = cur.fetchall()
        return [self._row_to_item(row) for row in rows]

    # ------------------------------------------------------------------
    # mark_processed
    # ------------------------------------------------------------------

    def mark_processed(self, id: int, status: str) -> None:
        """아이템 처리 상태를 갱신하고 processed_at을 기록한다."""
        query = (
            "UPDATE analysis_request_queue "
            "SET status = %s, processed_at = NOW() "
            "WHERE id = %s"
        )
        with self._connect() as conn:
            with conn.cursor() as cur:
                cur.execute(query, (status, id))
            conn.commit()

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    @staticmethod
    def _row_to_item(row: tuple) -> QueueItem:
        id_, symbol, window, interval, status, requested_at = row
        return QueueItem(
            id=id_,
            symbol=symbol,
            window=window,
            interval=interval,
            status=status,
        )


# ---------------------------------------------------------------------------
# SQL
# ---------------------------------------------------------------------------

_ENQUEUE_SQL = """
INSERT INTO analysis_request_queue (symbol, window, interval, status, requested_count, requested_at)
VALUES (%s, %s, %s, 'pending', 1, NOW())
ON CONFLICT (symbol, window, interval) WHERE status IN ('pending', 'processing')
DO UPDATE SET requested_count = analysis_request_queue.requested_count + 1
"""
