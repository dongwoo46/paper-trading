from __future__ import annotations

import json
from collections.abc import Mapping
from dataclasses import dataclass
from decimal import Decimal, InvalidOperation
from pathlib import Path

from src.backtest.domain import BacktestSummary


class LeanResultParseError(ValueError):
    pass


def _reject_json_constant(value: str) -> object:
    raise ValueError(f"invalid JSON number: {value}")


@dataclass(frozen=True)
class ParsedLeanResult:
    summary: BacktestSummary
    details: dict[str, object]
    summary_path: Path
    full_result_path: Path


class LeanResultParser:
    """Parse the two JSON artifacts emitted by LEAN's backtest result handler."""

    def parse(self, results_dir: Path) -> ParsedLeanResult:
        summary_path, full_result_path = self.locate_result_files(results_dir)
        summary_payload = self._read_object(summary_path)
        full_payload = self._read_object(full_result_path)
        statistics = summary_payload.get("Statistics")
        if not isinstance(statistics, Mapping):
            raise LeanResultParseError(
                f"LEAN summary Statistics must be an object: {summary_path.name}"
            )
        if not isinstance(summary_payload.get("TotalPerformance"), Mapping):
            raise LeanResultParseError(
                f"LEAN summary TotalPerformance must be an object: {summary_path.name}"
            )

        total_return = self._required_decimal(statistics, "Net Profit", percent=True)
        max_drawdown = abs(self._required_decimal(statistics, "Drawdown", percent=True))
        annualized_return = self._required_decimal(
            statistics,
            "Compounding Annual Return",
            percent=True,
        )
        sharpe = self._required_decimal(statistics, "Sharpe Ratio")
        win_rate = self._required_decimal(statistics, "Win Rate", percent=True)
        total_trades = self._required_integer(statistics, "Total Trades")
        calmar_value = statistics.get("Calmar Ratio")
        if calmar_value is not None:
            calmar = self._decimal(calmar_value, "Calmar Ratio")
        elif max_drawdown != 0:
            calmar = annualized_return / max_drawdown
        elif annualized_return == 0:
            calmar = Decimal(0)
        else:
            raise LeanResultParseError(
                "Calmar Ratio cannot be derived when Drawdown is zero"
            )

        return ParsedLeanResult(
            summary=BacktestSummary(
                total_return=total_return,
                max_drawdown=max_drawdown,
                annualized_return=annualized_return,
                sharpe=sharpe,
                calmar=calmar,
                win_rate=win_rate,
                total_trades=total_trades,
            ),
            details=self._json_safe(full_payload),
            summary_path=summary_path,
            full_result_path=full_result_path,
        )

    @staticmethod
    def locate_result_files(results_dir: Path) -> tuple[Path, Path]:
        resolved_results_dir = results_dir.resolve()
        summaries = sorted(
            path for path in results_dir.rglob("*-summary.json") if path.is_file()
        )
        if len(summaries) != 1:
            raise LeanResultParseError(
                "expected exactly one LEAN *-summary.json result artifact, "
                f"found {len(summaries)}"
            )
        summary_candidate = summaries[0]
        algorithm_id = summary_candidate.name.removesuffix("-summary.json")
        full_result_candidate = summary_candidate.with_name(f"{algorithm_id}.json")
        if not full_result_candidate.is_file():
            raise LeanResultParseError(
                "matching LEAN full result artifact is missing: "
                f"{full_result_candidate.name}"
            )
        summary_path = LeanResultParser._confined_result_file(
            resolved_results_dir,
            summary_candidate,
        )
        full_result_path = LeanResultParser._confined_result_file(
            resolved_results_dir,
            full_result_candidate,
        )
        return summary_path, full_result_path

    @staticmethod
    def _confined_result_file(results_dir: Path, candidate: Path) -> Path:
        resolved = candidate.resolve()
        if not resolved.is_relative_to(results_dir):
            raise LeanResultParseError(
                f"LEAN result artifact is outside results directory: {candidate.name}"
            )
        return resolved

    @staticmethod
    def _read_object(path: Path) -> dict[str, object]:
        try:
            payload = json.loads(
                path.read_text(encoding="utf-8"),
                parse_float=Decimal,
                parse_constant=_reject_json_constant,
            )
        except (OSError, ValueError) as exc:
            raise LeanResultParseError(
                f"cannot read LEAN result artifact {path.name}: {exc}"
            ) from exc
        if not isinstance(payload, dict):
            raise LeanResultParseError(
                f"LEAN result artifact must contain an object: {path.name}"
            )
        return payload

    @classmethod
    def _required_decimal(
        cls,
        statistics: Mapping[str, object],
        field: str,
        *,
        percent: bool = False,
    ) -> Decimal:
        if field not in statistics:
            raise LeanResultParseError(f"required LEAN statistic is missing: {field}")
        return cls._decimal(statistics[field], field, percent=percent)

    @staticmethod
    def _decimal(value: object, field: str, *, percent: bool = False) -> Decimal:
        if isinstance(value, (bool, float)):
            raise LeanResultParseError(
                f"invalid decimal LEAN statistic {field}: {value}"
            )
        is_percent = False
        if isinstance(value, str):
            normalized = value.strip().replace(",", "")
            is_percent = normalized.endswith("%")
            if is_percent:
                normalized = normalized[:-1].strip()
        elif isinstance(value, (Decimal, int)):
            normalized = str(value)
        else:
            raise LeanResultParseError(
                f"invalid decimal LEAN statistic {field}: {value}"
            )
        if percent != is_percent:
            expectation = "required" if percent else "not allowed"
            raise LeanResultParseError(
                f"percent marker is {expectation} for LEAN statistic {field}: {value}"
            )
        try:
            parsed = Decimal(normalized)
        except (InvalidOperation, ValueError) as exc:
            raise LeanResultParseError(
                f"invalid decimal LEAN statistic {field}: {value}"
            ) from exc
        if not parsed.is_finite():
            raise LeanResultParseError(
                f"invalid decimal LEAN statistic {field}: {value}"
            )
        if percent:
            return parsed / Decimal(100)
        return parsed

    @classmethod
    def _required_integer(
        cls,
        statistics: Mapping[str, object],
        field: str,
    ) -> int:
        value = cls._required_decimal(statistics, field)
        integral = value.to_integral_value()
        if value != integral or integral < 0:
            raise LeanResultParseError(
                f"invalid integer LEAN statistic {field}: {value}"
            )
        return int(integral)

    @classmethod
    def _json_safe(cls, value: object) -> object:
        if isinstance(value, Decimal):
            return str(value)
        if isinstance(value, dict):
            return {str(key): cls._json_safe(item) for key, item in value.items()}
        if isinstance(value, list):
            return [cls._json_safe(item) for item in value]
        return value
