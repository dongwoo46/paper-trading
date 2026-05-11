"""단위 테스트: RedisJobStore."""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "..", "src"))

from unittest.mock import MagicMock, patch

import pytest

from chart_analysis.infrastructure.redis_job_store import RedisJobStore


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _make_store() -> tuple[RedisJobStore, MagicMock]:
    """redis.Redis mock을 주입한 RedisJobStore 인스턴스 반환."""
    mock_redis = MagicMock()
    store = RedisJobStore.__new__(RedisJobStore)
    store._redis = mock_redis
    store._ttl = 600
    return store, mock_redis


# ---------------------------------------------------------------------------
# set_status 테스트
# ---------------------------------------------------------------------------

class TestSetStatus:
    """set_status(job_id, stage) — TTL 포함 Redis 저장."""

    def test_set_status_calls_setex(self):
        """setex 또는 set+expire 호출 확인."""
        store, mock_redis = _make_store()
        store.set_status("job-123", "running")

        # setex 또는 set이 호출되어야 함
        assert mock_redis.setex.called or mock_redis.set.called, \
            "set_status가 Redis에 값을 저장해야 합니다."

    def test_set_status_includes_job_id(self):
        store, mock_redis = _make_store()
        store.set_status("job-abc", "pending")

        # 모든 call의 인자에서 job_id 포함 여부 확인
        all_calls = str(mock_redis.setex.call_args_list) + str(mock_redis.set.call_args_list)
        assert "job-abc" in all_calls, "job_id가 Redis 키에 포함되어야 합니다."

    def test_set_status_applies_ttl(self):
        """TTL이 적용되어야 함 (setex 또는 set + expire)."""
        store, mock_redis = _make_store()
        store.set_status("job-123", "done")

        if mock_redis.setex.called:
            args = mock_redis.setex.call_args[0]
            # setex(key, ttl, value) — ttl이 양수여야 함
            assert args[1] > 0, "TTL이 양수여야 합니다."
        elif mock_redis.set.called:
            # set + expire 패턴
            assert mock_redis.expire.called or "ex" in str(mock_redis.set.call_args), \
                "TTL이 설정되어야 합니다."


# ---------------------------------------------------------------------------
# get_status 테스트
# ---------------------------------------------------------------------------

class TestGetStatus:
    """get_status(job_id) — 없으면 None 반환."""

    def test_get_status_returns_value(self):
        store, mock_redis = _make_store()
        mock_redis.get.return_value = b"running"

        result = store.get_status("job-123")
        assert result == "running"

    def test_get_status_returns_none_when_missing(self):
        store, mock_redis = _make_store()
        mock_redis.get.return_value = None

        result = store.get_status("job-404")
        assert result is None

    def test_get_status_decodes_bytes(self):
        """bytes 반환 시 str로 디코딩."""
        store, mock_redis = _make_store()
        mock_redis.get.return_value = b"completed"

        result = store.get_status("job-123")
        assert isinstance(result, str)
        assert result == "completed"


# ---------------------------------------------------------------------------
# acquire_lock 테스트
# ---------------------------------------------------------------------------

class TestAcquireLock:
    """acquire_lock(snapshot_hash, ttl) — SETNX 동작."""

    def test_acquire_lock_returns_true_on_success(self):
        store, mock_redis = _make_store()
        mock_redis.setnx.return_value = 1  # 성공 (키 없음)
        mock_redis.set.return_value = True

        result = store.acquire_lock("hash-abc", ttl=600)
        assert result is True

    def test_acquire_lock_returns_false_when_locked(self):
        """이미 잠겨있으면 False."""
        store, mock_redis = _make_store()
        mock_redis.setnx.return_value = 0  # 실패 (키 존재)
        mock_redis.set.return_value = None  # set NX 실패

        result = store.acquire_lock("hash-abc", ttl=600)
        assert result is False

    def test_acquire_lock_uses_hash_as_key(self):
        store, mock_redis = _make_store()
        mock_redis.setnx.return_value = 1
        mock_redis.set.return_value = True

        store.acquire_lock("myhash123", ttl=300)

        all_calls = (
            str(mock_redis.setnx.call_args_list) +
            str(mock_redis.set.call_args_list)
        )
        assert "myhash123" in all_calls, "snapshot_hash가 락 키에 포함되어야 합니다."


# ---------------------------------------------------------------------------
# release_lock 테스트
# ---------------------------------------------------------------------------

class TestReleaseLock:
    """release_lock(snapshot_hash) — Redis 키 삭제."""

    def test_release_lock_deletes_key(self):
        store, mock_redis = _make_store()
        store.release_lock("hash-abc")
        assert mock_redis.delete.called, "release_lock이 Redis 키를 삭제해야 합니다."

    def test_release_lock_uses_correct_key(self):
        store, mock_redis = _make_store()
        store.release_lock("specific-hash-xyz")

        all_calls = str(mock_redis.delete.call_args_list)
        assert "specific-hash-xyz" in all_calls, "올바른 키를 삭제해야 합니다."
