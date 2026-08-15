from __future__ import annotations

import json
from dataclasses import dataclass
from enum import Enum
from typing import ClassVar


class CostProfileId(str, Enum):
    KR_DEFAULT_V1 = "KR_DEFAULT_V1"
    US_DEFAULT_V1 = "US_DEFAULT_V1"


@dataclass(frozen=True)
class CostProfile:
    profile_id: CostProfileId
    market: str
    commission_bps_per_fill: str
    slippage_bps_per_fill: str
    sell_tax_bps: str

    def snapshot(self) -> dict[str, str]:
        return {
            "commission_bps_per_fill": self.commission_bps_per_fill,
            "market": self.market,
            "profile_id": self.profile_id.value,
            "sell_tax_bps": self.sell_tax_bps,
            "slippage_bps_per_fill": self.slippage_bps_per_fill,
        }


class CostProfileSelectionError(ValueError):
    def __init__(self, code: str, message: str) -> None:
        super().__init__(message)
        self.code = code


_PROFILES: ClassVar[dict[CostProfileId, CostProfile]] = {
    CostProfileId.KR_DEFAULT_V1: CostProfile(
        profile_id=CostProfileId.KR_DEFAULT_V1,
        market="KR",
        commission_bps_per_fill="5",
        slippage_bps_per_fill="10",
        sell_tax_bps="18",
    ),
    CostProfileId.US_DEFAULT_V1: CostProfile(
        profile_id=CostProfileId.US_DEFAULT_V1,
        market="US",
        commission_bps_per_fill="5",
        slippage_bps_per_fill="10",
        sell_tax_bps="0",
    ),
}


def resolve_cost_profile(
    profile_id: CostProfileId | str,
    *,
    market: str | None = None,
) -> CostProfile:
    try:
        normalized_id = CostProfileId(profile_id)
    except (TypeError, ValueError) as exc:
        raise CostProfileSelectionError(
            "unknown_cost_profile",
            f"unknown cost profile: {profile_id}",
        ) from exc
    profile = _PROFILES[normalized_id]
    if market is not None and profile.market != market:
        raise CostProfileSelectionError(
            "cost_profile_market_mismatch",
            f"cost profile {normalized_id.value} does not match market {market}",
        )
    return profile


def snapshot_cost_profile_json(profile: CostProfile) -> str:
    return f"{json.dumps(profile.snapshot(), sort_keys=True, separators=(',', ':'))}\n"
