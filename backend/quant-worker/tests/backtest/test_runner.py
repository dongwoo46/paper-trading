from __future__ import annotations

import subprocess
from collections.abc import Callable
from datetime import datetime
from pathlib import Path
from typing import Any
from uuid import UUID

import pytest

from src.backtest.domain import BacktestRunCreateRequest
from src.backtest.path_safety import UnsafeBacktestPathError
from src.backtest.repository import InMemoryBacktestRunRepository
from src.backtest.runner import DockerLeanRunner
from src.backtest.service import BacktestRunService
from src.backtest.workspace import LeanWorkspaceBuilder

from .test_domain import run_request_payload

RUN_ID = UUID("00000000-0000-0000-0000-000000000004")
NOW = datetime(2024, 1, 1, 9, 0)  # noqa: DTZ001 - project contract is KST-naive


class ProcessBoundary:
    def __init__(
        self,
        *,
        return_code: int = 0,
        stdout: str = "lean stdout",
        stderr: str = "lean stderr",
        timeout: bool = False,
        on_call: Callable[[], None] | None = None,
    ) -> None:
        self.return_code = return_code
        self.stdout = stdout
        self.stderr = stderr
        self.timeout = timeout
        self.on_call = on_call
        self.command: list[str] | None = None
        self.kwargs: dict[str, Any] | None = None

    def __call__(self, command: list[str], **kwargs: Any):
        self.command = command
        self.kwargs = kwargs
        if self.timeout:
            raise subprocess.TimeoutExpired(
                command,
                kwargs["timeout"],
                output="partial stdout",
                stderr="partial stderr",
            )
        if self.on_call is not None:
            self.on_call()
        return subprocess.CompletedProcess(
            command,
            self.return_code,
            stdout=self.stdout,
            stderr=self.stderr,
        )


def workspace(tmp_path: Path, *, market: str = "KR"):
    payload = run_request_payload(market=market)
    run = BacktestRunService(
        InMemoryBacktestRunRepository(),
        id_factory=lambda: RUN_ID,
        clock=lambda: NOW,
    ).create_run(BacktestRunCreateRequest.model_validate(payload))
    run_workspace = LeanWorkspaceBuilder(tmp_path / "runs").build(run)
    ticker = run.symbol.lower()
    lean_market = "krx" if market == "KR" else "usa"
    daily_zip = (
        run_workspace.data_dir
        / "equity"
        / lean_market
        / "daily"
        / f"{ticker}.zip"
    )
    daily_zip.parent.mkdir(parents=True)
    daily_zip.write_bytes(b"zip")
    return run_workspace


def mount_values(command: list[str]) -> list[str]:
    return [
        command[index + 1]
        for index, value in enumerate(command)
        if value == "--mount"
    ]


def test_command_uses_image_entrypoint_and_partial_read_only_data_mounts(
    tmp_path: Path,
) -> None:
    run_workspace = workspace(tmp_path)
    runner = DockerLeanRunner()

    command = runner.build_command(run_workspace)

    assert command[:4] == ["docker", "run", "--rm", "--network=none"]
    assert command[-1:] == ["quantconnect/lean:latest"]
    assert "dotnet" not in command
    assert "/Lean/Launcher/bin/Debug/QuantConnect.Lean.Launcher.dll" not in command
    mounts = mount_values(command)
    assert (
        f"type=bind,source={run_workspace.project_dir},target=/LeanCLI,readonly"
        in mounts
    )
    assert (
        f"type=bind,source={run_workspace.data_dir / 'equity/krx/daily/005930.zip'},"
        "target=/Lean/Data/equity/krx/daily/005930.zip,readonly"
        in mounts
    )
    assert all("target=/Lean/Data,readonly" not in mount for mount in mounts)
    assert (
        f"type=bind,source={run_workspace.market_hours_database_path},"
        "target=/Lean/Data/market-hours/market-hours-database.json,readonly"
        in mounts
    )
    assert (
        f"type=bind,source={run_workspace.symbol_properties_database_path},"
        "target=/Lean/Data/symbol-properties/symbol-properties-database.csv,readonly"
        in mounts
    )
    assert (
        f"type=bind,source={run_workspace.results_dir},target=/Results"
        in mounts
    )
    assert (
        f"type=bind,source={run_workspace.storage_dir},target=/Storage"
        in mounts
    )
    assert (
        f"type=bind,source={run_workspace.engine_config_path},"
        "target=/Lean/Launcher/bin/Debug/config.json,readonly"
        in mounts
    )
    assert all("target=/Lean/Logs" not in mount for mount in mounts)
    assert all(
        "readonly" not in mount
        for mount in mounts
        if "target=/Results" in mount or "target=/Storage" in mount
    )


def test_us_command_preserves_image_auxiliary_databases(tmp_path: Path) -> None:
    run_workspace = workspace(tmp_path, market="US")

    mounts = mount_values(DockerLeanRunner().build_command(run_workspace))

    assert any(
        "target=/Lean/Data/equity/usa/daily/aapl.zip,readonly" in mount
        for mount in mounts
    )
    assert all("target=/Lean/Data/market-hours/" not in mount for mount in mounts)
    assert all("target=/Lean/Data/symbol-properties/" not in mount for mount in mounts)


