"""NotificationHub — 인메모리 SSE 브로드캐스트.

연결된 SSE 클라이언트에 이벤트를 전송한다.
서버 재시작 시 구독 목록은 초기화된다 (재연결은 클라이언트 책임).
"""
from __future__ import annotations

import asyncio
import logging
from typing import Set

logger = logging.getLogger(__name__)

_subscribers: Set[asyncio.Queue] = set()


def subscribe() -> asyncio.Queue:
    """새 SSE 클라이언트를 구독 목록에 등록하고 전용 큐를 반환한다."""
    q: asyncio.Queue = asyncio.Queue(maxsize=32)
    _subscribers.add(q)
    logger.debug("notification_hub:subscribe total=%d", len(_subscribers))
    return q


def unsubscribe(q: asyncio.Queue) -> None:
    """SSE 클라이언트 연결 종료 시 구독 해제."""
    _subscribers.discard(q)
    logger.debug("notification_hub:unsubscribe total=%d", len(_subscribers))


async def broadcast(event: dict) -> None:
    """연결된 모든 클라이언트에 이벤트를 전송한다. 큐가 가득 찬 클라이언트는 스킵."""
    for q in list(_subscribers):
        try:
            q.put_nowait(event)
        except asyncio.QueueFull:
            logger.warning("notification_hub:queue_full skipping_client")
