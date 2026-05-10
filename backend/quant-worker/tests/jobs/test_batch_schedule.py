"""Tests for batch_schedule module — TDD Red phase written first, then Green.

Covers:
- Four batch definitions with correct IDs, providers, and KST schedules
- Provider routing (no provider="all" in automated runs)
- Timezone is Asia/Seoul
- Retry: exponential backoff, stops at max attempts
- noop_empty_universe on empty symbol set (no crash, no retry)
- partial_success triggers Slack/log notification
- no_new_bar is NOT treated as hard failure
- Slack notification boundary (env-configured only, mocked in tests)
"""
from __future__ import annotations

import os
from dataclasses import dataclass
from datetime import datetime
from typing import Literal
from unittest.mock import MagicMock, call, patch

import pytest

from src.jobs.batch_schedule import (
    BatchDefinition,
    BatchRunContext,
    BatchRunResult,
    BatchOutcome,
    build_default_batch_definitions,
    notify_slack,
    run_batch,
)


# ---------------------------------------------------------------------------
# Batch definition structure tests
# ---------------------------------------------------------------------------


class TestBatchDefinitions:
    def setup_method(self):
        self.defs = build_default_batch_definitions()
        self.def_map = {d.batch_id: d for d in self.defs}

    def test_exactly_four_definitions(self):
        assert len(self.defs) == 4

    def test_all_expected_ids_present(self):
        ids = {d.batch_id for d in self.defs}
        assert ids == {"kr_daily", "us_daily", "kr_weekly", "us_weekly"}

    def test_kr_daily_provider_is_pykrx(self):
        assert self.def_map["kr_daily"].provider == "pykrx"

    def test_us_daily_provider_is_yfinance(self):
        assert self.def_map["us_daily"].provider == "yfinance"

    def test_kr_weekly_provider_is_pykrx(self):
        assert self.def_map["kr_weekly"].provider == "pykrx"

    def test_us_weekly_provider_is_yfinance(self):
        assert self.def_map["us_weekly"].provider == "yfinance"

    def test_no_automated_batch_uses_provider_all(self):
        for d in self.defs:
            assert d.provider != "all", f"{d.batch_id} must not use provider='all'"

    def test_timezone_is_asia_seoul(self):
        for d in self.defs:
            assert d.timezone == "Asia/Seoul", f"{d.batch_id} must use Asia/Seoul"

    def test_kr_daily_interval_is_1d(self):
        assert self.def_map["kr_daily"].interval == "1d"

    def test_us_daily_interval_is_1d(self):
        assert self.def_map["us_daily"].interval == "1d"

    def test_kr_weekly_interval_is_1wk(self):
        assert self.def_map["kr_weekly"].interval == "1wk"

    def test_us_weekly_interval_is_1wk(self):
        assert self.def_map["us_weekly"].interval == "1wk"

    def test_kr_daily_market_is_KR(self):
        assert self.def_map["kr_daily"].market == "KR"

    def test_us_daily_market_is_US(self):
        assert self.def_map["us_daily"].market == "US"

    def test_kr_weekly_market_is_KR(self):
        assert self.def_map["kr_weekly"].market == "KR"

    def test_us_weekly_market_is_US(self):
        assert self.def_map["us_weekly"].market == "US"

    def test_kr_daily_cron_default(self):
        d = self.def_map["kr_daily"]
        # Mon-Fri 18:30 KST => minute=30, hour=18, day_of_week=mon-fri
        assert d.cron_minute == 30
        assert d.cron_hour == 18
        assert d.cron_day_of_week == "mon-fri"

    def test_us_daily_cron_default(self):
        d = self.def_map["us_daily"]
        # Tue-Sat 08:30 KST => minute=30, hour=8, day_of_week=tue-sat
        assert d.cron_minute == 30
        assert d.cron_hour == 8
        assert d.cron_day_of_week == "tue-sat"

    def test_kr_weekly_cron_default(self):
        d = self.def_map["kr_weekly"]
        # Fri 19:30 KST => minute=30, hour=19, day_of_week=fri
        assert d.cron_minute == 30
        assert d.cron_hour == 19
        assert d.cron_day_of_week == "fri"

    def test_us_weekly_cron_default(self):
        d = self.def_map["us_weekly"]
        # Sat 10:00 KST => minute=0, hour=10, day_of_week=sat
        assert d.cron_minute == 0
        assert d.cron_hour == 10
        assert d.cron_day_of_week == "sat"


# ---------------------------------------------------------------------------
# Provider routing tests (run_batch delegates correctly)
# ---------------------------------------------------------------------------


