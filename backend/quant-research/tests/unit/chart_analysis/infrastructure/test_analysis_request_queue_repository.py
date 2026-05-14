"""AnalysisRequestQueueRepository 단위 테스트 — mock connect_fn 사용."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

from datetime import datetime, timezone
from unittest.mock import MagicMock

import pytest

from chart_analysis.infrastructure.analysis_request_queue_repository import (
    PostgresAnalysisRequestQueueRepository,
)
from chart_analysis.domain.ports import QueueItem


def _make_connect_fn(rows=None):
    """mock connect_fn — (connect_fn, mock_conn, mock_cur) 반환."""
    mock_cur = MagicMock()
    mock_cur.__enter__ = lambda s: s
    mock_cur.__exit__ = MagicMock(return_value=False)
    if rows is not None:
        mock_cur.fetchall.return_value = rows

    mock_conn = MagicMock()
    mock_conn.__enter__ = lambda s: s
    mock_conn.__exit__ = MagicMock(return_value=False)
    mock_conn.cursor.return_value = mock_cur

    connect_fn = MagicMock()
    connect_fn.return_value = mock_conn
    return connect_fn, mock_conn, mock_cur


class TestPostgresAnalysisRequestQueueRepository:

    def test_enqueue_executes_upsert_sql(self):
        """enqueue() → SQL 실행 확인."""
        connect_fn, mock_conn, mock_cur = _make_connect_fn()
        repo = PostgresAnalysisRequestQueueRepository(connect_fn)

        repo.enqueue("005930", "1M", "D")

        assert mock_cur.execute.called
        sql_arg = mock_cur.execute.call_args[0][0]
        assert "INSERT" in sql_arg.upper()
        assert mock_conn.commit.called

    def test_fetch_pending_returns_queue_items(self):
        """fetch_pending() → QueueItem 목록 변환 검증."""
        now = datetime.now(timezone.utc)
        row = (1, "005930", "1M", "D", "pending", now)
        connect_fn, mock_conn, mock_cur = _make_connect_fn(rows=[row])
        repo = PostgresAnalysisRequestQueueRepository(connect_fn)

        items = repo.fetch_pending(limit=10)

        assert len(items) == 1
        item = items[0]
        # isinstance 대신 속성으로 검증 (class identity 문제 회피)
        assert item.id == 1
        assert item.symbol == "005930"
        assert item.window == "1M"
        assert item.interval == "D"
        assert item.status == "pending"

    def test_mark_processed_updates_status(self):
        """mark_processed() → UPDATE SQL 실행 확인."""
        connect_fn, mock_conn, mock_cur = _make_connect_fn()
        repo = PostgresAnalysisRequestQueueRepository(connect_fn)

        repo.mark_processed(42, "processed")

        assert mock_cur.execute.called
        call_args = mock_cur.execute.call_args[0]
        sql = call_args[0]
        params = call_args[1]
        assert "UPDATE" in sql.upper()
        assert "processed" in params
        assert 42 in params
        assert mock_conn.commit.called
