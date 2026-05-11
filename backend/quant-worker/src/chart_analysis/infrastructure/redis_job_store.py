"""RedisJobStore — SSE 진행 상태 + 중복 잠금 관리.

환경변수:
    REDIS_HOST                   = redis
    REDIS_PORT                   = 6379
    CHART_ANALYSIS_JOB_TTL_S     = 600
"""
from __future__ import annotations

import os
from typing import Optional

_KEY_PREFIX_STATUS = "chart_analysis:job:"
_KEY_PREFIX_LOCK = "chart_analysis:lock:"

_DEFAULT_HOST = "redis"
_DEFAULT_PORT = 6379
_DEFAULT_TTL = 600


class RedisJobStore:
    """Redis를 사용하여 분석 작업 상태와 중복 요청 잠금을 관리한다."""

    def __init__(self, redis_client=None) -> None:
        """redis_client: 외부 주입 가능 (테스트/프로덕션 모두 지원).

        redis_client가 None이면 환경변수로 연결한다.
        """
        if redis_client is not None:
            self._redis = redis_client
        else:
            import redis  # type: ignore[import]
            host = os.getenv("REDIS_HOST", _DEFAULT_HOST)
            port = int(os.getenv("REDIS_PORT", str(_DEFAULT_PORT)))
            self._redis = redis.Redis(host=host, port=port, decode_responses=False)

        self._ttl = int(os.getenv("CHART_ANALYSIS_JOB_TTL_S", str(_DEFAULT_TTL)))

    # ------------------------------------------------------------------
    # 작업 상태 관리
    # ------------------------------------------------------------------

    def set_status(self, job_id: str, stage: str) -> None:
        """작업 상태를 TTL 포함하여 저장한다."""
        key = _KEY_PREFIX_STATUS + job_id
        self._redis.setex(key, self._ttl, stage.encode("utf-8"))

    def get_status(self, job_id: str) -> Optional[str]:
        """작업 상태를 조회한다. 없으면 None."""
        key = _KEY_PREFIX_STATUS + job_id
        value = self._redis.get(key)
        if value is None:
            return None
        if isinstance(value, bytes):
            return value.decode("utf-8")
        return str(value)

    # ------------------------------------------------------------------
    # 중복 요청 잠금 (SETNX 패턴)
    # ------------------------------------------------------------------

    def acquire_lock(self, snapshot_hash: str, ttl: int) -> bool:
        """스냅샷 해시 기반 잠금을 획득한다.

        SETNX 동작: 키가 없으면 True(획득 성공), 이미 있으면 False(잠금 실패).
        TTL 적용으로 데드락 방지.
        """
        key = _KEY_PREFIX_LOCK + snapshot_hash
        result = self._redis.set(key, "1", nx=True, ex=ttl)
        return result is True or result == 1

    def release_lock(self, snapshot_hash: str) -> None:
        """스냅샷 해시 기반 잠금을 해제한다."""
        key = _KEY_PREFIX_LOCK + snapshot_hash
        self._redis.delete(key)
