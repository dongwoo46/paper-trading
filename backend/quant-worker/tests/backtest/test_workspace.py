from __future__ import annotations

import json
from datetime import datetime
from pathlib import Path
from uuid import UUID

import pytest

from src.backtest.domain import BacktestRunCreateRequest
from src.backtest.lean_template.cost_profiles import (
    CostProfileId,
    CostProfileSelectionError,
)
from src.backtest.repository import InMemoryBacktestRunRepository
from src.backtest.service import BacktestRunService
from src.backtest.workspace import (
    LeanWorkspaceBuilder,
    RunWorkspaceExistsError,
)

from .test_domain import run_request_payload

RUN_ID = UUID("00000000-0000-0000-0000-000000000003")
NOW = datetime(2024, 1, 1, 9, 0)  # noqa: DTZ001 - project contract is KST-naive


def make_run(*, market: str = "KR"):
    service = BacktestRunService(
        InMemoryBacktestRunRepository(),
        id_factory=lambda: RUN_ID,
        clock=lambda: NOW,
    )
    return service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload(market=market))
    )


def template_bytes(template_root: Path) -> dict[str, bytes]:
    return {
        str(path.relative_to(template_root)): path.read_bytes()
        for path in sorted(template_root.rglob("*"))
        if path.is_file()
    }


def test_workspace_contains_isolated_project_data_results_and_logs(
    tmp_path: Path,
) -> None:
    builder = LeanWorkspaceBuilder(tmp_path / "runs")

    workspace = builder.build(make_run())

    assert workspace.root == tmp_path / "runs" / str(RUN_ID)
    assert workspace.project_dir.is_dir()
    assert workspace.data_dir.is_dir()
    assert workspace.results_dir.is_dir()
    assert workspace.logs_dir.is_dir()
    assert workspace.storage_dir.is_dir()
    assert workspace.config_dir.is_dir()
    assert workspace.engine_config_path == workspace.config_dir / "config.json"
    assert workspace.engine_config_path.is_file()
    assert (workspace.project_dir / "main.py").is_file()
    assert (workspace.project_dir / "strategy_loader.py").is_file()
    assert (workspace.project_dir / "runtime.py").is_file()
    assert (workspace.project_dir / "cost_profiles.py").is_file()
    assert (workspace.project_dir / "strategy.json").is_file()
    assert (workspace.project_dir / "run_config.json").is_file()
    assert (workspace.project_dir / "cost_profile.json").is_file()


def test_kr_workspace_owns_reviewed_auxiliary_database_snapshot(tmp_path: Path) -> None:
    workspace = LeanWorkspaceBuilder(tmp_path / "runs").build(make_run())

    market_hours = json.loads(workspace.market_hours_database_path.read_text("utf-8"))
    symbol_properties = workspace.symbol_properties_database_path.read_text("utf-8")

    assert set(market_hours["entries"]) == {"Equity-krx-[*]"}
    assert market_hours["entries"]["Equity-krx-[*]"]["exchangeTimeZone"] == (
        "Asia/Seoul"
    )
    assert "krx,[*],equity,,KRW,1,1,1" in symbol_properties


def test_us_workspace_uses_auxiliary_databases_from_image(tmp_path: Path) -> None:
    workspace = LeanWorkspaceBuilder(tmp_path / "runs").build(
        make_run(market="US")
    )

    assert workspace.market_hours_database_path is None
    assert workspace.symbol_properties_database_path is None


def test_workspace_builder_never_mutates_fixed_template(tmp_path: Path) -> None:
    builder = LeanWorkspaceBuilder(tmp_path / "runs")
    before = template_bytes(builder.template_root)

    workspace = builder.build(make_run())

    assert template_bytes(builder.template_root) == before
    assert (workspace.project_dir / "main.py").read_bytes() == before["main.py"]
    assert (workspace.project_dir / "strategy_loader.py").read_bytes() == before[
        "strategy_loader.py"
    ]
    assert (workspace.project_dir / "runtime.py").read_bytes() == before["runtime.py"]
    assert (workspace.project_dir / "cost_profiles.py").read_bytes() == before[
        "cost_profiles.py"
    ]


