from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import ClassVar
from uuid import UUID

from src.backtest.domain import BacktestRun, Market, strategy_snapshot_json
from src.backtest.lean_template.cost_profiles import (
    CostProfile,
    resolve_cost_profile,
    snapshot_cost_profile_json,
)
from src.backtest.path_safety import (
    copy_to_new_confined_file,
    create_confined_directory,
    require_confined_directory,
    require_confined_regular_file,
    require_confined_tree,
    write_new_confined_text,
)


class RunWorkspaceExistsError(FileExistsError):
    pass


@dataclass(frozen=True)
class LeanRunWorkspace:
    root: Path
    project_dir: Path
    data_dir: Path
    results_dir: Path
    logs_dir: Path
    storage_dir: Path
    config_dir: Path
    engine_config_path: Path
    market_hours_database_path: Path | None
    symbol_properties_database_path: Path | None


class LeanWorkspaceBuilder:
    _TEMPLATE_FILES = (
        "main.py",
        "strategy_loader.py",
        "runtime.py",
        "cost_profiles.py",
    )
    _LEAN_MARKET_BY_MARKET: ClassVar[dict[Market, str]] = {
        Market.KR: "krx",
        Market.US: "usa",
    }

    def __init__(
        self,
        artifact_root: Path,
        *,
        template_root: Path | None = None,
    ) -> None:
        self.artifact_root = artifact_root.resolve()
        self.template_root = template_root or self.default_template_root()

    @staticmethod
    def default_template_root() -> Path:
        return Path(__file__).resolve().parent / "lean_template"

    @staticmethod
    def default_auxiliary_root() -> Path:
        return Path(__file__).resolve().parent / "lean_auxiliary"

    def root_for(self, run_id: UUID) -> Path:
        return self.artifact_root / str(run_id)

    def data_root_for(self, run_id: UUID) -> Path:
        return self.root_for(run_id) / "data"

    def build(self, run: BacktestRun) -> LeanRunWorkspace:
        cost_profile = self._resolve_cost_profile(run)
        root = self._reserve_run_root(run.run_id)
        return self._populate(
            run,
            root,
            cost_profile=cost_profile,
            data_already_exists=True,
        )

    def reserve(self, run: BacktestRun) -> Path:
        self._resolve_cost_profile(run)
        return self._reserve_run_root(run.run_id)

    def _reserve_run_root(self, run_id: UUID) -> Path:
        self.artifact_root.mkdir(parents=True, exist_ok=True)
        require_confined_directory(self.artifact_root, self.artifact_root)
        root = self.root_for(run_id)
        try:
            root.mkdir(exist_ok=False)
        except FileExistsError as exc:
            raise RunWorkspaceExistsError(
                f"run workspace already exists: {run_id}"
            ) from exc
        require_confined_directory(self.artifact_root, root)
        create_confined_directory(root, root / "data")
        return root

    def build_after_export(self, run: BacktestRun) -> LeanRunWorkspace:
        """Finish a workspace whose data directory was created by the exporter."""

        cost_profile = self._resolve_cost_profile(run)
        root = self.root_for(run.run_id)
        data_dir = self.data_root_for(run.run_id)
        require_confined_directory(self.artifact_root, root)
        require_confined_directory(root, data_dir)
        unexpected = {path.name for path in root.iterdir()} - {"data"}
        if unexpected:
            raise RunWorkspaceExistsError(
                f"run workspace contains unexpected paths: {sorted(unexpected)}"
            )
        return self._populate(
            run,
            root,
            cost_profile=cost_profile,
            data_already_exists=True,
        )

    def _populate(
        self,
        run: BacktestRun,
        root: Path,
        *,
        cost_profile: CostProfile,
        data_already_exists: bool,
    ) -> LeanRunWorkspace:
        auxiliary_dir = root / "auxiliary"
        market_hours_database_path = None
        symbol_properties_database_path = None
        if run.market is Market.KR:
            market_hours_database_path = auxiliary_dir / "market-hours-database.json"
            symbol_properties_database_path = (
                auxiliary_dir / "symbol-properties-database.csv"
            )
        workspace = LeanRunWorkspace(
            root=root,
            project_dir=root / "project",
            data_dir=root / "data",
            results_dir=root / "results",
            logs_dir=root / "logs",
            storage_dir=root / "storage",
            config_dir=root / "config",
            engine_config_path=root / "config" / "config.json",
            market_hours_database_path=market_hours_database_path,
            symbol_properties_database_path=symbol_properties_database_path,
        )
        for directory in (
            workspace.project_dir,
            workspace.data_dir,
            workspace.results_dir,
            workspace.logs_dir,
            workspace.storage_dir,
            workspace.config_dir,
        ):
            if data_already_exists and directory == workspace.data_dir:
                require_confined_directory(root, directory)
            else:
                create_confined_directory(root, directory)

        if run.market is Market.KR:
            create_confined_directory(root, auxiliary_dir)
            self._copy_krx_auxiliary_snapshot(workspace)

        self._copy_fixed_template(workspace.project_dir)
        strategy_json = strategy_snapshot_json(run.strategy)
        write_new_confined_text(
            root,
            workspace.project_dir / "strategy.json",
            f"{strategy_json}\n",
        )
        run_config = {
            "currency": run.currency.value,
            "end_date": run.end_date.isoformat(),
            "execution_policy": {
                "gap_buffer_bps": "500",
                "policy_id": "MOO_CLOSE_BUFFER_V1",
            },
            "initial_cash": str(run.initial_cash),
            "lean_market": self._LEAN_MARKET_BY_MARKET[run.market],
            "market": run.market.value,
            "resolution": run.resolution,
            "run_id": str(run.run_id),
            "start_date": run.start_date.isoformat(),
            "symbol": run.symbol,
        }
        write_new_confined_text(
            root,
            workspace.project_dir / "run_config.json",
            f"{json.dumps(run_config, sort_keys=True, separators=(',', ':'))}\n",
        )
        write_new_confined_text(
            root,
            workspace.project_dir / "cost_profile.json",
            snapshot_cost_profile_json(cost_profile),
        )
        write_new_confined_text(
            root,
            workspace.engine_config_path,
            f"{json.dumps(self._lean_engine_config(), sort_keys=True, separators=(',', ':'))}\n",
        )
        validate_workspace_paths(workspace)
        return workspace

    def _copy_krx_auxiliary_snapshot(self, workspace: LeanRunWorkspace) -> None:
        assert workspace.market_hours_database_path is not None
        assert workspace.symbol_properties_database_path is not None
        auxiliary_root = self.default_auxiliary_root()
        copy_to_new_confined_file(
            workspace.root,
            auxiliary_root / "krx-market-hours-database.json",
            workspace.market_hours_database_path,
        )
        copy_to_new_confined_file(
            workspace.root,
            auxiliary_root / "krx-symbol-properties-database.csv",
            workspace.symbol_properties_database_path,
        )

    @staticmethod
    def _resolve_cost_profile(run: BacktestRun) -> CostProfile:
        return resolve_cost_profile(
            run.cost_profile,
            market=run.market.value,
        )

    def _copy_fixed_template(self, project_dir: Path) -> None:
        for filename in self._TEMPLATE_FILES:
            source = self.template_root / filename
            if not source.is_file():
                raise FileNotFoundError(f"fixed LEAN template file is missing: {source}")
            copy_to_new_confined_file(
                project_dir.parent,
                source,
                project_dir / filename,
            )

    @staticmethod
    def _lean_engine_config() -> dict[str, object]:
        return {
            "algorithm-language": "Python",
            "algorithm-location": "/LeanCLI/main.py",
            "algorithm-type-name": "FixedDslBacktestAlgorithm",
            "api-handler": "QuantConnect.Api.Api",
            "data-aggregator": (
                "QuantConnect.Lean.Engine.DataFeeds.AggregationManager"
            ),
            "data-channel-provider": (
                "QuantConnect.Data.Channel.LocalFileSubscriptionStreamReader"
            ),
            "data-folder": "/Lean/Data",
            "data-provider": (
                "QuantConnect.Lean.Engine.DataFeeds.DefaultDataProvider"
            ),
            "environment": "backtesting",
            "environments": {
                "backtesting": {
                    "data-feed-handler": (
                        "QuantConnect.Lean.Engine.DataFeeds.FileSystemDataFeed"
                    ),
                    "history-provider": [
                        (
                            "QuantConnect.Lean.Engine.HistoricalData."
                            "SubscriptionDataReaderHistoryProvider"
                        )
                    ],
                    "live-mode": False,
                    "real-time-handler": (
                        "QuantConnect.Lean.Engine.RealTime."
                        "BacktestingRealTimeHandler"
                    ),
                    "result-handler": (
                        "QuantConnect.Lean.Engine.Results.BacktestingResultHandler"
                    ),
                    "setup-handler": (
                        "QuantConnect.Lean.Engine.Setup.BacktestingSetupHandler"
                    ),
                    "transaction-handler": (
                        "QuantConnect.Lean.Engine.TransactionHandlers."
                        "BacktestingTransactionHandler"
                    ),
                }
            },
            "factor-file-provider": (
                "QuantConnect.Data.Auxiliary.LocalDiskFactorFileProvider"
            ),
            "job-queue-handler": "QuantConnect.Queues.JobQueue",
            "map-file-provider": (
                "QuantConnect.Data.Auxiliary.LocalDiskMapFileProvider"
            ),
            "messaging-handler": "QuantConnect.Messaging.Messaging",
            "object-store": "QuantConnect.Lean.Engine.Storage.LocalObjectStore",
            "object-store-root": "/Storage",
            "results-destination-folder": "/Results",
        }


def validate_workspace_paths(workspace: LeanRunWorkspace) -> None:
    require_confined_tree(workspace.root)
    for directory in (
        workspace.project_dir,
        workspace.data_dir,
        workspace.results_dir,
        workspace.logs_dir,
        workspace.storage_dir,
        workspace.config_dir,
    ):
        require_confined_directory(workspace.root, directory)
    require_confined_regular_file(workspace.root, workspace.engine_config_path)
    for optional_file in (
        workspace.market_hours_database_path,
        workspace.symbol_properties_database_path,
    ):
        if optional_file is not None:
            require_confined_regular_file(workspace.root, optional_file)