class TestRunBatchProviderRouting:
    """run_batch should call execute(DailyFetchOptions) or execute_weekly(WeeklyFetchOptions)."""

    def _make_daily_result(self, provider: str, symbols: int = 2, success: int = 2, failed: int = 0, rows: int = 10):
        return {
            "provider": provider,
            "symbols": symbols,
            "success_symbols": success,
            "failed_symbols": failed,
            "total_rows_inserted": rows,
            "start": "2024-01-01",
            "end": "2024-01-31",
        }

    def test_kr_daily_calls_execute_with_pykrx(self):
        defs = build_default_batch_definitions()
        kr_daily = next(d for d in defs if d.batch_id == "kr_daily")

        with (
            patch("src.jobs.batch_schedule.execute") as mock_execute,
            patch("src.jobs.batch_schedule.execute_weekly") as mock_execute_weekly,
        ):
            mock_execute.return_value = self._make_daily_result("pykrx")
            result = run_batch(kr_daily, attempt=1)

        mock_execute.assert_called_once()
        mock_execute_weekly.assert_not_called()
        call_args = mock_execute.call_args[0][0]
        assert call_args.provider == "pykrx"
        assert call_args.only_default is True

    def test_us_daily_calls_execute_with_yfinance(self):
        defs = build_default_batch_definitions()
        us_daily = next(d for d in defs if d.batch_id == "us_daily")

        with (
            patch("src.jobs.batch_schedule.execute") as mock_execute,
            patch("src.jobs.batch_schedule.execute_weekly") as mock_execute_weekly,
        ):
            mock_execute.return_value = self._make_daily_result("yfinance")
            result = run_batch(us_daily, attempt=1)

        mock_execute.assert_called_once()
        mock_execute_weekly.assert_not_called()
        call_args = mock_execute.call_args[0][0]
        assert call_args.provider == "yfinance"
        assert call_args.only_default is True

    def test_kr_weekly_calls_execute_weekly_with_pykrx(self):
        defs = build_default_batch_definitions()
        kr_weekly = next(d for d in defs if d.batch_id == "kr_weekly")

        with (
            patch("src.jobs.batch_schedule.execute") as mock_execute,
            patch("src.jobs.batch_schedule.execute_weekly") as mock_execute_weekly,
        ):
            mock_execute_weekly.return_value = self._make_daily_result("pykrx")
            result = run_batch(kr_weekly, attempt=1)

        mock_execute_weekly.assert_called_once()
        mock_execute.assert_not_called()
        call_args = mock_execute_weekly.call_args[0][0]
        assert call_args.provider == "pykrx"
        assert call_args.only_default is True

    def test_us_weekly_calls_execute_weekly_with_yfinance(self):
        defs = build_default_batch_definitions()
        us_weekly = next(d for d in defs if d.batch_id == "us_weekly")

        with (
            patch("src.jobs.batch_schedule.execute") as mock_execute,
            patch("src.jobs.batch_schedule.execute_weekly") as mock_execute_weekly,
        ):
            mock_execute_weekly.return_value = self._make_daily_result("yfinance")
            result = run_batch(us_weekly, attempt=1)

        mock_execute_weekly.assert_called_once()
        mock_execute.assert_not_called()
        call_args = mock_execute_weekly.call_args[0][0]
        assert call_args.provider == "yfinance"
        assert call_args.only_default is True


# ---------------------------------------------------------------------------
# Outcome classification tests
# ---------------------------------------------------------------------------


