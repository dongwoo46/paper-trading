"""Substep 5-8: SlackWebhookNotifier 단위 테스트."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

import json
from typing import List
from unittest.mock import AsyncMock, MagicMock, patch

import pytest


class TestSlackWebhookNotifierDisabled:
    """SLACK_NOTIFICATIONS_ENABLED=false → POST 호출 안 함."""

    @pytest.mark.asyncio
    async def test_disabled_no_http_post(self, monkeypatch):
        monkeypatch.setenv("SLACK_NOTIFICATIONS_ENABLED", "false")
        monkeypatch.setenv("SLACK_WEBHOOK_URL", "https://hooks.slack.com/test")

        # Re-import after env change
        import importlib
        import chart_analysis.infrastructure.slack_notifier as module
        importlib.reload(module)
        SlackWebhookNotifier = module.SlackWebhookNotifier

        call_log = []

        with patch("httpx.AsyncClient") as mock_client_class:
            mock_client = AsyncMock()
            mock_client.post = AsyncMock(side_effect=lambda *a, **kw: call_log.append(kw) or MagicMock())
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_client_class.return_value = mock_client

            notifier = SlackWebhookNotifier()
            await notifier.notify_analysis_failure("005930", "1M", "test error")
            await notifier.notify_analysis_success("005930", "1M", "llm_primary")

        assert len(call_log) == 0, "disabled 상태에서 POST 호출이 발생함"


class TestSlackWebhookNotifierEnabled:
    """알림 활성화 시 POST body 검증."""

    @pytest.mark.asyncio
    async def test_notify_analysis_failure_posts_symbol_window_error(self, monkeypatch):
        monkeypatch.setenv("SLACK_NOTIFICATIONS_ENABLED", "true")
        monkeypatch.setenv("SLACK_WEBHOOK_URL", "https://hooks.slack.com/test")

        import importlib
        import chart_analysis.infrastructure.slack_notifier as module
        importlib.reload(module)
        SlackWebhookNotifier = module.SlackWebhookNotifier

        posted_data = []

        async def _fake_post(url, json=None, **kwargs):
            posted_data.append(json)
            mock_resp = MagicMock()
            mock_resp.raise_for_status = MagicMock()
            return mock_resp

        with patch("httpx.AsyncClient") as mock_client_class:
            mock_client = AsyncMock()
            mock_client.post = AsyncMock(side_effect=_fake_post)
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_client_class.return_value = mock_client

            notifier = SlackWebhookNotifier()
            await notifier.notify_analysis_failure("005930", "1M", "LLM timeout")

        assert len(posted_data) == 1
        payload = posted_data[0]
        text = payload.get("text", "")
        assert "005930" in text
        assert "1M" in text
        assert "LLM timeout" in text

    @pytest.mark.asyncio
    async def test_notify_analysis_success_posts_source(self, monkeypatch):
        monkeypatch.setenv("SLACK_NOTIFICATIONS_ENABLED", "true")
        monkeypatch.setenv("SLACK_WEBHOOK_URL", "https://hooks.slack.com/test")

        import importlib
        import chart_analysis.infrastructure.slack_notifier as module
        importlib.reload(module)
        SlackWebhookNotifier = module.SlackWebhookNotifier

        posted_data = []

        async def _fake_post(url, json=None, **kwargs):
            posted_data.append(json)
            mock_resp = MagicMock()
            mock_resp.raise_for_status = MagicMock()
            return mock_resp

        with patch("httpx.AsyncClient") as mock_client_class:
            mock_client = AsyncMock()
            mock_client.post = AsyncMock(side_effect=_fake_post)
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_client_class.return_value = mock_client

            notifier = SlackWebhookNotifier()
            await notifier.notify_analysis_success("005930", "3M", "llm_primary")

        assert len(posted_data) == 1
        payload = posted_data[0]
        text = payload.get("text", "")
        assert "005930" in text
        assert "3M" in text
        assert "llm_primary" in text

    @pytest.mark.asyncio
    async def test_notify_batch_completed_posts_summary(self, monkeypatch):
        monkeypatch.setenv("SLACK_NOTIFICATIONS_ENABLED", "true")
        monkeypatch.setenv("SLACK_WEBHOOK_URL", "https://hooks.slack.com/test")

        import importlib
        import chart_analysis.infrastructure.slack_notifier as module
        importlib.reload(module)
        SlackWebhookNotifier = module.SlackWebhookNotifier

        posted_data = []

        async def _fake_post(url, json=None, **kwargs):
            posted_data.append(json)
            mock_resp = MagicMock()
            mock_resp.raise_for_status = MagicMock()
            return mock_resp

        with patch("httpx.AsyncClient") as mock_client_class:
            mock_client = AsyncMock()
            mock_client.post = AsyncMock(side_effect=_fake_post)
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_client_class.return_value = mock_client

            notifier = SlackWebhookNotifier()
            await notifier.notify_batch_completed("KRX", success_count=150, failed_count=3)

        assert len(posted_data) == 1
        payload = posted_data[0]
        text = payload.get("text", "")
        assert "KRX" in text
        assert "150" in text
        assert "3" in text


class TestSlackWebhookNotifierHttpError:
    """HTTP 오류 → 예외 비전파 (로그만)."""

    @pytest.mark.asyncio
    async def test_http_error_does_not_propagate(self, monkeypatch):
        monkeypatch.setenv("SLACK_NOTIFICATIONS_ENABLED", "true")
        monkeypatch.setenv("SLACK_WEBHOOK_URL", "https://hooks.slack.com/test")

        import importlib
        import chart_analysis.infrastructure.slack_notifier as module
        importlib.reload(module)
        SlackWebhookNotifier = module.SlackWebhookNotifier

        import httpx

        async def _failing_post(url, json=None, **kwargs):
            raise httpx.HTTPError("connection refused")

        with patch("httpx.AsyncClient") as mock_client_class:
            mock_client = AsyncMock()
            mock_client.post = AsyncMock(side_effect=_failing_post)
            mock_client.__aenter__ = AsyncMock(return_value=mock_client)
            mock_client.__aexit__ = AsyncMock(return_value=None)
            mock_client_class.return_value = mock_client

            notifier = SlackWebhookNotifier()
            # 예외가 전파되어서는 안 된다
            await notifier.notify_analysis_failure("005930", "1M", "test error")
            await notifier.notify_analysis_success("005930", "1M", "llm_primary")
            await notifier.notify_batch_completed("KRX", 10, 2)
