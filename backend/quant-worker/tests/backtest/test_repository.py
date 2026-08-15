from __future__ import annotations

from datetime import datetime
from uuid import UUID

from src.backtest.domain import BacktestRunCreateRequest, BacktestSummary, RunStatus
from src.backtest.repository import PostgresBacktestRunRepository
from src.backtest.service import BacktestRunService

from .test_domain import run_request_payload


def test_postgres_row_round_trip_preserves_cost_profile_id() -> None:
    run = BacktestRunService(
        _CaptureRepository(),
        id_factory=lambda: UUID("00000000-0000-0000-0000-000000000011"),
        clock=lambda: datetime(2024, 1, 1, 9, 0),  # noqa: DTZ001 - KST-naive contract
    ).create_run(BacktestRunCreateRequest.model_validate(run_request_payload()))

    params = PostgresBacktestRunRepository._params(run)
    reloaded = PostgresBacktestRunRepository._to_run(tuple(params))

    assert reloaded == run
    assert reloaded.cost_profile.value == "KR_DEFAULT_V1"


def test_postgres_create_and_update_bind_every_column_including_profile() -> None:
    capture = _SqlCaptureRepository()
    run = BacktestRunService(
        _CaptureRepository(),
        id_factory=lambda: UUID("00000000-0000-0000-0000-000000000012"),
        clock=lambda: datetime(2024, 1, 1, 9, 0),  # noqa: DTZ001 - KST-naive contract
    ).create_run(BacktestRunCreateRequest.model_validate(run_request_payload()))

    capture.create(run)
    capture.update(run)

    assert len(capture.executions) == 2
    for query, params in capture.executions:
        assert query.count("%s") == len(params)
        assert "cost_profile" in query
        assert "KR_DEFAULT_V1" in params


def test_postgres_summary_offsets_survive_row_round_trip() -> None:
    run = BacktestRunService(
        _CaptureRepository(),
        id_factory=lambda: UUID("00000000-0000-0000-0000-000000000013"),
        clock=lambda: datetime(2024, 1, 1, 9, 0),  # noqa: DTZ001 - KST-naive contract
    ).create_run(BacktestRunCreateRequest.model_validate(run_request_payload()))
    summary = BacktestSummary(
        total_return="0.1",
        max_drawdown="0.2",
        annualized_return="0.3",
        sharpe="1.1",
        calmar="1.2",
        win_rate="0.6",
        total_trades=4,
    )
    run = run.model_copy(update={"summary": summary})

    reloaded = PostgresBacktestRunRepository._to_run(
        tuple(PostgresBacktestRunRepository._params(run))
    )

    assert reloaded.summary == summary


def test_postgres_pending_claim_is_single_atomic_compare_and_set() -> None:
    run = BacktestRunService(
        _CaptureRepository(),
        id_factory=lambda: UUID("00000000-0000-0000-0000-000000000014"),
        clock=lambda: datetime(2024, 1, 1, 9, 0),  # noqa: DTZ001 - KST-naive
    ).create_run(BacktestRunCreateRequest.model_validate(run_request_payload()))
    artifact_path = f"/runs/{run.run_id}"
    running = run.model_copy(
        update={
            "status": RunStatus.RUNNING,
            "artifact_path": artifact_path,
            "started_at": datetime(2024, 1, 1, 9, 1),  # noqa: DTZ001
        }
    )
    capture = _ClaimCaptureRepository(
        tuple(PostgresBacktestRunRepository._params(running))
    )

    claimed = capture.claim_pending(
        run.run_id,
        started_at=running.started_at,
        artifact_path=artifact_path,
    )

    assert claimed == running
    query, params = capture.execution
    assert "WHERE run_id = %s AND status = %s" in query
    assert "RETURNING" in query
    assert params == [
        RunStatus.RUNNING.value,
        running.started_at,
        artifact_path,
        run.run_id,
        RunStatus.PENDING.value,
    ]


class _CaptureRepository:
    def create(self, run):
        self.run = run
        return run

    def update(self, run):
        self.run = run
        return run

    def get(self, run_id):
        return getattr(self, "run", None)


class _SqlCaptureRepository(PostgresBacktestRunRepository):
    def __init__(self) -> None:
        self.executions: list[tuple[str, list[object]]] = []

    def _execute(self, query: str, params: list[object]) -> int:
        self.executions.append((query, params))
        return 1


class _ClaimCaptureRepository(PostgresBacktestRunRepository):
    def __init__(self, row: tuple[object, ...] | None) -> None:
        self.row = row
        self.execution: tuple[str, list[object]]

    def _fetch_one(self, query: str, params: list[object]):
        self.execution = (query, params)
        return self.row