class TestOutcomeClassification:
    def _make_definition(self, batch_id: str = "kr_daily", provider: str = "pykrx", interval: str = "1d") -> BatchDefinition:
        defs = build_default_batch_definitions()
        return next(d for d in defs if d.batch_id == batch_id)

    def _service_result(
        self,
        symbols: int = 2,
        success: int = 2,
        failed: int = 0,
        rows: int = 10,
        provider: str = "pykrx",
    ) -> dict:
        return {
            "provider": provider,
            "symbols": symbols,
            "success_symbols": success,
            "failed_symbols": failed,
            "total_rows_inserted": rows,
            "start": "2024-01-01",
            "end": "2024-01-31",
        }

    def test_all_success_gives_success_outcome(self):
        defn = self._make_definition("kr_daily")
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            mock_exec.return_value = self._service_result(symbols=3, success=3, failed=0, rows=15)
            result = run_batch(defn, attempt=1)
        assert result.status == BatchOutcome.SUCCESS

    def test_zero_symbols_gives_noop_empty_universe(self):
        defn = self._make_definition("kr_daily")
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            mock_exec.return_value = self._service_result(symbols=0, success=0, failed=0, rows=0)
            result = run_batch(defn, attempt=1)
        assert result.status == BatchOutcome.NOOP_EMPTY_UNIVERSE

    def test_noop_empty_universe_does_not_crash(self):
        defn = self._make_definition("kr_daily")
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            mock_exec.return_value = self._service_result(symbols=0, success=0, failed=0, rows=0)
            # Should not raise
            result = run_batch(defn, attempt=1)
        assert result is not None

    def test_partial_success_outcome(self):
        defn = self._make_definition("kr_daily")
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            mock_exec.return_value = self._service_result(symbols=3, success=2, failed=1, rows=10)
            result = run_batch(defn, attempt=1)
        assert result.status == BatchOutcome.PARTIAL_SUCCESS

    def test_service_exception_gives_failed_outcome(self):
        defn = self._make_definition("kr_daily")
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            mock_exec.side_effect = RuntimeError("DB connection refused")
            result = run_batch(defn, attempt=1)
        assert result.status == BatchOutcome.FAILED
        assert result.error is not None

    def test_no_rows_inserted_all_skipped_gives_no_new_bar(self):
        """When all symbols succeed but 0 rows inserted and 0 failed, classify as no_new_bar."""
        defn = self._make_definition("kr_daily")
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            # symbols=2, success=2, failed=0, rows=0 => provider returned nothing new
            mock_exec.return_value = self._service_result(symbols=2, success=2, failed=0, rows=0)
            result = run_batch(defn, attempt=1)
        assert result.status == BatchOutcome.NO_NEW_BAR

    def test_no_new_bar_is_not_hard_failure(self):
        defn = self._make_definition("kr_daily")
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            mock_exec.return_value = self._service_result(symbols=2, success=2, failed=0, rows=0)
            result = run_batch(defn, attempt=1)
        # no_new_bar should NOT be treated as failed
        assert result.status != BatchOutcome.FAILED

    def test_result_has_structured_log_fields(self):
        defn = self._make_definition("kr_daily")
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            mock_exec.return_value = self._service_result(symbols=2, success=2, failed=0, rows=10)
            result = run_batch(defn, attempt=1)
        assert result.batch_id == "kr_daily"
        assert result.market == "KR"
        assert result.interval == "1d"
        assert result.provider == "pykrx"
        assert result.attempt == 1
        assert result.total_rows_inserted == 10
        assert result.elapsed_ms >= 0


# ---------------------------------------------------------------------------
# Retry policy tests
# ---------------------------------------------------------------------------


class TestRetryPolicy:
    """Verify exponential backoff and max attempt limits."""

    def _defn(self) -> BatchDefinition:
        defs = build_default_batch_definitions()
        return next(d for d in defs if d.batch_id == "kr_daily")

    def test_retry_not_triggered_for_success(self):
        """BatchRunResult.should_retry is False for success."""
        defn = self._defn()
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            mock_exec.return_value = {
                "provider": "pykrx",
                "symbols": 2,
                "success_symbols": 2,
                "failed_symbols": 0,
                "total_rows_inserted": 10,
                "start": "2024-01-01",
                "end": "2024-01-31",
            }
            result = run_batch(defn, attempt=1)
        assert result.should_retry is False

    def test_retry_not_triggered_for_noop_empty_universe(self):
        defn = self._defn()
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            mock_exec.return_value = {
                "provider": "pykrx",
                "symbols": 0,
                "success_symbols": 0,
                "failed_symbols": 0,
                "total_rows_inserted": 0,
                "start": "2024-01-01",
                "end": "2024-01-31",
            }
            result = run_batch(defn, attempt=1)
        assert result.should_retry is False

    def test_retry_triggered_for_partial_success(self):
        defn = self._defn()
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            mock_exec.return_value = {
                "provider": "pykrx",
                "symbols": 3,
                "success_symbols": 2,
                "failed_symbols": 1,
                "total_rows_inserted": 10,
                "start": "2024-01-01",
                "end": "2024-01-31",
            }
            result = run_batch(defn, attempt=1)
        assert result.should_retry is True

    def test_retry_triggered_for_failed(self):
        defn = self._defn()
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            mock_exec.side_effect = RuntimeError("network error")
            result = run_batch(defn, attempt=1)
        assert result.should_retry is True

    def test_retry_stops_at_max_attempts(self):
        """When attempt >= max_attempts (4), should_retry is False."""
        defn = self._defn()
        with patch("src.jobs.batch_schedule.execute") as mock_exec:
            mock_exec.side_effect = RuntimeError("persistent error")
            result = run_batch(defn, attempt=4)
        assert result.should_retry is False

    def test_retry_delay_exponential_backoff(self):
        """Retry delay for attempt n = min(initial * multiplier^(n-1), max_delay)."""
        from src.jobs.batch_schedule import compute_retry_delay_seconds

        # initial=600, multiplier=2, max=3600
        assert compute_retry_delay_seconds(attempt=1) == 600   # 600 * 2^0 = 600
        assert compute_retry_delay_seconds(attempt=2) == 1200  # 600 * 2^1 = 1200
        assert compute_retry_delay_seconds(attempt=3) == 2400  # 600 * 2^2 = 2400
        assert compute_retry_delay_seconds(attempt=4) == 3600  # 600 * 2^3 = 4800, capped at 3600

    def test_retry_delay_does_not_exceed_max(self):
        from src.jobs.batch_schedule import compute_retry_delay_seconds

        for attempt in range(1, 10):
            assert compute_retry_delay_seconds(attempt=attempt) <= 3600


