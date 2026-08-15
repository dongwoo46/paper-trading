from __future__ import annotations

import json
from datetime import datetime
from pathlib import Path
from uuid import UUID

import pytest

from src.backtest.artifacts import ArtifactAccessError, BacktestArtifactReader
from src.backtest.domain import BacktestRunCreateRequest, RunStatus
from src.backtest.repository import InMemoryBacktestRunRepository
from src.backtest.result_parser import LeanResultParseError, LeanResultParser
from src.backtest.service import BacktestRunService

from .test_domain import run_request_payload

RUN_ID = UUID("00000000-0000-0000-0000-000000000041")
NOW = datetime(2024, 1, 1, 9, 0)  # noqa: DTZ001 - project contract is KST-naive


def _write_official_result(results_dir: Path) -> tuple[Path, Path]:
    results_dir.mkdir(parents=True)
    statistics = {
        "Net Profit": "12.50%",
        "Drawdown": "5.00%",
        "Compounding Annual Return": "10.00%",
        "Sharpe Ratio": "1.25",
        "Win Rate": "60.00%",
        "Total Trades": "8",
    }
    summary_path = results_dir / "algorithm-summary.json"
    full_path = results_dir / "algorithm.json"
    summary_path.write_text(
        json.dumps(
            {
                "Statistics": statistics,
                "TotalPerformance": {"ClosedTrades": 8},
            }
        ),
        encoding="utf-8",
    )
    full_path.write_text(
        json.dumps(
            {
                "Statistics": statistics,
                "TotalPerformance": {
                    "PortfolioStatistics": {"EndEquity": 112500.25},
                },
            }
        ),
        encoding="utf-8",
    )
    return summary_path, full_path


def test_parser_normalizes_official_lean_summary_without_float_metrics(
    tmp_path: Path,
) -> None:
    summary_path, full_path = _write_official_result(tmp_path / "results")

    parsed = LeanResultParser().parse(tmp_path / "results")

    assert parsed.summary.model_dump(mode="json") == {
        "total_return": "0.125",
        "max_drawdown": "0.05",
        "annualized_return": "0.10",
        "sharpe": "1.25",
        "calmar": "2",
        "win_rate": "0.60",
        "total_trades": 8,
    }
    assert parsed.summary_path == summary_path
    assert parsed.full_result_path == full_path
    assert parsed.details["TotalPerformance"]["PortfolioStatistics"]["EndEquity"] == (
        "112500.25"
    )


def test_parser_prefers_official_calmar_statistic_when_present(tmp_path: Path) -> None:
    summary_path, _ = _write_official_result(tmp_path / "results")
    payload = json.loads(summary_path.read_text(encoding="utf-8"))
    payload["Statistics"]["Calmar Ratio"] = "3.75"
    summary_path.write_text(json.dumps(payload), encoding="utf-8")

    parsed = LeanResultParser().parse(tmp_path / "results")

    assert str(parsed.summary.calmar) == "3.75"


@pytest.mark.parametrize(
    ("field", "value"),
    [
        ("Net Profit", None),
        ("Sharpe Ratio", "not-a-number"),
        ("Total Trades", "1.5"),
    ],
)
def test_parser_rejects_missing_or_malformed_required_statistics(
    tmp_path: Path,
    field: str,
    value: object,
) -> None:
    summary_path, _ = _write_official_result(tmp_path / "results")
    payload = json.loads(summary_path.read_text(encoding="utf-8"))
    if value is None:
        del payload["Statistics"][field]
    else:
        payload["Statistics"][field] = value
    summary_path.write_text(json.dumps(payload), encoding="utf-8")

    with pytest.raises(LeanResultParseError, match=field):
        LeanResultParser().parse(tmp_path / "results")


def test_parser_does_not_treat_arbitrary_json_as_a_lean_result(tmp_path: Path) -> None:
    results_dir = tmp_path / "results"
    results_dir.mkdir()
    (results_dir / "strategy.json").write_text("{}", encoding="utf-8")
    (results_dir / "config.json").write_text("{}", encoding="utf-8")

    with pytest.raises(LeanResultParseError, match="summary"):
        LeanResultParser().parse(results_dir)


