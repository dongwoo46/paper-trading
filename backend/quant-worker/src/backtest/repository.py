from __future__ import annotations

import json
from copy import deepcopy
from datetime import datetime
from threading import Lock
from typing import Protocol
from uuid import UUID

from src.backtest.domain import (
    BacktestRun,
    BacktestSummary,
    Currency,
    Market,
    RunStatus,
    StrategyDefinition,
    strategy_snapshot_json,
)
from src.backtest.lean_template.cost_profiles import CostProfileId
from src.catalog.postgres_symbol_catalog import DbConfig, connect


class BacktestRunRepository(Protocol):
    def create(self, run: BacktestRun) -> BacktestRun: ...

    def update(self, run: BacktestRun) -> BacktestRun: ...

    def get(self, run_id: UUID) -> BacktestRun | None: ...

    def claim_pending(
        self,
        run_id: UUID,
        *,
        started_at: datetime,
        artifact_path: str,
    ) -> BacktestRun | None: ...


class InMemoryBacktestRunRepository:
    """Deterministic repository for local composition and domain tests."""

    def __init__(self) -> None:
        self._runs: dict[UUID, BacktestRun] = {}
        self._lock = Lock()

    def create(self, run: BacktestRun) -> BacktestRun:
        with self._lock:
            if run.run_id in self._runs:
                raise ValueError(f"backtest run already exists: {run.run_id}")
            self._runs[run.run_id] = deepcopy(run)
            return deepcopy(run)

    def update(self, run: BacktestRun) -> BacktestRun:
        with self._lock:
            if run.run_id not in self._runs:
                raise KeyError(run.run_id)
            self._runs[run.run_id] = deepcopy(run)
            return deepcopy(run)

    def get(self, run_id: UUID) -> BacktestRun | None:
        with self._lock:
            run = self._runs.get(run_id)
            return deepcopy(run) if run is not None else None

    def claim_pending(
        self,
        run_id: UUID,
        *,
        started_at: datetime,
        artifact_path: str,
    ) -> BacktestRun | None:
        with self._lock:
            current = self._runs.get(run_id)
            if current is None or current.status is not RunStatus.PENDING:
                return None
            claimed = current.model_copy(
                update={
                    "status": RunStatus.RUNNING,
                    "started_at": started_at,
                    "artifact_path": artifact_path,
                }
            )
            self._runs[run_id] = deepcopy(claimed)
            return deepcopy(claimed)


class PostgresBacktestRunRepository:
    _SELECT_COLUMNS = (
        "run_id, status, market, symbol, resolution, start_date, end_date, "
        "initial_cash, currency, cost_profile, strategy_json, artifact_path, error_message, "
        "total_return, max_drawdown, annualized_return, sharpe, calmar, win_rate, "
        "total_trades, created_at, started_at, finished_at"
    )

    def __init__(self, config: DbConfig) -> None:
        self._config = config

    def create(self, run: BacktestRun) -> BacktestRun:
        query = (
            "INSERT INTO backtest_runs ("
            "run_id, status, market, symbol, resolution, start_date, end_date, "
            "initial_cash, currency, cost_profile, strategy_json, artifact_path, error_message, "
            "total_return, max_drawdown, annualized_return, sharpe, calmar, win_rate, "
            "total_trades, created_at, started_at, finished_at"
            ") VALUES ("
            "%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s::jsonb, %s, %s, "
            "%s, %s, %s, %s, %s, %s, %s, %s, %s, %s"
            ")"
        )
        self._execute(query, self._params(run))
        return run

    def update(self, run: BacktestRun) -> BacktestRun:
        query = (
            "UPDATE backtest_runs SET "
            "status = %s, market = %s, symbol = %s, resolution = %s, "
            "start_date = %s, end_date = %s, initial_cash = %s, currency = %s, "
            "cost_profile = %s, strategy_json = %s::jsonb, artifact_path = %s, error_message = %s, "
            "total_return = %s, max_drawdown = %s, annualized_return = %s, "
            "sharpe = %s, calmar = %s, win_rate = %s, total_trades = %s, "
            "created_at = %s, started_at = %s, finished_at = %s "
            "WHERE run_id = %s"
        )
        params = list(self._params(run)[1:]) + [run.run_id]
        row_count = self._execute(query, params)
        if row_count == 0:
            raise KeyError(run.run_id)
        return run

    def get(self, run_id: UUID) -> BacktestRun | None:
        query = f"SELECT {self._SELECT_COLUMNS} FROM backtest_runs WHERE run_id = %s"
        with connect(self._config) as connection, connection.cursor() as cursor:
            cursor.execute(query, [run_id])
            row = cursor.fetchone()
        return self._to_run(row) if row is not None else None

    def claim_pending(
        self,
        run_id: UUID,
        *,
        started_at: datetime,
        artifact_path: str,
    ) -> BacktestRun | None:
        query = (
            "UPDATE backtest_runs SET status = %s, started_at = %s, artifact_path = %s "
            "WHERE run_id = %s AND status = %s "
            f"RETURNING {self._SELECT_COLUMNS}"
        )
        row = self._fetch_one(
            query,
            [
                RunStatus.RUNNING.value,
                started_at,
                artifact_path,
                run_id,
                RunStatus.PENDING.value,
            ],
        )
        return self._to_run(row) if row is not None else None

    def _execute(self, query: str, params: list[object]) -> int:
        with connect(self._config) as connection, connection.cursor() as cursor:
            cursor.execute(query, params)
            row_count = cursor.rowcount
            connection.commit()
        return row_count

    def _fetch_one(
        self,
        query: str,
        params: list[object],
    ) -> tuple[object, ...] | None:
        with connect(self._config) as connection, connection.cursor() as cursor:
            cursor.execute(query, params)
            row = cursor.fetchone()
            connection.commit()
        return row

    @staticmethod
    def _params(run: BacktestRun) -> list[object]:
        summary = run.summary
        return [
            run.run_id,
            run.status.value,
            run.market.value,
            run.symbol,
            run.resolution,
            run.start_date,
            run.end_date,
            run.initial_cash,
            run.currency.value,
            run.cost_profile.value,
            strategy_snapshot_json(run.strategy),
            run.artifact_path,
            run.error_message,
            summary.total_return if summary else None,
            summary.max_drawdown if summary else None,
            summary.annualized_return if summary else None,
            summary.sharpe if summary else None,
            summary.calmar if summary else None,
            summary.win_rate if summary else None,
            summary.total_trades if summary else None,
            run.created_at,
            run.started_at,
            run.finished_at,
        ]

    @staticmethod
    def _to_run(row: tuple[object, ...]) -> BacktestRun:
        strategy_value = row[10]
        if isinstance(strategy_value, str):
            strategy_value = json.loads(strategy_value)
        summary = None
        if row[13] is not None:
            summary = BacktestSummary(
                total_return=row[13],
                max_drawdown=row[14],
                annualized_return=row[15],
                sharpe=row[16],
                calmar=row[17],
                win_rate=row[18],
                total_trades=row[19],
            )
        return BacktestRun(
            run_id=row[0],
            status=RunStatus(row[1]),
            market=Market(row[2]),
            symbol=row[3],
            resolution=row[4],
            start_date=row[5],
            end_date=row[6],
            initial_cash=row[7],
            currency=Currency(row[8]),
            cost_profile=CostProfileId(row[9]),
            strategy=StrategyDefinition.model_validate(strategy_value),
            artifact_path=row[11],
            error_message=row[12],
            summary=summary,
            created_at=row[20],
            started_at=row[21],
            finished_at=row[22],
        )
