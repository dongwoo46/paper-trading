from __future__ import annotations

from dataclasses import dataclass
from datetime import date, datetime


@dataclass(frozen=True)
class CatalogSymbol:
    symbol: str
    name: str
    market: str
    enabled: bool
    is_default: bool
    fetched_until_date: date | None
    last_collected_at: datetime | None
