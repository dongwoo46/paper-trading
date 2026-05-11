"""도메인 Value Objects — 금융 안전: 모든 가격/수치 필드는 Decimal 사용."""
from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from decimal import Decimal
from enum import Enum
from typing import Any


# ---------------------------------------------------------------------------
# Enums
# ---------------------------------------------------------------------------

class Direction(Enum):
    UPTREND = "uptrend"
    DOWNTREND = "downtrend"
    SIDEWAYS = "sideways"


class Strength(Enum):
    WEAK = "weak"
    MEDIUM = "medium"
    STRONG = "strong"


class Grade(Enum):
    STRONG_BUY = "STRONG_BUY"
    BUY = "BUY"
    HOLD = "HOLD"
    SELL = "SELL"
    STRONG_SELL = "STRONG_SELL"


class PatternType(Enum):
    DOJI = "DOJI"
    HAMMER = "HAMMER"
    INVERTED_HAMMER = "INVERTED_HAMMER"
    BULLISH_ENGULFING = "BULLISH_ENGULFING"
    BEARISH_ENGULFING = "BEARISH_ENGULFING"
    MORNING_STAR = "MORNING_STAR"
    EVENING_STAR = "EVENING_STAR"


class ReportSource(Enum):
    LLM_PRIMARY = "llm_primary"
    RULE_TEMPLATE = "rule_template"
    NONE = "none"


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _require_decimal(value: Any, field_name: str) -> None:
    """Decimal이 아닌 값(float, int 등)을 거부한다."""
    if not isinstance(value, Decimal):
        raise TypeError(
            f"{field_name} must be Decimal, got {type(value).__name__}."
            " Use Decimal('value') instead of float/int."
        )


# ---------------------------------------------------------------------------
# Value Objects
# ---------------------------------------------------------------------------

@dataclass(frozen=True)
class Candle:
    date: date
    open: Decimal
    high: Decimal
    low: Decimal
    close: Decimal
    volume: Decimal

    def __post_init__(self) -> None:
        for field_name in ("open", "high", "low", "close", "volume"):
            _require_decimal(getattr(self, field_name), f"Candle.{field_name}")


@dataclass(frozen=True)
class IndicatorSet:
    ma20: Decimal
    ma60: Decimal
    ma120: Decimal
    rsi14: Decimal
    macd: Decimal
    macd_signal: Decimal
    macd_hist: Decimal
    bb_upper: Decimal
    bb_lower: Decimal
    atr14: Decimal
    adx14: Decimal
    stoch_k: Decimal
    stoch_d: Decimal
    obv: Decimal
    volume_ma20: Decimal

    def __post_init__(self) -> None:
        decimal_fields = (
            "ma20", "ma60", "ma120", "rsi14", "macd", "macd_signal",
            "macd_hist", "bb_upper", "bb_lower", "atr14", "adx14",
            "stoch_k", "stoch_d", "obv", "volume_ma20",
        )
        for field_name in decimal_fields:
            _require_decimal(getattr(self, field_name), f"IndicatorSet.{field_name}")


@dataclass(frozen=True)
class TrendAnalysis:
    direction: Direction
    strength: Strength
    ma_alignment: str
    adx: Decimal
    hh_ll_structure: str

    def __post_init__(self) -> None:
        _require_decimal(self.adx, "TrendAnalysis.adx")
        if not isinstance(self.direction, Direction):
            raise TypeError(f"direction must be Direction enum, got {type(self.direction).__name__}")
        if not isinstance(self.strength, Strength):
            raise TypeError(f"strength must be Strength enum, got {type(self.strength).__name__}")


@dataclass(frozen=True)
class LevelSet:
    supports: tuple[Decimal, ...]
    resistances: tuple[Decimal, ...]

    def __init__(self, supports: list[Decimal], resistances: list[Decimal]) -> None:
        # Validate each level is Decimal
        for i, v in enumerate(supports):
            _require_decimal(v, f"LevelSet.supports[{i}]")
        for i, v in enumerate(resistances):
            _require_decimal(v, f"LevelSet.resistances[{i}]")
        object.__setattr__(self, "supports", tuple(supports))
        object.__setattr__(self, "resistances", tuple(resistances))


@dataclass(frozen=True)
class TradePlan:
    entry_price: Decimal
    stop_loss: Decimal
    target_price: Decimal
    risk_reward_ratio: Decimal

    def __post_init__(self) -> None:
        for field_name in ("entry_price", "stop_loss", "target_price", "risk_reward_ratio"):
            _require_decimal(getattr(self, field_name), f"TradePlan.{field_name}")


@dataclass(frozen=True)
class CandlePattern:
    type: PatternType
    index: int
    date: date

    def __post_init__(self) -> None:
        if not isinstance(self.type, PatternType):
            raise TypeError(f"type must be PatternType enum, got {type(self.type).__name__}")


@dataclass(frozen=True)
class IndicatorSignal:
    name: str
    value: Decimal
    interpretation: str

    def __post_init__(self) -> None:
        _require_decimal(self.value, "IndicatorSignal.value")


@dataclass(frozen=True)
class VolumeAnalysis:
    trend: str
    spike_detected: bool
    avg_ratio: Decimal

    def __post_init__(self) -> None:
        _require_decimal(self.avg_ratio, "VolumeAnalysis.avg_ratio")


@dataclass(frozen=True)
class Recommendation:
    grade: Grade
    confidence: Decimal

    def __post_init__(self) -> None:
        _require_decimal(self.confidence, "Recommendation.confidence")
        if not isinstance(self.grade, Grade):
            raise TypeError(f"grade must be Grade enum, got {type(self.grade).__name__}")
        if not (Decimal("0") <= self.confidence <= Decimal("1")):
            raise ValueError(
                f"Recommendation.confidence must be in [0, 1], got {self.confidence}"
            )


@dataclass(frozen=True)
class NarrativeReport:
    trend_section: str
    support_resistance_section: str
    entry_plan_section: str
    signal_evidence_section: str
    risk_section: str
    source: ReportSource

    def __post_init__(self) -> None:
        sections = {
            "trend_section": self.trend_section,
            "support_resistance_section": self.support_resistance_section,
            "entry_plan_section": self.entry_plan_section,
            "signal_evidence_section": self.signal_evidence_section,
            "risk_section": self.risk_section,
        }
        for field_name, value in sections.items():
            if not isinstance(value, str) or not value.strip():
                raise ValueError(
                    f"NarrativeReport.{field_name} must be a non-empty string"
                )
        if not isinstance(self.source, ReportSource):
            raise TypeError(f"source must be ReportSource enum, got {type(self.source).__name__}")