def test_command_rejects_data_file_symlink_outside_run_root(tmp_path: Path) -> None:
    run_workspace = workspace(tmp_path)
    daily_zip = run_workspace.data_dir / "equity/krx/daily/005930.zip"
    outside_zip = tmp_path / "outside.zip"
    outside_zip.write_bytes(b"outside")
    daily_zip.unlink()
    daily_zip.symlink_to(outside_zip)

    with pytest.raises(UnsafeBacktestPathError):
        DockerLeanRunner().build_command(run_workspace)


def test_command_rejects_data_directory_symlink_outside_run_root(
    tmp_path: Path,
) -> None:
    run_workspace = workspace(tmp_path)
    daily_dir = run_workspace.data_dir / "equity/krx/daily"
    (daily_dir / "005930.zip").unlink()
    daily_dir.rmdir()
    outside_dir = tmp_path / "outside"
    outside_dir.mkdir()
    (outside_dir / "005930.zip").write_bytes(b"outside")
    daily_dir.symlink_to(outside_dir, target_is_directory=True)

    with pytest.raises(UnsafeBacktestPathError):
        DockerLeanRunner().build_command(run_workspace)


@pytest.mark.parametrize(
    "log_name",
    ["docker.stdout.log", "docker.stderr.log"],
)
def test_runner_rejects_log_symlink_swap_without_writing_outside_run_root(
    tmp_path: Path,
    log_name: str,
) -> None:
    run_workspace = workspace(tmp_path)
    outside_log = tmp_path / "outside.log"
    outside_log.write_text("preserve", encoding="utf-8")

    def replace_log_with_symlink() -> None:
        (run_workspace.logs_dir / log_name).symlink_to(outside_log)

    process = ProcessBoundary(return_code=17, on_call=replace_log_with_symlink)

    with pytest.raises(UnsafeBacktestPathError):
        DockerLeanRunner(run_process=process).run(
            run_workspace,
            timeout_seconds=30,
        )

    assert outside_log.read_text(encoding="utf-8") == "preserve"


def test_success_captures_stdout_and_stderr_without_shell(
    tmp_path: Path,
) -> None:
    run_workspace = workspace(tmp_path)
    result_path = run_workspace.results_dir / "backtest" / "result.json"

    def write_result_during_process() -> None:
        result_path.parent.mkdir()
        result_path.write_text("{}", encoding="utf-8")

    process = ProcessBoundary(on_call=write_result_during_process)
    result = DockerLeanRunner(run_process=process).run(
        run_workspace,
        timeout_seconds=123,
    )

    assert result.succeeded is True
    assert result.timed_out is False
    assert result.return_code == 0
    assert result.stdout_path.read_text("utf-8") == "lean stdout"
    assert result.stderr_path.read_text("utf-8") == "lean stderr"
    assert result_path.is_file()
    assert process.kwargs == {
        "capture_output": True,
        "check": False,
        "shell": False,
        "text": True,
        "timeout": 123,
    }


def test_nonzero_exit_is_reported_and_output_is_preserved(tmp_path: Path) -> None:
    process = ProcessBoundary(return_code=17, stderr="engine failed")
    result = DockerLeanRunner(run_process=process).run(
        workspace(tmp_path),
        timeout_seconds=30,
    )

    assert result.succeeded is False
    assert result.timed_out is False
    assert result.return_code == 17
    assert result.stderr_path.read_text("utf-8") == "engine failed"


def test_zero_exit_without_new_recursive_json_result_is_failed(
    tmp_path: Path,
) -> None:
    run_workspace = workspace(tmp_path)
    stale_result = run_workspace.results_dir / "stale.json"
    stale_result.write_text('{"stale":true}', encoding="utf-8")
    process = ProcessBoundary(stdout="launcher exited cleanly")

    result = DockerLeanRunner(run_process=process).run(
        run_workspace,
        timeout_seconds=30,
    )

    assert result.succeeded is False
    assert result.timed_out is False
    assert result.return_code == 0
    assert result.error_message == "LEAN produced no new JSON result artifact"
    assert result.stdout_path.read_text("utf-8") == "launcher exited cleanly"
    assert result.stderr_path.read_text("utf-8") == "lean stderr"


def test_timeout_is_reported_and_partial_output_is_preserved(tmp_path: Path) -> None:
    process = ProcessBoundary(timeout=True)
    result = DockerLeanRunner(run_process=process).run(
        workspace(tmp_path),
        timeout_seconds=5,
    )

    assert result.succeeded is False
    assert result.timed_out is True
    assert result.return_code is None
    assert result.stdout_path.read_text("utf-8") == "partial stdout"
    assert result.stderr_path.read_text("utf-8") == "partial stderr"


def test_runner_workspace_includes_runtime_and_cost_snapshot(tmp_path: Path) -> None:
    run_workspace = workspace(tmp_path)

    assert (run_workspace.project_dir / "runtime.py").is_file()
    assert (run_workspace.project_dir / "cost_profiles.py").is_file()
    assert (run_workspace.project_dir / "cost_profile.json").is_file()
