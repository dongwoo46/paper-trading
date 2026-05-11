"""단위 테스트: PostgresAnalysisRequestQueueRepository."""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "..", "src"))

from datetime import datetime, timezone
from unittest.mock import MagicMock, call

import pytest

from chart_analysis.infrastructure.analysis_request_queue_repository import (
    PostgresAnalysisRequestQueueRepository,
)
from chart_analysis.domain.ports import QueueItem


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_repo() -> tuple[PostgresAnalysisRequestQueueRepository, MagicMock, MagicMock]:
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_cursor.__enter__ = MagicMock(return_value=mock_cursor)
    mock_cursor.__exit__ = MagicMock(return_value=False)
    mock_conn.cursor.return_value = mock_cursor
    mock_conn.__enter__ = MagicMock(return_value=mock_conn)
    mock_conn.__exit__ = MagicMock(return_value=False)

    repo = PostgresAnalysisRequestQueueRepository.__new__(
        PostgresAnalysisRequestQueueRepository
    )
    repo._connect = MagicMock(return_value=mock_conn)
    return repo, mock_conn, mock_cursor


# ---------------------------------------------------------------------------
# enqueue 테스트
# ---------------------------------------------------------------------------

class TestEnqueue:
    """enqueue — 중복 요청 시 requested_count 증가 검증."""

    def test_enqueue_executes_insert(self):
        repo, _, mock_cursor = _make_repo()
        repo.enqueue("005930", "3M", "D")
        assert mock_cursor.execute.called

    def test_enqueue_sql_has_on_conflict_increment(self):
        repo, _, mock_cursor = _make_repo()
        repo.enqueue("005930", "3M", "D")

        query: str = mock_cursor.execute.call_args[0][0].upper()
        assert "ON CONFLICT" in query, "ON CONFLICT 구문 필요"
        assert "DO UPDATE" in query, "DO UPDATE 구문 필요"
        assert "REQUESTED_COUNT" in query, "requested_count 증가 구문 필요"

    def test_enqueue_params_include_key_fields(self):
        repo, _, mock_cursor = _make_repo()
        repo.enqueue("035720", "6M", "W")

        params = mock_cursor.execute.call_args[0][1]
        assert "035720" in params
        assert "6M" in params
        assert "W" in params


# ---------------------------------------------------------------------------
# fetch_pending 테스트
# ---------------------------------------------------------------------------

def _make_queue_row(id: int = 1, symbol: str = "005930",
                    window: str = "1M", interval: str = "D") -> tuple:
    return (id, symbol, window, interval, "pending",
            datetime(2026, 5, 11, 8, 0, 0, tzinfo=timezone.utc))


class TestFetchPending:
    """fetch_pending(limit) → pending FIFO 정렬 검증."""

    def test_fetch_pending_queries_status_pending(self):
        repo, _, mock_cursor = _make_repo()
        mock_cursor.fetchall.return_value = []
        repo.fetch_pending(10)

        query: str = mock_cursor.execute.call_args[0][0].upper()
        assert "PENDING" in query, "status='pending' 필터가 없습니다."

    def test_fetch_pending_applies_limit(self):
        repo, _, mock_cursor = _make_repo()
        mock_cursor.fetchall.return_value = []
        repo.fetch_pending(5)

        params = mock_cursor.execute.call_args[0][1]
        assert 5 in params, "limit 파라미터가 없습니다."

    def test_fetch_pending_returns_queue_items(self):
        repo, _, mock_cursor = _make_repo()
        mock_cursor.fetchall.return_value = [_make_queue_row(1), _make_queue_row(2)]
        result = repo.fetch_pending(10)
        assert len(result) == 2

    def test_fetch_pending_items_have_correct_type(self):
        repo, _, mock_cursor = _make_repo()
        mock_cursor.fetchall.return_value = [_make_queue_row(1)]
        result = repo.fetch_pending(10)
        assert isinstance(result[0], QueueItem)

    def test_fetch_pending_fifo_order(self):
        """requested_at ASC 정렬 쿼리 확인."""
        repo, _, mock_cursor = _make_repo()
        mock_cursor.fetchall.return_value = []
        repo.fetch_pending(10)

        query: str = mock_cursor.execute.call_args[0][0].upper()
        assert "ORDER BY" in query and "REQUESTED_AT" in query, \
            "FIFO 정렬(ORDER BY requested_at)이 없습니다."

    def test_fetch_pending_empty(self):
        repo, _, mock_cursor = _make_repo()
        mock_cursor.fetchall.return_value = []
        result = repo.fetch_pending(10)
        assert result == []


# ---------------------------------------------------------------------------
# mark_processed 테스트
# ---------------------------------------------------------------------------

class TestMarkProcessed:
    """mark_processed(id, status) → 상태 갱신 + processed_at 기록."""

    def test_mark_processed_executes_update(self):
        repo, _, mock_cursor = _make_repo()
        repo.mark_processed(42, "completed")
        assert mock_cursor.execute.called

    def test_mark_processed_updates_status(self):
        repo, _, mock_cursor = _make_repo()
        repo.mark_processed(42, "completed")

        query: str = mock_cursor.execute.call_args[0][0].upper()
        assert "UPDATE" in query and "STATUS" in query

    def test_mark_processed_sets_processed_at(self):
        repo, _, mock_cursor = _make_repo()
        repo.mark_processed(42, "failed")

        query: str = mock_cursor.execute.call_args[0][0].upper()
        assert "PROCESSED_AT" in query, "processed_at 갱신이 없습니다."

    @pytest.mark.parametrize("status", ["completed", "failed"])
    def test_mark_processed_accepts_valid_statuses(self, status: str):
        repo, _, mock_cursor = _make_repo()
        repo.mark_processed(1, status)  # 예외 없이 실행

        params = mock_cursor.execute.call_args[0][1]
        assert status in params
