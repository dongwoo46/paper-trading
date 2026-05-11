"""chart_analysis_schedule APScheduler 통합 단위 테스트 (TDD)."""
from __future__ import annotations

from unittest.mock import MagicMock, patch, call

import pytest


# ---------------------------------------------------------------------------
# BackgroundScheduler Mock
# ---------------------------------------------------------------------------

class MockScheduler:
    """APScheduler BackgroundScheduler stub."""

    def __init__(self) -> None:
        self.jobs: list[dict] = []
        self.listeners: list[callable] = []
        self.started = False
        self.shutdown_called = False

    def add_job(self, func, trigger=None, id=None, name=None, **kwargs) -> MagicMock:
        self.jobs.append({"func": func, "id": id, "name": name, "trigger": trigger, "kwargs": kwargs})
        job = MagicMock()
        
        job.id = id
        return job

    def add_listener(self, listener, mask=None) -> None:
        self.listeners.append(listener)

    def start(self) -> None:
        self.started = True

    def shutdown(self, wait: bool = True) -> None:
        self.shutdown_called = True


# ---------------------------------------------------------------------------
# 테스트
# ---------------------------------------------------------------------------

def _patch_scheduler(mock_sched: MockScheduler):
    """BackgroundScheduler 생성자를 mock으로 교체.

    chart_analysis_schedule.py 는 함수 내부에서 import 하므로
    apscheduler 모듈 자체를 패치한다.
    """
    mock_module = MagicMock()
    mock_module.BackgroundScheduler.return_value = mock_sched
    mock_module.CronTrigger = MagicMock(return_value=MagicMock())
    mock_module.EVENT_JOB_EXECUTED = 1  # 임의 상수

    return patch.dict(
        "sys.modules",
        {
            "apscheduler": mock_module,
            "apscheduler.schedulers": mock_module,
            "apscheduler.schedulers.background": mock_module,
            "apscheduler.triggers": mock_module,
            "apscheduler.triggers.cron": mock_module,
            "apscheduler.events": mock_module,
        },
    )


def test_start_returns_scheduler_instance():
    """start_chart_analysis_scheduler()는 스케줄러 인스턴스를 반환한다."""
    from src.jobs.chart_analysis_schedule import start_chart_analysis_scheduler

    mock_sched = MockScheduler()
    with _patch_scheduler(mock_sched):
        result = start_chart_analysis_scheduler()

    assert result is mock_sched
    assert mock_sched.started is True


def test_start_registers_four_jobs():
    """start_chart_analysis_scheduler() 호출 시 4개 잡이 등록된다."""
    from src.jobs.chart_analysis_schedule import start_chart_analysis_scheduler

    mock_sched = MockScheduler()
    with _patch_scheduler(mock_sched):
        start_chart_analysis_scheduler()

    assert len(mock_sched.jobs) == 4, (
        f"4개 잡 등록 필요, 실제: {len(mock_sched.jobs)} — ids: {[j['id'] for j in mock_sched.jobs]}"
    )


def test_start_registers_krx_chart_analysis_job():
    """KRX 차트 분석 잡이 등록된다 (id 포함)."""
    from src.jobs.chart_analysis_schedule import start_chart_analysis_scheduler

    mock_sched = MockScheduler()
    with _patch_scheduler(mock_sched):
        start_chart_analysis_scheduler()

    ids = [j["id"] for j in mock_sched.jobs]
    assert any("krx" in (jid or "").lower() for jid in ids), (
        f"KRX 차트 분석 잡이 없음, ids={ids}"
    )


def test_start_registers_us_chart_analysis_job():
    """US 차트 분석 잡이 등록된다."""
    from src.jobs.chart_analysis_schedule import start_chart_analysis_scheduler

    mock_sched = MockScheduler()
    with _patch_scheduler(mock_sched):
        start_chart_analysis_scheduler()

    ids = [j["id"] for j in mock_sched.jobs]
    assert any("us" in (jid or "").lower() for jid in ids), (
        f"US 차트 분석 잡이 없음, ids={ids}"
    )


def test_start_registers_weekly_chart_analysis_job():
    """주봉 차트 분석 잡이 등록된다."""
    from src.jobs.chart_analysis_schedule import start_chart_analysis_scheduler

    mock_sched = MockScheduler()
    with _patch_scheduler(mock_sched):
        start_chart_analysis_scheduler()

    ids = [j["id"] for j in mock_sched.jobs]
    assert any("weekly" in (jid or "").lower() for jid in ids), (
        f"주봉 차트 분석 잡이 없음, ids={ids}"
    )


def test_start_registers_popular_symbols_refresh_job():
    """인기 종목 갱신 잡이 등록된다 (CronTrigger: 월요일 04:00 KST)."""
    from src.jobs.chart_analysis_schedule import start_chart_analysis_scheduler

    mock_sched = MockScheduler()
    with _patch_scheduler(mock_sched):
        start_chart_analysis_scheduler()

    ids = [j["id"] for j in mock_sched.jobs]
    assert any("popular" in (jid or "").lower() for jid in ids), (
        f"인기 종목 갱신 잡이 없음, ids={ids}"
    )


def test_stop_shuts_down_scheduler():
    """stop_chart_analysis_scheduler()는 스케줄러를 정상 종료한다."""
    from src.jobs.chart_analysis_schedule import stop_chart_analysis_scheduler

    mock_sched = MockScheduler()
    stop_chart_analysis_scheduler(mock_sched)

    assert mock_sched.shutdown_called is True


def test_stop_with_none_does_not_raise():
    """stop_chart_analysis_scheduler(None)은 예외 없이 반환한다."""
    from src.jobs.chart_analysis_schedule import stop_chart_analysis_scheduler

    stop_chart_analysis_scheduler(None)  # 예외 없어야 함


def test_listener_registered():
    """EVENT_JOB_EXECUTED 리스너가 등록된다."""
    from src.jobs.chart_analysis_schedule import start_chart_analysis_scheduler

    mock_sched = MockScheduler()
    with _patch_scheduler(mock_sched):
        start_chart_analysis_scheduler()

    assert len(mock_sched.listeners) >= 1, "EVENT_JOB_EXECUTED 리스너 미등록"