# ---------------------------------------------------------------------------
# Slack notification tests
# ---------------------------------------------------------------------------


class TestSlackNotification:
    def _make_result(
        self,
        status: BatchOutcome,
        batch_id: str = "kr_daily",
        attempt: int = 1,
        should_retry: bool = False,
        error: str | None = None,
    ) -> BatchRunResult:
        return BatchRunResult(
            batch_id=batch_id,
            market="KR",
            interval="1d",
            provider="pykrx",
            attempt=attempt,
            status=status,
            symbols=2,
            success_symbols=2 if status == BatchOutcome.SUCCESS else 1,
            failed_symbols=0 if status == BatchOutcome.SUCCESS else 1,
            total_rows_inserted=10,
            elapsed_ms=500,
            error=error,
            should_retry=should_retry,
        )

    def test_notify_slack_does_nothing_when_disabled(self):
        result = self._make_result(BatchOutcome.FAILED)
        with (
            patch.dict(os.environ, {"SLACK_NOTIFICATIONS_ENABLED": "false"}, clear=False),
            patch("src.jobs.batch_schedule.urllib.request.urlopen") as mock_urlopen,
        ):
            notify_slack(result)
        mock_urlopen.assert_not_called()

    def test_notify_slack_does_nothing_when_no_webhook_url(self):
        result = self._make_result(BatchOutcome.FAILED)
        env = {"SLACK_NOTIFICATIONS_ENABLED": "true"}
        env.pop("SLACK_WEBHOOK_URL", None)
        with (
            patch.dict(os.environ, env, clear=False),
        ):
            # Remove SLACK_WEBHOOK_URL if present
            original = os.environ.pop("SLACK_WEBHOOK_URL", None)
            try:
                with patch("src.jobs.batch_schedule.urllib.request.urlopen") as mock_urlopen:
                    notify_slack(result)
                mock_urlopen.assert_not_called()
            finally:
                if original is not None:
                    os.environ["SLACK_WEBHOOK_URL"] = original

    def test_notify_slack_sends_for_failed(self):
        result = self._make_result(BatchOutcome.FAILED)
        with (
            patch.dict(os.environ, {
                "SLACK_NOTIFICATIONS_ENABLED": "true",
                "SLACK_WEBHOOK_URL": "https://hooks.slack.com/test",
            }, clear=False),
            patch("src.jobs.batch_schedule.urllib.request.urlopen") as mock_urlopen,
        ):
            mock_urlopen.return_value.__enter__ = lambda s: s
            mock_urlopen.return_value.__exit__ = MagicMock(return_value=False)
            notify_slack(result)
        mock_urlopen.assert_called_once()

    def test_notify_slack_sends_for_partial_success(self):
        result = self._make_result(BatchOutcome.PARTIAL_SUCCESS)
        with (
            patch.dict(os.environ, {
                "SLACK_NOTIFICATIONS_ENABLED": "true",
                "SLACK_WEBHOOK_URL": "https://hooks.slack.com/test",
            }, clear=False),
            patch("src.jobs.batch_schedule.urllib.request.urlopen") as mock_urlopen,
        ):
            mock_urlopen.return_value.__enter__ = lambda s: s
            mock_urlopen.return_value.__exit__ = MagicMock(return_value=False)
            notify_slack(result)
        mock_urlopen.assert_called_once()

    def test_notify_slack_does_not_send_for_success(self):
        result = self._make_result(BatchOutcome.SUCCESS)
        with (
            patch.dict(os.environ, {
                "SLACK_NOTIFICATIONS_ENABLED": "true",
                "SLACK_WEBHOOK_URL": "https://hooks.slack.com/test",
            }, clear=False),
            patch("src.jobs.batch_schedule.urllib.request.urlopen") as mock_urlopen,
        ):
            notify_slack(result)
        mock_urlopen.assert_not_called()

    def test_notify_slack_does_not_send_for_no_new_bar(self):
        result = self._make_result(BatchOutcome.NO_NEW_BAR)
        with (
            patch.dict(os.environ, {
                "SLACK_NOTIFICATIONS_ENABLED": "true",
                "SLACK_WEBHOOK_URL": "https://hooks.slack.com/test",
            }, clear=False),
            patch("src.jobs.batch_schedule.urllib.request.urlopen") as mock_urlopen,
        ):
            notify_slack(result)
        mock_urlopen.assert_not_called()

    def test_notify_slack_sends_for_noop_empty_universe(self):
        """noop_empty_universe should trigger a Slack warning when enabled."""
        result = self._make_result(BatchOutcome.NOOP_EMPTY_UNIVERSE)
        with (
            patch.dict(os.environ, {
                "SLACK_NOTIFICATIONS_ENABLED": "true",
                "SLACK_WEBHOOK_URL": "https://hooks.slack.com/test",
            }, clear=False),
            patch("src.jobs.batch_schedule.urllib.request.urlopen") as mock_urlopen,
        ):
            mock_urlopen.return_value.__enter__ = lambda s: s
            mock_urlopen.return_value.__exit__ = MagicMock(return_value=False)
            notify_slack(result)
        mock_urlopen.assert_called_once()

    def test_slack_message_does_not_contain_webhook_url_secret(self):
        """Slack message payload must not echo back the webhook URL."""
        captured_payloads = []

        def capture_urlopen(req, *args, **kwargs):
            import urllib.request
            if isinstance(req, urllib.request.Request):
                captured_payloads.append(req.data)
            mock_resp = MagicMock()
            mock_resp.__enter__ = lambda s: s
            mock_resp.__exit__ = MagicMock(return_value=False)
            return mock_resp

        result = self._make_result(BatchOutcome.FAILED, error="some error")
        webhook_url = "https://hooks.slack.com/services/SECRET_TOKEN"
        with (
            patch.dict(os.environ, {
                "SLACK_NOTIFICATIONS_ENABLED": "true",
                "SLACK_WEBHOOK_URL": webhook_url,
            }, clear=False),
            patch("src.jobs.batch_schedule.urllib.request.urlopen", side_effect=capture_urlopen),
        ):
            notify_slack(result)

        assert len(captured_payloads) == 1
        payload_str = captured_payloads[0].decode("utf-8")
        assert "SECRET_TOKEN" not in payload_str


