from __future__ import annotations

from unittest.mock import MagicMock, patch

import pytest

from src.jobs.investor_flow_schedule import (
    build_investor_flow_batch_definitions,
    start_investor_flow_scheduler,
    stop_investor_flow_scheduler,
)


# ---------------------------------------------------------------------------
# build_investor_flow_batch_definitions
# ---------------------------------------------------------------------------

class TestBuildBatchDefinitions:
    def test_returns_two_definitions(self):
        definitions = build_investor_flow_batch_definitions()
        assert len(definitions) == 2

    def test_kospi_batch_exists(self):
        definitions = build_investor_flow_batch_definitions()
        ids = [d.batch_id for d in definitions]
        assert "investor_flow_kospi" in ids

    def test_kosdaq_batch_exists(self):
        definitions = build_investor_flow_batch_definitions()
        ids = [d.batch_id for d in definitions]
        assert "investor_flow_kosdaq" in ids

    def test_default_hour_is_19(self):
        definitions = build_investor_flow_batch_definitions()
        for d in definitions:
            assert d.cron_hour == 19, f"{d.batch_id} should run at 19:00"

    def test_default_day_of_week_is_mon_fri(self):
        definitions = build_investor_flow_batch_definitions()
        for d in definitions:
            assert d.cron_day_of_week == "mon-fri", f"{d.batch_id} should run Mon-Fri"

    def test_cron_override_via_env(self, monkeypatch):
        monkeypatch.setenv("INVESTOR_FLOW_CRON_KOSPI", "0 20 * * 1-5")
        definitions = build_investor_flow_batch_definitions()
        kospi = next(d for d in definitions if d.batch_id == "investor_flow_kospi")
        assert kospi.cron_hour == 20

    def test_market_is_correctly_set(self):
        definitions = build_investor_flow_batch_definitions()
        markets = {d.batch_id: d.market for d in definitions}
        assert markets["investor_flow_kospi"] == "KOSPI"
        assert markets["investor_flow_kosdaq"] == "KOSDAQ"


# ---------------------------------------------------------------------------
# start_investor_flow_scheduler — disabled (default)
# ---------------------------------------------------------------------------

class TestStartSchedulerDisabled:
    def test_returns_none_when_disabled(self):
        with patch.dict("os.environ", {"INVESTOR_FLOW_SCHEDULE_ENABLED": "false"}, clear=False):
            result = start_investor_flow_scheduler()
        assert result is None

    def test_returns_none_when_env_not_set(self):
        import os
        env = {k: v for k, v in os.environ.items() if k != "INVESTOR_FLOW_SCHEDULE_ENABLED"}
        with patch.dict("os.environ", env, clear=True):
            result = start_investor_flow_scheduler()
        assert result is None


# ---------------------------------------------------------------------------
# start_investor_flow_scheduler — enabled
# ---------------------------------------------------------------------------

class TestStartSchedulerEnabled:
    def test_returns_scheduler_when_enabled(self):
        mock_scheduler = MagicMock()
        mock_bg_cls = MagicMock(return_value=mock_scheduler)
        mock_cron_trigger = MagicMock()
        mock_cron_cls = MagicMock(return_value=mock_cron_trigger)

        with patch.dict("os.environ", {"INVESTOR_FLOW_SCHEDULE_ENABLED": "true"}, clear=False):
            with patch("src.jobs.investor_flow_schedule.BackgroundScheduler", mock_bg_cls):
                with patch("src.jobs.investor_flow_schedule.CronTrigger", mock_cron_cls):
                    result = start_investor_flow_scheduler()

        assert result is mock_scheduler
        mock_scheduler.start.assert_called_once()

    def test_registers_two_jobs_when_enabled(self):
        mock_scheduler = MagicMock()
        mock_bg_cls = MagicMock(return_value=mock_scheduler)
        mock_cron_trigger = MagicMock()
        mock_cron_cls = MagicMock(return_value=mock_cron_trigger)

        with patch.dict("os.environ", {"INVESTOR_FLOW_SCHEDULE_ENABLED": "true"}, clear=False):
            with patch("src.jobs.investor_flow_schedule.BackgroundScheduler", mock_bg_cls):
                with patch("src.jobs.investor_flow_schedule.CronTrigger", mock_cron_cls):
                    start_investor_flow_scheduler()

        assert mock_scheduler.add_job.call_count == 2


# ---------------------------------------------------------------------------
# stop_investor_flow_scheduler
# ---------------------------------------------------------------------------

class TestStopScheduler:
    def test_stop_with_none_does_not_raise(self):
        stop_investor_flow_scheduler(None)  # Should not raise

    def test_stop_calls_shutdown(self):
        mock_scheduler = MagicMock()
        stop_investor_flow_scheduler(mock_scheduler)
        mock_scheduler.shutdown.assert_called_once_with(wait=False)

    def test_stop_handles_shutdown_exception(self):
        mock_scheduler = MagicMock()
        mock_scheduler.shutdown.side_effect = Exception("scheduler already stopped")
        stop_investor_flow_scheduler(mock_scheduler)  # Should not raise
