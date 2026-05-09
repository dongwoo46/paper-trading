from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal, InvalidOperation

import pandas as pd


@dataclass(frozen=True)
class ProviderWindow:
    provider: str


class MicrostructureCollector:
    OUTPUT_COLUMNS = [
        "as_of",
        "symbol",
        "source",
        "bid",
        "ask",
        "bid_size",
        "ask_size",
        "spread",
        "bid_ask_available",
        "depth_available",
        "availability_note",
    ]

    def normalize_kr(self, frame: pd.DataFrame) -> pd.DataFrame:
        out = frame.copy()
        out["as_of"] = pd.to_datetime(out["timestamp"], utc=True)
        out["bid_ask_available"] = out["bid"].notna() & out["ask"].notna()
        out["depth_available"] = out["bid_size"].notna() & out["ask_size"].notna()
        if "availability_note" not in out.columns:
            out["availability_note"] = None
        return out[self.OUTPUT_COLUMNS]


class SessionOhlcvCollector:
    OUTPUT_COLUMNS = ["as_of", "symbol", "source", "session", "open", "high", "low", "close", "volume"]
    SESSION_ALIASES = {
        "regular": "regular",
        "reg": "regular",
        "rth": "regular",
        "pre": "pre",
        "premarket": "pre",
        "pre_market": "pre",
        "pre-market": "pre",
        "after": "after",
        "post": "after",
        "afterhours": "after",
        "after_hours": "after",
        "postmarket": "after",
        "post_market": "after",
        "post-market": "after",
    }

    @classmethod
    def _canonicalize_session(cls, raw_session: object) -> str:
        key = str(raw_session or "").strip().lower()
        if key in cls.SESSION_ALIASES:
            return cls.SESSION_ALIASES[key]
        raise ValueError(f"unsupported session alias: {raw_session}")

    def normalize_us(self, frame: pd.DataFrame, source: str) -> pd.DataFrame:
        out = frame.copy()
        out["as_of"] = pd.to_datetime(out["timestamp"], utc=True)
        out["source"] = source
        out["session"] = out["session"].map(self._canonicalize_session)
        return out[self.OUTPUT_COLUMNS]


class RelativeStrengthCollector:
    OUTPUT_COLUMNS = [
        "as_of",
        "symbol",
        "source",
        "benchmark_symbol",
        "symbol_return",
        "benchmark_return",
        "relative_strength",
    ]

    def normalize(self, frame: pd.DataFrame, source: str) -> pd.DataFrame:
        def _to_decimal(value: object) -> Decimal | None:
            if value is None or pd.isna(value):
                return None
            try:
                parsed = Decimal(str(value))
            except (InvalidOperation, ValueError, TypeError):
                return None
            if not parsed.is_finite():
                return None
            return parsed

        def _relative_strength(row: pd.Series) -> Decimal | None:
            symbol_return = _to_decimal(row.get("symbol_return"))
            benchmark_return = _to_decimal(row.get("benchmark_return"))
            if symbol_return is None or benchmark_return in (None, Decimal("0")):
                return None
            return symbol_return / benchmark_return

        out = frame.copy()
        out["as_of"] = pd.to_datetime(out["as_of"], utc=True)
        out["source"] = source
        if "benchmark_symbol" not in out.columns:
            out["benchmark_symbol"] = "SPY"
        out["relative_strength"] = out.apply(_relative_strength, axis=1)
        return out[self.OUTPUT_COLUMNS]


class AlternativeFlowCollector:
    OUTPUT_COLUMNS = [
        "as_of",
        "symbol",
        "source",
        "short_interest",
        "days_to_cover",
        "shares_outstanding",
        "float_shares",
    ]

    def normalize(self, frame: pd.DataFrame, source: str) -> pd.DataFrame:
        out = frame.copy()
        out["as_of"] = pd.to_datetime(out["as_of"], utc=True)
        out["source"] = source
        return out[self.OUTPUT_COLUMNS]


class MetadataCollector:
    OUTPUT_COLUMNS = [
        "as_of",
        "symbol",
        "source",
        "exchange",
        "currency",
        "timezone",
        "market_cap",
        "free_float",
    ]

    def normalize(self, frame: pd.DataFrame, source: str) -> pd.DataFrame:
        out = frame.copy()
        out["as_of"] = pd.to_datetime(out["as_of"], utc=True)
        out["source"] = source
        return out[self.OUTPUT_COLUMNS]