@pytest.mark.parametrize(
    ("field", "value"),
    [
        ("Net Profit", "12.50"),
        ("Drawdown", "5.00"),
        ("Compounding Annual Return", "10.00"),
        ("Win Rate", "60.00"),
        ("Sharpe Ratio", "1.25%"),
        ("Calmar Ratio", "2.00%"),
    ],
)
def test_parser_rejects_missing_or_unexpected_percent_markers(
    tmp_path: Path,
    field: str,
    value: str,
) -> None:
    summary_path, _ = _write_official_result(tmp_path / "results")
    payload = json.loads(summary_path.read_text(encoding="utf-8"))
    payload["Statistics"][field] = value
    summary_path.write_text(json.dumps(payload), encoding="utf-8")

    with pytest.raises(LeanResultParseError, match="percent marker"):
        LeanResultParser().parse(tmp_path / "results")


def test_parser_rejects_ambiguous_summaries_and_mismatched_full_result(
    tmp_path: Path,
) -> None:
    results_dir = tmp_path / "results"
    first_summary, first_full = _write_official_result(results_dir)
    second_summary = results_dir / "other-summary.json"
    second_summary.write_text(
        first_summary.read_text(encoding="utf-8"), encoding="utf-8"
    )

    with pytest.raises(LeanResultParseError, match="exactly one"):
        LeanResultParser().parse(results_dir)

    second_summary.unlink()
    first_full.rename(results_dir / "different.json")
    with pytest.raises(LeanResultParseError, match="matching LEAN full"):
        LeanResultParser().parse(results_dir)


def test_parser_rejects_result_file_symlinks_outside_results_root(
    tmp_path: Path,
) -> None:
    outside_summary, outside_full = _write_official_result(tmp_path / "outside")
    results_dir = tmp_path / "results"
    results_dir.mkdir()
    (results_dir / "algorithm-summary.json").symlink_to(outside_summary)
    (results_dir / "algorithm.json").symlink_to(outside_full)

    with pytest.raises(LeanResultParseError, match="outside results directory"):
        LeanResultParser().parse(results_dir)


def test_artifact_reader_confines_paths_and_returns_details_and_logs(
    tmp_path: Path,
) -> None:
    artifact_root = tmp_path / "artifacts"
    run_root = artifact_root / str(RUN_ID)
    _write_official_result(run_root / "results")
    (run_root / "logs").mkdir()
    (run_root / "logs" / "docker.stdout.log").write_text(
        "lean output", encoding="utf-8"
    )
    (run_root / "logs" / "docker.stderr.log").write_text(
        "lean warning", encoding="utf-8"
    )
    service = BacktestRunService(
        InMemoryBacktestRunRepository(),
        id_factory=lambda: RUN_ID,
        clock=lambda: NOW,
    )
    created = service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload())
    )
    running = service.update_status(
        created.run_id,
        RunStatus.RUNNING,
        artifact_path=str(run_root),
    )
    reader = BacktestArtifactReader(artifact_root)

    details = reader.read_details(running)
    logs = reader.read_logs(running)

    assert details["TotalPerformance"]["PortfolioStatistics"]["EndEquity"] == (
        "112500.25"
    )
    assert logs.stdout == "lean output"
    assert logs.stderr == "lean warning"


def test_artifact_reader_rejects_repository_path_outside_configured_run_root(
    tmp_path: Path,
) -> None:
    service = BacktestRunService(
        InMemoryBacktestRunRepository(),
        id_factory=lambda: RUN_ID,
        clock=lambda: NOW,
    )
    created = service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload())
    )
    untrusted = service.update_status(
        created.run_id,
        RunStatus.RUNNING,
        artifact_path=str(tmp_path / "outside"),
    )

    with pytest.raises(ArtifactAccessError, match="outside configured run root"):
        BacktestArtifactReader(tmp_path / "artifacts").read_logs(untrusted)


def test_artifact_reader_rejects_symlinked_run_root(tmp_path: Path) -> None:
    artifact_root = tmp_path / "artifacts"
    artifact_root.mkdir()
    outside = tmp_path / "outside"
    (outside / "logs").mkdir(parents=True)
    (outside / "logs" / "docker.stdout.log").write_text(
        "must not leak", encoding="utf-8"
    )
    (artifact_root / str(RUN_ID)).symlink_to(outside, target_is_directory=True)
    service = BacktestRunService(
        InMemoryBacktestRunRepository(),
        id_factory=lambda: RUN_ID,
        clock=lambda: NOW,
    )
    created = service.create_run(
        BacktestRunCreateRequest.model_validate(run_request_payload())
    )
    untrusted = service.update_status(
        created.run_id,
        RunStatus.RUNNING,
        artifact_path=str(artifact_root / str(RUN_ID)),
    )

    with pytest.raises(ArtifactAccessError, match="outside configured run root"):
        BacktestArtifactReader(artifact_root).read_logs(untrusted)
