from __future__ import annotations

from collections.abc import Callable
from concurrent.futures import ThreadPoolExecutor
from datetime import datetime
from decimal import Decimal
from pathlib import Path
from threading import Barrier, local
from uuid import UUID

from src.backtest.domain import BacktestRunCreateRequest, BacktestSummary, RunStatus
from src.backtest.orchestrator import (
    BacktestExecutionOrchestrator,
    InProcessBacktestExecutionDispatcher,
)
from src.backtest.repository import InMemoryBacktestRunRepository
from src.backtest.result_parser import ParsedLeanResult
from src.backtest.runner import LeanProcessResult
from src.backtest.service import BacktestRunService
from src.backtest.workspace import LeanWorkspaceBuilder

from .test_domain import run_request_payload

RUN_ID = UUID("00000000-0000-0000-0000-000000000042")
NOW = datetime(2024, 1, 1, 9, 0)  # noqa: DTZ001 - project contract is KST-naive


def _summary() -> BacktestSummary:
    return BacktestSummary(
        total_return=Decimal("0.10"),
        max_drawdown=Decimal("0.04"),
        annualized_return=Decimal("0.08"),
        sharpe=Decimal("1.25"),
        calmar=Decimal(2),
        win_rate=Decimal("0.60"),
        total_trades=12,
    )


class FakeExporter:
    def __init__(self, events: list[str]) -> None:
        self.events = events
        self.calls: list[dict[str, object]] = []

    def export_daily(self, **kwargs: object) -> object:
        self.events.append("export")
        self.calls.append(kwargs)
        data_root = kwargs["data_root"]
        assert isinstance(data_root, Path)
        data_root.mkdir(parents=True, exist_ok=True)
        (data_root / "exported.marker").write_text("data", encoding="utf-8")
        return object()


class RecordingWorkspaceBuilder:
    def __init__(self, artifact_root: Path, events: list[str]) -> None:
        self._builder = LeanWorkspaceBuilder(artifact_root)
        self.events = events

    def root_for(self, run_id: UUID) -> Path:
        return self._builder.root_for(run_id)

    def data_root_for(self, run_id: UUID) -> Path:
        return self._builder.data_root_for(run_id)

    def reserve(self, run: object) -> Path:
        return self._builder.reserve(run)  # type: ignore[arg-type]

    def build_after_export(self, run: object) -> object:
        self.events.append("workspace")
        return self._builder.build_after_export(run)  # type: ignore[arg-type]


class FakeRunner:
    def __init__(self, events: list[str], *, succeeds: bool = True) -> None:
        self.events = events
        self.succeeds = succeeds
        self.calls = 0

    def run(self, workspace: object, *, timeout_seconds: int) -> LeanProcessResult:
        self.events.append("run")
        self.calls += 1
        assert timeout_seconds == 60
        logs_dir = workspace.logs_dir  # type: ignore[attr-defined]
        logs_dir.mkdir(exist_ok=True)
        stdout_path = logs_dir / "docker.stdout.log"
        stderr_path = logs_dir / "docker.stderr.log"
        stdout_path.write_text("stdout kept", encoding="utf-8")
        stderr_path.write_text("stderr kept", encoding="utf-8")
        return LeanProcessResult(
            command=("docker", "run"),
            succeeded=self.succeeds,
            timed_out=False,
            return_code=0 if self.succeeds else 17,
            stdout_path=stdout_path,
            stderr_path=stderr_path,
            error_message=None if self.succeeds else "LEAN exited with code 17",
        )


class FakeParser:
    def __init__(self, events: list[str]) -> None:
        self.events = events
        self.calls = 0

    def parse(self, results_dir: Path) -> ParsedLeanResult:
        self.events.append("parse")
        self.calls += 1
        return ParsedLeanResult(
            summary=_summary(),
            details={"Statistics": {}},
            summary_path=results_dir / "algorithm-summary.json",
            full_result_path=results_dir / "algorithm.json",
        )


def _service() -> BacktestRunService:
    return BacktestRunService(
        InMemoryBacktestRunRepository(),
        id_factory=lambda: RUN_ID,
        clock=lambda: NOW,
    )


def test_orchestrator_runs_ordered_pipeline_and_persists_summary_only_on_success(
    tmp_path: Path,
) -> None:
    service = _service()
    created = service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload())
    )
    events: list[str] = []
    exporter = FakeExporter(events)
    runner = FakeRunner(events)
    parser = FakeParser(events)
    orchestrator = BacktestExecutionOrchestrator(
        service=service,
        exporter=exporter,
        workspace_builder=RecordingWorkspaceBuilder(tmp_path / "runs", events),
        runner=runner,
        parser=parser,
        timeout_seconds=60,
    )

    completed = orchestrator.execute(created.run_id)

    assert events == ["export", "workspace", "run", "parse"]
    assert completed.status is RunStatus.COMPLETED
    assert completed.summary == _summary()
    assert completed.artifact_path == str(tmp_path / "runs" / str(RUN_ID))
    assert exporter.calls[0]["market"] == created.market
    assert exporter.calls[0]["symbol"] == created.symbol
    assert exporter.calls[0]["start_date"] == created.start_date
    assert exporter.calls[0]["end_date"] == created.end_date


