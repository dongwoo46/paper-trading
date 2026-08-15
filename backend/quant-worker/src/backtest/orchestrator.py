from __future__ import annotations

from concurrent.futures import Executor
from datetime import date
from pathlib import Path
from typing import Protocol
from uuid import UUID

from src.backtest.domain import BacktestRun, Market, RunStatus
from src.backtest.result_parser import ParsedLeanResult
from src.backtest.runner import LeanProcessResult
from src.backtest.service import BacktestRunService
from src.backtest.workspace import LeanRunWorkspace


class BacktestDataExporterPort(Protocol):
    def export_daily(
        self,
        *,
        market: Market,
        symbol: str,
        start_date: date,
        end_date: date,
        data_root: Path,
    ) -> object: ...


class LeanWorkspaceBuilderPort(Protocol):
    def root_for(self, run_id: UUID) -> Path: ...

    def data_root_for(self, run_id: UUID) -> Path: ...

    def reserve(self, run: BacktestRun) -> Path: ...

    def build_after_export(self, run: BacktestRun) -> LeanRunWorkspace: ...


class LeanRunnerPort(Protocol):
    def run(
        self,
        workspace: LeanRunWorkspace,
        *,
        timeout_seconds: int,
    ) -> LeanProcessResult: ...


class LeanResultParserPort(Protocol):
    def parse(self, results_dir: Path) -> ParsedLeanResult: ...


class BacktestExecutionDispatcher(Protocol):
    def dispatch(self, run_id: UUID) -> None: ...


class BacktestExecutionOrchestrator:
    def __init__(
        self,
        *,
        service: BacktestRunService,
        exporter: BacktestDataExporterPort,
        workspace_builder: LeanWorkspaceBuilderPort,
        runner: LeanRunnerPort,
        parser: LeanResultParserPort,
        timeout_seconds: int,
    ) -> None:
        if timeout_seconds <= 0:
            raise ValueError("timeout_seconds must be greater than zero")
        self._service = service
        self._exporter = exporter
        self._workspace_builder = workspace_builder
        self._runner = runner
        self._parser = parser
        self._timeout_seconds = timeout_seconds

    def execute(self, run_id: UUID) -> BacktestRun:
        artifact_path = str(self._workspace_builder.root_for(run_id))
        running = self._service.claim_pending(
            run_id,
            artifact_path=artifact_path,
        )
        if running is None:
            return self._service.get_run(run_id)
        try:
            self._workspace_builder.reserve(running)
            self._exporter.export_daily(
                market=running.market,
                symbol=running.symbol,
                start_date=running.start_date,
                end_date=running.end_date,
                data_root=self._workspace_builder.data_root_for(run_id),
            )
            workspace = self._workspace_builder.build_after_export(running)
            process_result = self._runner.run(
                workspace,
                timeout_seconds=self._timeout_seconds,
            )
            if not process_result.succeeded:
                raise BacktestExecutionError(
                    process_result.error_message or "LEAN execution failed"
                )
            parsed = self._parser.parse(workspace.results_dir)
            return self._service.store_summary(run_id, parsed.summary)
        except Exception as exc:  # noqa: BLE001 - dependency boundary must fail the run
            return self._service.update_status(
                run_id,
                RunStatus.FAILED,
                error_message=str(exc) or exc.__class__.__name__,
                artifact_path=artifact_path,
            )


class BacktestExecutionError(RuntimeError):
    pass


class InProcessBacktestExecutionDispatcher:
    def __init__(
        self,
        orchestrator: BacktestExecutionOrchestrator,
        executor: Executor,
    ) -> None:
        self._orchestrator = orchestrator
        self._executor = executor

    def dispatch(self, run_id: UUID) -> None:
        self._executor.submit(self._orchestrator.execute, run_id)
