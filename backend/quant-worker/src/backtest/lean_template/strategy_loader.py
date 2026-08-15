from __future__ import annotations

import json
from pathlib import Path
from typing import Any

try:
    from cost_profiles import CostProfile, resolve_cost_profile
except ImportError:  # Imported as part of the quant-worker package in unit tests.
    from src.backtest.lean_template.cost_profiles import (
        CostProfile,
        resolve_cost_profile,
    )

PROJECT_ROOT = Path(__file__).resolve().parent


def load_strategy() -> dict[str, Any]:
    return _load_object("strategy.json")


def load_run_config() -> dict[str, Any]:
    return _load_object("run_config.json")


def load_cost_profile() -> CostProfile:
    value = _load_object("cost_profile.json")
    profile = resolve_cost_profile(value.get("profile_id"), market=value.get("market"))
    if value != profile.snapshot():
        raise ValueError("cost_profile.json does not match its immutable profile ID")
    return profile


def _load_object(filename: str) -> dict[str, Any]:
    value = json.loads((PROJECT_ROOT / filename).read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise TypeError(f"{filename} must contain a JSON object")
    return value