def test_workspace_injects_deterministic_strategy_and_run_config_without_secrets(
    tmp_path: Path,
) -> None:
    first = LeanWorkspaceBuilder(tmp_path / "first").build(make_run())
    second = LeanWorkspaceBuilder(tmp_path / "second").build(make_run())

    first_files = template_bytes(first.project_dir)
    second_files = template_bytes(second.project_dir)
    assert first_files == second_files
    assert first.engine_config_path.read_bytes() == second.engine_config_path.read_bytes()

    assert (first.project_dir / "cost_profile.json").read_text("utf-8") == (
        '{"commission_bps_per_fill":"5","market":"KR",'
        '"profile_id":"KR_DEFAULT_V1","sell_tax_bps":"18",'
        '"slippage_bps_per_fill":"10"}\n'
    )

    config = json.loads((first.project_dir / "run_config.json").read_text("utf-8"))
    assert config == {
        "currency": "KRW",
        "end_date": "2024-12-31",
        "execution_policy": {
            "gap_buffer_bps": "500",
            "policy_id": "MOO_CLOSE_BUFFER_V1",
        },
        "initial_cash": "100000000",
        "lean_market": "krx",
        "market": "KR",
        "resolution": "daily",
        "run_id": str(RUN_ID),
        "start_date": "2024-01-01",
        "symbol": "005930",
    }
    workspace_text = "\n".join(
        path.read_text("utf-8")
        for path in first.root.rglob("*")
        if path.is_file()
    )
    assert "PG_PASSWORD" not in workspace_text
    assert "DB_PASSWORD" not in workspace_text
    assert "api_key" not in workspace_text.lower()
    assert "access-token" not in workspace_text.lower()


def test_workspace_writes_official_lean_backtesting_engine_config(
    tmp_path: Path,
) -> None:
    workspace = LeanWorkspaceBuilder(tmp_path / "runs").build(make_run())

    config = json.loads(workspace.engine_config_path.read_text("utf-8"))

    assert config == {
        "algorithm-language": "Python",
        "algorithm-location": "/LeanCLI/main.py",
        "algorithm-type-name": "FixedDslBacktestAlgorithm",
        "api-handler": "QuantConnect.Api.Api",
        "data-aggregator": "QuantConnect.Lean.Engine.DataFeeds.AggregationManager",
        "data-channel-provider": (
            "QuantConnect.Data.Channel.LocalFileSubscriptionStreamReader"
        ),
        "data-folder": "/Lean/Data",
        "data-provider": "QuantConnect.Lean.Engine.DataFeeds.DefaultDataProvider",
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
                    "QuantConnect.Lean.Engine.RealTime.BacktestingRealTimeHandler"
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
        "map-file-provider": "QuantConnect.Data.Auxiliary.LocalDiskMapFileProvider",
        "messaging-handler": "QuantConnect.Messaging.Messaging",
        "object-store": "QuantConnect.Lean.Engine.Storage.LocalObjectStore",
        "object-store-root": "/Storage",
        "results-destination-folder": "/Results",
    }


def test_existing_run_workspace_is_never_overwritten(tmp_path: Path) -> None:
    builder = LeanWorkspaceBuilder(tmp_path / "runs")
    builder.build(make_run())

    with pytest.raises(RunWorkspaceExistsError):
        builder.build(make_run())


def test_workspace_resolves_persisted_profile_id_instead_of_market_default(
    tmp_path: Path,
) -> None:
    mismatched_reloaded_run = make_run().model_copy(
        update={"cost_profile": CostProfileId.US_DEFAULT_V1}
    )

    with pytest.raises(CostProfileSelectionError) as raised:
        LeanWorkspaceBuilder(tmp_path / "runs").build(mismatched_reloaded_run)

    assert raised.value.code == "cost_profile_market_mismatch"
    assert not (tmp_path / "runs" / str(RUN_ID)).exists()


def test_fixed_template_does_not_execute_generated_python() -> None:
    template_root = LeanWorkspaceBuilder.default_template_root()
    source = "\n".join(
        path.read_text("utf-8") for path in template_root.glob("*.py")
    )

    assert "exec(" not in source
    assert "eval(" not in source
    assert "compile(" not in source
    assert "subprocess" not in source
