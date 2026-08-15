from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path

from src.backtest.domain import BacktestRun
from src.backtest.result_parser import LeanResultParser


class ArtifactAccessError(ValueError):
    pass


@dataclass(frozen=True)
class BacktestLogs:
    stdout: str
    stderr: str


class BacktestArtifactReader:
    def __init__(
        self,
        artifact_root: Path,
        *,
        parser: LeanResultParser | None = None,
    ) -> None:
        self._artifact_root = artifact_root.resolve()
        self._parser = parser or LeanResultParser()

    def read_details(self, run: BacktestRun) -> dict[str, object]:
        run_root = self._validated_run_root(run)
        results_dir = self._confined_path(run_root, "results")
        return self._parser.parse(results_dir).details

    def read_logs(self, run: BacktestRun) -> BacktestLogs:
        run_root = self._validated_run_root(run)
        stdout_path = self._confined_path(run_root, "logs", "docker.stdout.log")
        stderr_path = self._confined_path(run_root, "logs", "docker.stderr.log")
        return BacktestLogs(
            stdout=self._read_optional_text(stdout_path),
            stderr=self._read_optional_text(stderr_path),
        )

    def _validated_run_root(self, run: BacktestRun) -> Path:
        if run.artifact_path is None:
            raise ArtifactAccessError("backtest run has no artifact path")
        configured_run_root = self._artifact_root / str(run.run_id)
        expected = configured_run_root.resolve()
        candidate = Path(run.artifact_path).resolve()
        if (
            configured_run_root.is_symlink()
            or not expected.is_relative_to(self._artifact_root)
            or candidate != expected
        ):
            raise ArtifactAccessError(
                "backtest artifact path is outside configured run root"
            )
        if not candidate.is_dir():
            raise ArtifactAccessError("backtest artifact directory does not exist")
        return candidate

    @staticmethod
    def _confined_path(run_root: Path, *parts: str) -> Path:
        path = run_root.joinpath(*parts).resolve()
        if not path.is_relative_to(run_root):
            raise ArtifactAccessError("backtest artifact resolved outside run root")
        return path

    @staticmethod
    def _read_optional_text(path: Path) -> str:
        if not path.exists():
            return ""
        if not path.is_file():
            raise ArtifactAccessError(f"backtest log is not a file: {path.name}")
        try:
            return path.read_text(encoding="utf-8")
        except OSError as exc:
            raise ArtifactAccessError(f"cannot read backtest log: {path.name}") from exc
