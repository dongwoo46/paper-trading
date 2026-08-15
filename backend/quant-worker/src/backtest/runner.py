from __future__ import annotations

import subprocess
from collections.abc import Callable
from dataclasses import dataclass
from pathlib import Path, PurePosixPath
from typing import Any

from src.backtest.path_safety import create_confined_directory, write_new_confined_text
from src.backtest.workspace import LeanRunWorkspace, validate_workspace_paths


@dataclass(frozen=True)
class LeanProcessResult:
    command: tuple[str, ...]
    succeeded: bool
    timed_out: bool
    return_code: int | None
    stdout_path: Path
    stderr_path: Path
    error_message: str | None = None


class DockerLeanRunner:
    def __init__(
        self,
        *,
        image: str = "quantconnect/lean:latest",
        docker_executable: str = "docker",
        run_process: Callable[..., Any] = subprocess.run,
    ) -> None:
        self._image = image
        self._docker_executable = docker_executable
        self._run_process = run_process

    def build_command(self, workspace: LeanRunWorkspace) -> list[str]:
        validate_workspace_paths(workspace)
        command = [
            self._docker_executable,
            "run",
            "--rm",
            "--network=none",
            "--mount",
            self._mount(workspace.project_dir, "/LeanCLI", read_only=True),
        ]
        for data_file in sorted(workspace.data_dir.rglob("*.zip")):
            relative_path = data_file.relative_to(workspace.data_dir)
            target = str(PurePosixPath("/Lean/Data", *relative_path.parts))
            command.extend(
                ["--mount", self._mount(data_file, target, read_only=True)]
            )
        if workspace.market_hours_database_path is not None:
            command.extend(
                [
                    "--mount",
                    self._mount(
                        workspace.market_hours_database_path,
                        "/Lean/Data/market-hours/market-hours-database.json",
                        read_only=True,
                    ),
                ]
            )
        if workspace.symbol_properties_database_path is not None:
            command.extend(
                [
                    "--mount",
                    self._mount(
                        workspace.symbol_properties_database_path,
                        (
                            "/Lean/Data/symbol-properties/"
                            "symbol-properties-database.csv"
                        ),
                        read_only=True,
                    ),
                ]
            )
        command.extend(
            [
                "--mount",
                self._mount(workspace.results_dir, "/Results"),
                "--mount",
                self._mount(workspace.storage_dir, "/Storage"),
                "--mount",
                self._mount(
                    workspace.engine_config_path,
                    "/Lean/Launcher/bin/Debug/config.json",
                    read_only=True,
                ),
                self._image,
            ]
        )
        return command

    def run(
        self,
        workspace: LeanRunWorkspace,
        *,
        timeout_seconds: int,
    ) -> LeanProcessResult:
        if timeout_seconds <= 0:
            raise ValueError("timeout_seconds must be greater than zero")

        create_confined_directory(workspace.root, workspace.logs_dir)
        create_confined_directory(workspace.root, workspace.results_dir)
        create_confined_directory(workspace.root, workspace.storage_dir)
        stdout_path = workspace.logs_dir / "docker.stdout.log"
        stderr_path = workspace.logs_dir / "docker.stderr.log"
        command = self.build_command(workspace)
        result_state_before = self._json_result_state(workspace.results_dir)

        try:
            completed = self._run_process(
                command,
                capture_output=True,
                check=False,
                shell=False,
                text=True,
                timeout=timeout_seconds,
            )
        except subprocess.TimeoutExpired as exc:
            stdout = self._to_text(exc.output)
            stderr = self._to_text(exc.stderr)
            self._write_logs(
                workspace.root,
                stdout_path,
                stderr_path,
                stdout,
                stderr,
            )
            return LeanProcessResult(
                command=tuple(command),
                succeeded=False,
                timed_out=True,
                return_code=None,
                stdout_path=stdout_path,
                stderr_path=stderr_path,
                error_message=f"LEAN Docker execution timed out after {timeout_seconds}s",
            )
        except OSError as exc:
            self._write_logs(
                workspace.root,
                stdout_path,
                stderr_path,
                "",
                str(exc),
            )
            return LeanProcessResult(
                command=tuple(command),
                succeeded=False,
                timed_out=False,
                return_code=None,
                stdout_path=stdout_path,
                stderr_path=stderr_path,
                error_message=str(exc),
            )

        stdout = self._to_text(completed.stdout)
        stderr = self._to_text(completed.stderr)
        self._write_logs(
            workspace.root,
            stdout_path,
            stderr_path,
            stdout,
            stderr,
        )
        return_code = int(completed.returncode)
        if return_code == 0 and not self._has_new_json_result(
            result_state_before,
            self._json_result_state(workspace.results_dir),
        ):
            return LeanProcessResult(
                command=tuple(command),
                succeeded=False,
                timed_out=False,
                return_code=return_code,
                stdout_path=stdout_path,
                stderr_path=stderr_path,
                error_message="LEAN produced no new JSON result artifact",
            )
        return LeanProcessResult(
            command=tuple(command),
            succeeded=return_code == 0,
            timed_out=False,
            return_code=return_code,
            stdout_path=stdout_path,
            stderr_path=stderr_path,
            error_message=None if return_code == 0 else f"LEAN exited with code {return_code}",
        )

    @staticmethod
    def _json_result_state(root: Path) -> dict[Path, tuple[int, int]]:
        return {
            path: (path.stat().st_mtime_ns, path.stat().st_size)
            for path in root.rglob("*.json")
            if path.is_file()
        }

    @staticmethod
    def _has_new_json_result(
        before: dict[Path, tuple[int, int]],
        after: dict[Path, tuple[int, int]],
    ) -> bool:
        return any(before.get(path) != state for path, state in after.items())

    @staticmethod
    def _mount(source: Path, target: str, *, read_only: bool = False) -> str:
        mount = f"type=bind,source={source.resolve()},target={target}"
        return f"{mount},readonly" if read_only else mount

    @staticmethod
    def _to_text(value: object) -> str:
        if value is None:
            return ""
        if isinstance(value, bytes):
            return value.decode("utf-8", errors="replace")
        return str(value)

    @staticmethod
    def _write_logs(
        root: Path,
        stdout_path: Path,
        stderr_path: Path,
        stdout: str,
        stderr: str,
    ) -> None:
        write_new_confined_text(root, stdout_path, stdout)
        write_new_confined_text(root, stderr_path, stderr)
