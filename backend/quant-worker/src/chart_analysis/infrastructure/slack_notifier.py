"""SlackWebhookNotifier — 차트 분석 Slack 알림 어댑터.

환경변수:
    SLACK_WEBHOOK_URL             = (기존 quant-worker 공유)
    SLACK_NOTIFICATIONS_ENABLED   = true | false (기본: false)

알림 실패 → 로그만, 예외 전파 금지 (메인 플로우 차단 금지).
기존 batch_schedule.py의 Slack 패턴과 일관성 유지.
"""
from __future__ import annotations

import logging
import os

logger = logging.getLogger(__name__)

_DEFAULT_TIMEOUT = 5.0


def _is_enabled() -> bool:
    return os.getenv("SLACK_NOTIFICATIONS_ENABLED", "false").lower() in ("1", "true", "yes", "on")


def _webhook_url() -> str:
    return os.getenv("SLACK_WEBHOOK_URL", "")


class SlackWebhookNotifier:
    """차트 분석 이벤트 Slack Webhook 알림기.

    비동기 메서드 제공. 실패 시 예외 비전파.
    """

    async def notify_analysis_success(
        self, symbol: str, window: str, source: str
    ) -> None:
        """LLM 리포트 생성 성공 알림.

        Args:
            symbol: 종목 코드
            window: 분석 윈도우 (예: '1M', '3M')
            source: 리포트 소스 ('llm_primary' | 'rule_template')
        """
        if not _is_enabled():
            return
        text = (
            f":white_check_mark: *chart_analysis 리포트 생성 완료*\n"
            f"symbol={symbol} window={window} source={source}"
        )
        await self._post(text)

    async def notify_analysis_failure(
        self, symbol: str, window: str, error: str
    ) -> None:
        """LLM 리포트 생성 실패 알림.

        Args:
            symbol: 종목 코드
            window: 분석 윈도우
            error: 에러 메시지 (200자 이내로 잘림)
        """
        if not _is_enabled():
            return
        safe_error = error[:200]
        text = (
            f":rotating_light: *chart_analysis 리포트 생성 실패*\n"
            f"symbol={symbol} window={window}\n"
            f"error: {safe_error}"
        )
        await self._post(text)

    async def notify_batch_completed(
        self, market: str, success_count: int, failed_count: int
    ) -> None:
        """배치 파이프라인 완료 알림.

        Args:
            market: 시장 ('KRX' | 'US')
            success_count: 성공 종목 수
            failed_count: 실패 종목 수
        """
        if not _is_enabled():
            return
        icon = ":white_check_mark:" if failed_count == 0 else ":warning:"
        text = (
            f"{icon} *chart_analysis 배치 완료*\n"
            f"market={market} success={success_count} failed={failed_count}"
        )
        await self._post(text)

    async def _post(self, text: str) -> None:
        """Slack Webhook POST. 실패 시 로그만, 예외 전파 금지."""
        webhook_url = _webhook_url()
        if not webhook_url:
            logger.debug("notify_slack:no_webhook_url configured")
            return
        try:
            import httpx
            async with httpx.AsyncClient(timeout=_DEFAULT_TIMEOUT) as client:
                resp = await client.post(webhook_url, json={"text": text})
                resp.raise_for_status()
        except Exception as exc:  # noqa: BLE001
            logger.warning("notify_slack:error error=%s", str(exc))