def test_orchestrator_failure_keeps_artifact_logs_and_does_not_store_summary(
    tmp_path: Path,
) -> None:
    service = _service()
    created = service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload())
    )
    events: list[str] = []
    parser = FakeParser(events)
    orchestrator = BacktestExecutionOrchestrator(
        service=service,
        exporter=FakeExporter(events),
        workspace_builder=RecordingWorkspaceBuilder(tmp_path / "runs", events),
        runner=FakeRunner(events, succeeds=False),
        parser=parser,
        timeout_seconds=60,
    )

    failed = orchestrator.execute(created.run_id)

    assert failed.status is RunStatus.FAILED
    assert failed.error_message == "LEAN exited with code 17"
    assert failed.summary is None
    assert failed.artifact_path == str(tmp_path / "runs" / str(RUN_ID))
    assert (Path(failed.artifact_path) / "logs" / "docker.stdout.log").read_text(
        encoding="utf-8"
    ) == "stdout kept"
    assert parser.calls == 0


def test_orchestrator_lookup_is_idempotent_for_terminal_run(tmp_path: Path) -> None:
    service = _service()
    created = service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload())
    )
    events: list[str] = []
    runner = FakeRunner(events)
    orchestrator = BacktestExecutionOrchestrator(
        service=service,
        exporter=FakeExporter(events),
        workspace_builder=RecordingWorkspaceBuilder(tmp_path / "runs", events),
        runner=runner,
        parser=FakeParser(events),
        timeout_seconds=60,
    )

    first = orchestrator.execute(created.run_id)
    second = orchestrator.execute(created.run_id)

    assert first == second
    assert runner.calls == 1


def test_orchestrator_converts_export_exception_to_failed_run(tmp_path: Path) -> None:
    class FailingExporter(FakeExporter):
        def export_daily(self, **kwargs: object) -> object:
            raise RuntimeError("database read failed")

    service = _service()
    created = service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload())
    )
    events: list[str] = []
    orchestrator = BacktestExecutionOrchestrator(
        service=service,
        exporter=FailingExporter(events),
        workspace_builder=RecordingWorkspaceBuilder(tmp_path / "runs", events),
        runner=FakeRunner(events),
        parser=FakeParser(events),
        timeout_seconds=60,
    )

    failed = orchestrator.execute(created.run_id)

    assert failed.status is RunStatus.FAILED
    assert failed.error_message == "database read failed"
    assert failed.summary is None


def test_in_process_dispatcher_submits_without_running_inline() -> None:
    class DeferredExecutor:
        def __init__(self) -> None:
            self.tasks: list[tuple[Callable[..., object], tuple[object, ...]]] = []

        def submit(
            self,
            function: Callable[..., object],
            /,
            *args: object,
            **_kwargs: object,
        ) -> object:
            self.tasks.append((function, args))
            return object()

    class RecordingOrchestrator:
        def __init__(self) -> None:
            self.calls: list[UUID] = []

        def execute(self, run_id: UUID) -> object:
            self.calls.append(run_id)
            return object()

    executor = DeferredExecutor()
    orchestrator = RecordingOrchestrator()
    dispatcher = InProcessBacktestExecutionDispatcher(
        orchestrator,  # type: ignore[arg-type]
        executor,  # type: ignore[arg-type]
    )

    dispatcher.dispatch(RUN_ID)

    assert orchestrator.calls == []
    assert len(executor.tasks) == 1
    function, args = executor.tasks[0]
    function(*args)
    assert orchestrator.calls == [RUN_ID]


def test_concurrent_execution_enters_export_pipeline_only_once(tmp_path: Path) -> None:
    barrier = Barrier(2)

    class BarrierRepository(InMemoryBacktestRunRepository):
        def __init__(self) -> None:
            super().__init__()
            self.thread_state = local()

        def get(self, run_id: UUID):
            run = super().get(run_id)
            if (
                run is not None
                and run.status is RunStatus.PENDING
                and not getattr(self.thread_state, "waited", False)
            ):
                self.thread_state.waited = True
                barrier.wait()
            return run

    repository = BarrierRepository()
    service = BacktestRunService(
        repository,
        id_factory=lambda: RUN_ID,
        clock=lambda: NOW,
    )
    created = service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload())
    )
    events: list[str] = []
    exporter = FakeExporter(events)
    orchestrator = BacktestExecutionOrchestrator(
        service=service,
        exporter=exporter,
        workspace_builder=RecordingWorkspaceBuilder(tmp_path / "runs", events),
        runner=FakeRunner(events),
        parser=FakeParser(events),
        timeout_seconds=60,
    )

    with ThreadPoolExecutor(max_workers=2) as executor:
        outcomes = list(executor.map(orchestrator.execute, [created.run_id] * 2))

    assert len(exporter.calls) == 1
    assert service.get_run(RUN_ID).status is RunStatus.COMPLETED
    assert {outcome.status for outcome in outcomes} <= {
        RunStatus.RUNNING,
        RunStatus.COMPLETED,
    }


def test_precreated_run_symlink_fails_before_export_and_writes_nothing_outside(
    tmp_path: Path,
) -> None:
    artifact_root = tmp_path / "runs"
    artifact_root.mkdir()
    outside = tmp_path / "outside"
    outside.mkdir()
    (artifact_root / str(RUN_ID)).symlink_to(outside, target_is_directory=True)
    service = _service()
    created = service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload())
    )
    events: list[str] = []
    exporter = FakeExporter(events)
    orchestrator = BacktestExecutionOrchestrator(
        service=service,
        exporter=exporter,
        workspace_builder=RecordingWorkspaceBuilder(artifact_root, events),
        runner=FakeRunner(events),
        parser=FakeParser(events),
        timeout_seconds=60,
    )

    failed = orchestrator.execute(created.run_id)

    assert failed.status is RunStatus.FAILED
    assert exporter.calls == []
    assert list(outside.iterdir()) == []
