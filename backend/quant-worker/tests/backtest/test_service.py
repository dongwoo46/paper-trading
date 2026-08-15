from __future__ import annotations

from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from decimal import Decimal
from threading import Barrier
from uuid import UUID

import pytest

from src.backtest.domain import BacktestRunCreateRequest, BacktestSummary, RunStatus
from src.backtest.repository import InMemoryBacktestRunRepository
from src.backtest.service import BacktestRunService, InvalidRunTransitionError

from .test_domain import run_request_payload

RUN_ID = UUID("00000000-0000-0000-0000-000000000001")
NOW = datetime(2024, 1, 1, 9, 0)  # noqa: DTZ001 - project contract is KST-naive


def service() -> BacktestRunService:
    return BacktestRunService(
        InMemoryBacktestRunRepository(),
        id_factory=lambda: RUN_ID,
        clock=lambda: NOW,
    )


def summary() -> BacktestSummary:
    return BacktestSummary(
        total_return=Decimal("0.10"),
        max_drawdown=Decimal("0.04"),
        annualized_return=Decimal("0.08"),
        sharpe=Decimal("1.25"),
        calmar=Decimal("2.00"),
        win_rate=Decimal("0.60"),
        total_trades=12,
    )


def test_create_persists_pending_run_and_load_returns_it() -> None:
    backtest_service = service()
    request = BacktestRunCreateRequest.model_validate(run_request_payload())

    created = backtest_service.create_run(request)

    assert created.run_id == RUN_ID
    assert created.status is RunStatus.PENDING
    assert created.initial_cash == Decimal(100000000)
    assert created.cost_profile.value == "KR_DEFAULT_V1"
    assert backtest_service.get_run(RUN_ID) == created


def test_default_clock_creates_naive_timestamp() -> None:
    backtest_service = BacktestRunService(InMemoryBacktestRunRepository())

    created = backtest_service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload())
    )

    assert created.created_at.tzinfo is None


def test_status_updates_are_explicit_and_terminal_runs_cannot_restart() -> None:
    backtest_service = service()
    run = backtest_service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload())
    )

    running = backtest_service.update_status(run.run_id, RunStatus.RUNNING)
    completed = backtest_service.store_summary(run.run_id, summary())

    assert running.started_at == NOW
    assert completed.status is RunStatus.COMPLETED
    assert completed.finished_at == NOW
    assert completed.summary == summary()
    with pytest.raises(InvalidRunTransitionError):
        backtest_service.update_status(run.run_id, RunStatus.RUNNING)


def test_pending_run_can_be_claimed_by_only_one_concurrent_worker() -> None:
    repository = InMemoryBacktestRunRepository()
    backtest_service = BacktestRunService(
        repository,
        id_factory=lambda: RUN_ID,
        clock=lambda: NOW,
    )
    backtest_service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload())
    )
    barrier = Barrier(3)

    def claim():
        barrier.wait()
        return backtest_service.claim_pending(
            RUN_ID,
            artifact_path=f"/runs/{RUN_ID}",
        )

    with ThreadPoolExecutor(max_workers=2) as executor:
        futures = [executor.submit(claim) for _ in range(2)]
        barrier.wait()
        claims = [future.result() for future in futures]

    assert sum(claimed is not None for claimed in claims) == 1
    assert backtest_service.get_run(RUN_ID).status is RunStatus.RUNNING