# ---------------------------------------------------------------------------
# app.py APScheduler lifecycle tests
# ---------------------------------------------------------------------------


class TestAppSchedulerLifecycle:
    """Verify the FastAPI app startup wires APScheduler, not the old asyncio loop."""

    def test_app_has_no_run_periodic_collect_loop(self):
        """The old _run_periodic_collect_loop should be removed from app.py."""
        import src.interfaces.api.app as app_module
        assert not hasattr(app_module, "_run_periodic_collect_loop"), (
            "_run_periodic_collect_loop should be removed in favor of APScheduler"
        )

    def test_app_has_no_is_periodic_enabled(self):
        import src.interfaces.api.app as app_module
        assert not hasattr(app_module, "_is_periodic_enabled"), (
            "_is_periodic_enabled should be removed"
        )

    def test_collect_daily_endpoint_still_accessible(self):
        from fastapi.testclient import TestClient
        from src.interfaces.api.app import app
        with patch("src.interfaces.api.app.execute") as mock_exec:
            mock_exec.return_value = {
                "provider": "pykrx",
                "symbols": 0,
                "success_symbols": 0,
                "failed_symbols": 0,
                "total_rows_inserted": 0,
                "start": "2024-01-01",
                "end": "2024-01-31",
            }
            client = TestClient(app)
            resp = client.post("/collect/daily", json={"provider": "pykrx", "start": "2024-01-01", "end": "2024-01-31"})
        assert resp.status_code == 200

    def test_collect_weekly_endpoint_still_accessible(self):
        from fastapi.testclient import TestClient
        from src.interfaces.api.app import app
        with patch("src.interfaces.api.app.execute_weekly") as mock_exec:
            mock_exec.return_value = {
                "provider": "yfinance",
                "symbols": 0,
                "success_symbols": 0,
                "failed_symbols": 0,
                "total_rows_inserted": 0,
                "start": "2024-01-01",
                "end": "2024-01-31",
            }
            client = TestClient(app)
            resp = client.post("/collect/weekly", json={"provider": "yfinance", "start": "2024-01-01", "end": "2024-01-31"})
        assert resp.status_code == 200