from __future__ import annotations

from collections.abc import Callable
from datetime import datetime
from uuid import UUID, uuid4

from src.backtest.domain import (
    BacktestRun,
    BacktestRunCreateRequest,
    BacktestSummary,
    RunStatus,
)
from src.backtest.repository import BacktestRunRepository


class BacktestRunNotFoundError(LookupError):
    pass


class InvalidRunTransitionError(ValueError):
    pass


_ALLOWED_TRANSITIONS = {
    RunStatus.PENDING: {RunStatus.RUNNING, RunStatus.FAILED, RunStatus.CANCELED},
    RunStatus.RUNNING: {RunStatus.COMPLETED, RunStatus.FAILED, RunStatus.CANCELED},
    RunStatus.COMPLETED: set(),
    RunStatus.FAILED: set(),
    RunStatus.CANCELED: set(),
}


class BacktestRunService:
    def __init__(
        self,
        repository: BacktestRunRepository,
        *,
        id_factory: Callable[[], UUID] = uuid4,
        clock: Callable[[], datetime] | None = None,
    ) -> None:
        self._repository = repository
        self._id_factory = id_factory
        self._clock = clock or datetime.now

    def create_run(self, request: BacktestRunCreateRequest) -> BacktestRun:
        run = BacktestRun(
            run_id=self._id_factory(),
            status=RunStatus.PENDING,
            market=request.market,
            symbol=request.symbol,
            resolution=request.resolution,
            start_date=request.start_date,
            end_date=request.end_date,
            initial_cash=request.initial_cash,
            currency=request.currency,
            cost_profile=request.cost_profile,
            strategy=request.strategy,
            created_at=self._clock(),
        )
        return self._repository.create(run)

    def get_run(self, run_id: UUID) -> BacktestRun:
        run = self._repository.get(run_id)
        if run is None:
            raise BacktestRunNotFoundError(str(run_id))
        return run

    def claim_pending(
        self,
        run_id: UUID,
        *,
        artifact_path: str,
    ) -> BacktestRun | None:
        return self._repository.claim_pending(
            run_id,
            started_at=self._clock(),
            artifact_path=artifact_path,
        )

    def update_status(
        self,
        run_id: UUID,
        status: RunStatus,
        *,
        error_message: str | None = None,
        artifact_path: str | None = None,
    ) -> BacktestRun:
        if status is RunStatus.RUNNING:
            claimed = self.claim_pending(
                run_id,
                artifact_path=artifact_path or "",
            )
            if claimed is not None:
                return claimed
            current = self.get_run(run_id)
            if current.status is RunStatus.RUNNING:
                return current
            raise InvalidRunTransitionError(
                f"invalid backtest run transition: {current.status.value} -> RUNNING"
            )

        current = self.get_run(run_id)
        if status is current.status:
            return current
        if status not in _ALLOWED_TRANSITIONS[current.status]:
            raise InvalidRunTransitionError(
                f"invalid backtest run transition: {current.status.value} -> {status.value}"
            )

        now = self._clock()
        updates: dict[str, object] = {
            "status": status,
            "artifact_path": artifact_path or current.artifact_path,
        }
        if status in {RunStatus.COMPLETED, RunStatus.FAILED, RunStatus.CANCELED}:
            updates["finished_at"] = now
        if status is RunStatus.FAILED:
            updates["error_message"] = error_message or "backtest execution failed"
        elif error_message is not None:
            raise InvalidRunTransitionError("error_message is only valid for FAILED runs")

        return self._repository.update(current.model_copy(update=updates))

    def store_summary(self, run_id: UUID, summary: BacktestSummary) -> BacktestRun:
        current = self.get_run(run_id)
        if current.status is not RunStatus.RUNNING:
            raise InvalidRunTransitionError(
                "summary can only be stored for a RUNNING backtest"
            )
        completed = current.model_copy(
            update={
                "status": RunStatus.COMPLETED,
                "summary": summary,
                "finished_at": self._clock(),
            }
        )
        return self._repository.update(completed)
