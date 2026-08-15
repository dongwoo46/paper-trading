from __future__ import annotations

import os
from concurrent.futures import ThreadPoolExecutor
from datetime import date, datetime
from pathlib import Path
from typing import Annotated
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, ConfigDict, Field

from src.backtest.artifacts import (
    ArtifactAccessError,
    BacktestArtifactReader,
)
from src.backtest.data_export import (
    BacktestDataExporter,
    PostgresBacktestOhlcvRepository,
)
from src.backtest.domain import (
    BacktestRun,
    BacktestRunCreateRequest,
    BacktestSummary,
    RunStatus,
    StrategyDefinition,
)
from src.backtest.lean_template.cost_profiles import CostProfileId
from src.backtest.orchestrator import (
    BacktestExecutionDispatcher,
    BacktestExecutionOrchestrator,
    InProcessBacktestExecutionDispatcher,
)
from src.backtest.repository import PostgresBacktestRunRepository
from src.backtest.result_parser import LeanResultParseError, LeanResultParser
from src.backtest.runner import DockerLeanRunner
from src.backtest.service import BacktestRunNotFoundError, BacktestRunService
from src.backtest.workspace import LeanWorkspaceBuilder
from src.catalog.postgres_symbol_catalog import DbConfig

backtest_router = APIRouter(prefix="/backtest-runs", tags=["backtest"])
_BACKTEST_EXECUTOR = ThreadPoolExecutor(
    max_workers=2,
    thread_name_prefix="backtest-execution",
)


