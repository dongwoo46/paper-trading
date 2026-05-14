"""GenerateReportService — LLM 백그라운드 태스크 Application Service."""
from __future__ import annotations

import asyncio
import logging
from typing import Callable, Optional

from src.chart_analysis.domain.ports import LlmReportGenerator

logger = logging.getLogger(__name__)


class GenerateReportService:
    """LLM 자연어 리포트 백그라운드 생성 서비스.

    흐름:
    1. prepare() — 실행 가능 여부 확인 + Redis 락 획득
    2. run_background() — BackgroundTask 로 실행. LLM 생성 → DB 저장 → Slack 알림 → SSE 브로드캐스트
    """

    def __init__(
        self,
        chart_analysis_repository,
        llm_report_generator: LlmReportGenerator,
        redis_job_store,
        popular_symbols_fn: Callable[[str], bool],
        slack_notifier,
        symbol_name_fn: Callable[[str], Optional[str]],
    ) -> None:
        self._repo = chart_analysis_repository
        self._llm = llm_report_generator
        self._redis = redis_job_store
        self._is_popular = popular_symbols_fn
        self._slack = slack_notifier
        self._symbol_name = symbol_name_fn

    async def prepare(self, symbol: str, window: str, interval: str) -> dict:
        """LLM 백그라운드 실행 준비.

        반환:
            {"runnable": bool, "status": str, "snapshot_hash": str | None}
        """
        result = self._repo.find_one(symbol, window, interval)
        if result is None:
            return {"runnable": False, "status": "not_found", "snapshot_hash": None}

        if result.report is not None and result.report_source.value == "llm_primary":
            return {"runnable": False, "status": "already_done", "snapshot_hash": None}

        if not self._is_popular(symbol):
            return {"runnable": False, "status": "not_popular", "snapshot_hash": None}

        snapshot_hash = result.snapshot_hash
        try:
            lock_acquired = self._redis.acquire_lock(snapshot_hash, ttl=600)
        except Exception as exc:  # noqa: BLE001
            logger.warning(
                "generate_report:redis_lock_unavailable symbol=%s error=%s", symbol, str(exc)
            )
            lock_acquired = True

        if not lock_acquired:
            return {"runnable": False, "status": "pending", "snapshot_hash": None}

        return {"runnable": True, "status": "queued", "snapshot_hash": snapshot_hash}

    async def run_background(
        self, symbol: str, window: str, interval: str, snapshot_hash: str
    ) -> None:
        """LLM 생성 + DB 저장. FastAPI BackgroundTask 로 실행된다."""
        try:
            result = self._repo.find_one(symbol, window, interval)
            if result is None:
                logger.warning(
                    "generate_report:result_not_found symbol=%s window=%s", symbol, window
                )
                return

            name: str = self._symbol_name(symbol) or symbol

            # 동기 LLM 호출을 스레드풀에서 실행 (이벤트 루프 블로킹 방지)
            llm_report = await asyncio.to_thread(self._llm.generate, None, result)
            updated = result.with_report(llm_report, llm_report.source)
            self._repo.upsert(updated)
            logger.info("generate_report:llm_done symbol=%s name=%s window=%s", symbol, name, window)

            await self._slack.notify_analysis_success(
                symbol, name, window, source=llm_report.source.value
            )

            from src.chart_analysis.infrastructure.notification_hub import broadcast
            await broadcast({
                "type": "llm_done",
                "symbol": symbol,
                "name": name,
                "window": window,
                "interval": interval,
                "source": llm_report.source.value,
            })

        except Exception as exc:  # noqa: BLE001
            logger.warning(
                "generate_report:llm_error symbol=%s window=%s error=%s", symbol, window, str(exc)
            )
            try:
                name = self._symbol_name(symbol) or symbol
                await self._slack.notify_analysis_failure(symbol, name, window, error=str(exc))
            except Exception:  # noqa: BLE001
                pass
        finally:
            try:
                self._redis.release_lock(snapshot_hash)
            except Exception:  # noqa: BLE001
                pass