class BacktestRunAccepted(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    run_id: UUID = Field(serialization_alias="runId")
    status: RunStatus
    cost_profile: CostProfileId = Field(serialization_alias="costProfile")


class BacktestRunResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    run_id: UUID = Field(serialization_alias="runId")
    status: RunStatus
    market: str
    symbol: str
    resolution: str
    start_date: date = Field(serialization_alias="startDate")
    end_date: date = Field(serialization_alias="endDate")
    initial_cash: str = Field(serialization_alias="initialCash")
    currency: str
    cost_profile: CostProfileId = Field(serialization_alias="costProfile")
    strategy: StrategyDefinition
    artifact_path: str | None = Field(serialization_alias="artifactPath")
    error_message: str | None = Field(serialization_alias="errorMessage")
    summary: BacktestSummary | None
    created_at: datetime = Field(serialization_alias="createdAt")
    started_at: datetime | None = Field(serialization_alias="startedAt")
    finished_at: datetime | None = Field(serialization_alias="finishedAt")

    @classmethod
    def from_domain(cls, run: BacktestRun) -> BacktestRunResponse:
        return cls(
            run_id=run.run_id,
            status=run.status,
            market=run.market.value,
            symbol=run.symbol,
            resolution=run.resolution,
            start_date=run.start_date,
            end_date=run.end_date,
            initial_cash=str(run.initial_cash),
            currency=run.currency.value,
            cost_profile=run.cost_profile,
            strategy=run.strategy,
            artifact_path=run.artifact_path,
            error_message=run.error_message,
            summary=run.summary,
            created_at=run.created_at,
            started_at=run.started_at,
            finished_at=run.finished_at,
        )


class BacktestResultResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    run_id: UUID = Field(serialization_alias="runId")
    status: RunStatus
    summary: BacktestSummary
    artifact_path: str | None = Field(serialization_alias="artifactPath")
    details: dict[str, object]


class BacktestLogsResponse(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    run_id: UUID = Field(serialization_alias="runId")
    status: RunStatus
    artifact_path: str = Field(serialization_alias="artifactPath")
    stdout: str
    stderr: str


def _db_config() -> DbConfig:
    return DbConfig(
        host=os.getenv("PG_HOST", "localhost"),
        port=int(os.getenv("PG_PORT", "5432")),
        database=os.getenv("PG_DATABASE", "paper"),
        user=os.getenv("PG_USER", "paper"),
        password=os.getenv("PG_PASSWORD", "paper"),
    )


def _artifact_root() -> Path:
    return Path(os.getenv("BACKTEST_ARTIFACT_ROOT", "runs"))


def get_backtest_run_service() -> BacktestRunService:
    repository = PostgresBacktestRunRepository(_db_config())
    return BacktestRunService(repository)


def get_backtest_artifact_reader() -> BacktestArtifactReader:
    return BacktestArtifactReader(_artifact_root())


def get_backtest_execution_dispatcher(
    service: Annotated[BacktestRunService, Depends(get_backtest_run_service)],
) -> BacktestExecutionDispatcher:
    orchestrator = BacktestExecutionOrchestrator(
        service=service,
        exporter=BacktestDataExporter(PostgresBacktestOhlcvRepository(_db_config())),
        workspace_builder=LeanWorkspaceBuilder(_artifact_root()),
        runner=DockerLeanRunner(),
        parser=LeanResultParser(),
        timeout_seconds=int(os.getenv("BACKTEST_TIMEOUT_SECONDS", "600")),
    )
    return InProcessBacktestExecutionDispatcher(orchestrator, _BACKTEST_EXECUTOR)


def _load_run(service: BacktestRunService, run_id: UUID) -> BacktestRun:
    try:
        return service.get_run(run_id)
    except BacktestRunNotFoundError as exc:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={
                "code": "BACKTEST_RUN_NOT_FOUND",
                "message": f"backtest run not found: {run_id}",
            },
        ) from exc


@backtest_router.post(
    "",
    response_model=BacktestRunAccepted,
    status_code=status.HTTP_202_ACCEPTED,
)
async def create_backtest_run(
    request: BacktestRunCreateRequest,
    service: Annotated[BacktestRunService, Depends(get_backtest_run_service)],
    dispatcher: Annotated[
        BacktestExecutionDispatcher,
        Depends(get_backtest_execution_dispatcher),
    ],
) -> BacktestRunAccepted:
    run = service.create_run(request)
    dispatcher.dispatch(run.run_id)
    return BacktestRunAccepted(
        run_id=run.run_id,
        status=run.status,
        cost_profile=run.cost_profile,
    )


@backtest_router.get("/{run_id}", response_model=BacktestRunResponse)
def get_backtest_run(
    run_id: UUID,
    service: Annotated[BacktestRunService, Depends(get_backtest_run_service)],
) -> BacktestRunResponse:
    return BacktestRunResponse.from_domain(_load_run(service, run_id))


@backtest_router.get("/{run_id}/result", response_model=BacktestResultResponse)
def get_backtest_result(
    run_id: UUID,
    service: Annotated[BacktestRunService, Depends(get_backtest_run_service)],
    artifact_reader: Annotated[
        BacktestArtifactReader,
        Depends(get_backtest_artifact_reader),
    ],
) -> BacktestResultResponse:
    run = _load_run(service, run_id)
    if run.status is RunStatus.FAILED:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={
                "code": "BACKTEST_RUN_FAILED",
                "message": run.error_message or "backtest run failed",
            },
        )
    if run.status is not RunStatus.COMPLETED or run.summary is None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={
                "code": "BACKTEST_RESULT_NOT_READY",
                "message": f"backtest result is not ready while status is {run.status.value}",
            },
        )
    try:
        details = artifact_reader.read_details(run)
    except (ArtifactAccessError, LeanResultParseError) as exc:
        raise _artifact_unavailable() from exc
    return BacktestResultResponse(
        run_id=run.run_id,
        status=run.status,
        summary=run.summary,
        artifact_path=run.artifact_path,
        details=details,
    )


@backtest_router.get("/{run_id}/logs", response_model=BacktestLogsResponse)
def get_backtest_logs(
    run_id: UUID,
    service: Annotated[BacktestRunService, Depends(get_backtest_run_service)],
    artifact_reader: Annotated[
        BacktestArtifactReader,
        Depends(get_backtest_artifact_reader),
    ],
) -> BacktestLogsResponse:
    run = _load_run(service, run_id)
    if run.artifact_path is None:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={
                "code": "BACKTEST_LOGS_NOT_READY",
                "message": f"backtest logs are not ready while status is {run.status.value}",
            },
        )
    try:
        logs = artifact_reader.read_logs(run)
    except ArtifactAccessError as exc:
        raise _artifact_unavailable() from exc
    return BacktestLogsResponse(
        run_id=run.run_id,
        status=run.status,
        artifact_path=run.artifact_path,
        stdout=logs.stdout,
        stderr=logs.stderr,
    )


def _artifact_unavailable() -> HTTPException:
    return HTTPException(
        status_code=status.HTTP_409_CONFLICT,
        detail={
            "code": "BACKTEST_ARTIFACT_UNAVAILABLE",
            "message": "backtest artifact is unavailable",
        },
    )
