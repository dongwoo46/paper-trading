from __future__ import annotations

import json
import re
from datetime import date, datetime
from decimal import Decimal, InvalidOperation
from enum import Enum
from typing import Any
from uuid import UUID

from pydantic import (
    AliasChoices,
    BaseModel,
    ConfigDict,
    Field,
    ValidationInfo,
    field_validator,
    model_validator,
)
from pydantic_core import PydanticCustomError

from src.backtest.lean_template.cost_profiles import (
    CostProfileId,
    CostProfileSelectionError,
    resolve_cost_profile,
)


class RunStatus(str, Enum):
    PENDING = "PENDING"
    RUNNING = "RUNNING"
    COMPLETED = "COMPLETED"
    FAILED = "FAILED"
    CANCELED = "CANCELED"


class Market(str, Enum):
    KR = "KR"
    US = "US"


class Currency(str, Enum):
    KRW = "KRW"
    USD = "USD"


CURRENCY_BY_MARKET = {
    Market.KR: Currency.KRW,
    Market.US: Currency.USD,
}


class FactorCategory(str, Enum):
    PRICE = "price"
    TECHNICAL = "technical"
    FLOW = "flow"
    FUNDAMENTAL = "fundamental"
    MACRO = "macro"
    NEWS_SENTIMENT = "news_sentiment"
    DISCLOSURE = "disclosure"
    PAPER_FACTOR = "paper_factor"
    EVENT = "event"


EXECUTABLE_FACTOR_CATEGORIES = {
    FactorCategory.PRICE,
    FactorCategory.TECHNICAL,
}


class Indicator(str, Enum):
    PRICE = "price"
    RETURNS = "returns"
    SMA = "sma"
    EMA = "ema"
    WMA = "wma"
    RSI = "rsi"
    MACD = "macd"
    BOLLINGER = "bollinger"
    ATR = "atr"
    ROC = "roc"
    MOMENTUM = "momentum"


class PriceField(str, Enum):
    OPEN = "open"
    HIGH = "high"
    LOW = "low"
    CLOSE = "close"
    VOLUME = "volume"
    ADJUSTED_CLOSE = "adjusted_close"


class LogicalOperator(str, Enum):
    AND = "and"
    OR = "or"


class ConditionType(str, Enum):
    CROSS_ABOVE = "cross_above"
    CROSS_BELOW = "cross_below"
    GREATER_THAN = "greater_than"
    LESS_THAN = "less_than"
    EQUAL = "equal"


def _require_decimal_input(value: object) -> object:
    if isinstance(value, float):
        raise PydanticCustomError(
            "decimal_string_required",
            "floating-point values are not accepted; provide a decimal string",
        )
    return value


def _require_decimal_string(value: object) -> object:
    if value is None:
        return value
    if not isinstance(value, str):
        raise PydanticCustomError(
            "decimal_string_required",
            "provide a finite decimal string",
        )
    try:
        parsed = Decimal(value)
    except (InvalidOperation, ValueError) as exc:
        raise PydanticCustomError(
            "decimal_string_required",
            "provide a finite decimal string",
        ) from exc
    if not parsed.is_finite():
        raise PydanticCustomError(
            "decimal_string_required",
            "provide a finite decimal string",
        )
    return value


class DataReference(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    field: PriceField | None = None
    factor: str | None = None
    value: str | None = None
    lag: int = 0
    shift: int | None = None

    @model_validator(mode="before")
    @classmethod
    def validate_shape(cls, value: object) -> object:
        if not isinstance(value, dict):
            raise PydanticCustomError(
                "invalid_condition_operand",
                "condition operand must be an object",
            )
        allowed = {"field", "factor", "value", "lag", "shift"}
        if set(value) - allowed:
            raise PydanticCustomError(
                "invalid_condition_operand",
                "condition operand contains unsupported keys",
            )
        reference_keys = [key for key in ("field", "factor", "value") if key in value]
        if len(reference_keys) != 1:
            raise PydanticCustomError(
                "invalid_condition_operand",
                "condition operand must contain exactly one field, factor, or value",
            )
        literal = value.get("value")
        if "value" in value:
            if not isinstance(literal, str):
                raise PydanticCustomError(
                    "decimal_string_required",
                    "literal values must be decimal strings",
                )
            try:
                decimal_value = Decimal(literal)
            except Exception as exc:
                raise PydanticCustomError(
                    "decimal_string_required",
                    "literal values must be decimal strings",
                ) from exc
            if not decimal_value.is_finite():
                raise PydanticCustomError(
                    "decimal_string_required",
                    "literal values must be finite decimal strings",
                )
        return value

    @field_validator("field", mode="before")
    @classmethod
    def validate_field(cls, value: object) -> object:
        if value == PriceField.ADJUSTED_CLOSE.value:
            raise PydanticCustomError(
                "unsupported_price_field",
                "adjusted_close is not available in exported LEAN data",
            )
        try:
            return PriceField(value)
        except (TypeError, ValueError) as exc:
            raise PydanticCustomError(
                "unknown_price_field",
                "unknown raw price field '{field}'",
                {"field": value},
            ) from exc

    @model_validator(mode="after")
    def validate_reference(self) -> DataReference:
        if self.factor is not None and not self.factor.strip():
            raise PydanticCustomError(
                "invalid_condition_operand",
                "factor reference must not be blank",
            )
        if isinstance(self.lag, bool) or not isinstance(self.lag, int):
            raise PydanticCustomError(
                "invalid_condition_operand",
                "reference lag must be an integer",
            )
        if self.lag != 0:
            raise PydanticCustomError(
                "unsupported_reference_lag",
                "nonzero reference lag is not executable",
            )
        if self.shift is not None and self.shift != 0:
            raise PydanticCustomError(
                "look_ahead_reference",
                "nonzero shifted data references are not allowed",
            )
        return self


class StrategyFactor(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    id: str
    category: FactorCategory
    indicator: Indicator
    params: dict[str, Any] = Field(default_factory=dict)
    source: DataReference

    @field_validator("id")
    @classmethod
    def validate_id(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("factor id must not be blank")
        return normalized

    @field_validator("category", mode="before")
    @classmethod
    def validate_category(cls, value: object) -> object:
        try:
            category = FactorCategory(value)
        except (TypeError, ValueError) as exc:
            raise PydanticCustomError(
                "unknown_factor_category",
                "unknown factor category '{category}'",
                {"category": value},
            ) from exc
        if category not in EXECUTABLE_FACTOR_CATEGORIES:
            raise PydanticCustomError(
                "unsupported_factor_category",
                "factor category '{category}' is recognized but not executable",
                {"category": category.value},
            )
        return category

    @field_validator("indicator", mode="before")
    @classmethod
    def validate_indicator(cls, value: object) -> object:
        try:
            return Indicator(value)
        except (TypeError, ValueError) as exc:
            raise PydanticCustomError(
                "unknown_indicator",
                "unknown indicator '{indicator}'",
                {"indicator": value},
            ) from exc

    @field_validator("source", mode="before")
    @classmethod
    def validate_source_shape(cls, value: object) -> object:
        if not isinstance(value, dict) or "field" not in value:
            raise PydanticCustomError(
                "unsupported_factor_source",
                "factor source must be exactly one raw field",
            )
        if set(value) - {"field", "lag", "shift"}:
            raise PydanticCustomError(
                "unsupported_factor_source",
                "factor source must be exactly one raw field",
            )
        return value

    @model_validator(mode="after")
    def validate_execution_contract(self) -> StrategyFactor:
        if self.category is FactorCategory.PRICE:
            if self.indicator is not Indicator.PRICE:
                raise PydanticCustomError(
                    "invalid_category_indicator_pair",
                    "price category requires price indicator",
                )
            if self.params:
                raise PydanticCustomError(
                    "invalid_factor_params",
                    "price indicator requires empty params",
                )
            return self

        if self.indicator is Indicator.PRICE:
            raise PydanticCustomError(
                "invalid_category_indicator_pair",
                "technical category cannot use price indicator",
            )
        if self.indicator in {Indicator.MACD, Indicator.BOLLINGER, Indicator.ATR}:
            raise PydanticCustomError(
                "unsupported_indicator_for_execution",
                "indicator '{indicator}' is recognized but not executable",
                {"indicator": self.indicator.value},
            )
        if set(self.params) != {"window"}:
            raise PydanticCustomError(
                "invalid_factor_params",
                "technical indicator params must contain exactly window",
            )
        window = self.params["window"]
        if isinstance(window, bool) or not isinstance(window, int) or window <= 0:
            raise PydanticCustomError(
                "invalid_factor_window",
                "factor window must be a positive integer",
            )
        return self


class StrategyCondition(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    type: ConditionType
    left: DataReference
    right: DataReference

    @model_validator(mode="before")
    @classmethod
    def validate_condition_shape(cls, value: object) -> object:
        if not isinstance(value, dict) or set(value) != {"type", "left", "right"}:
            raise PydanticCustomError(
                "unsupported_rule_shape",
                "each condition must contain exactly type, left, and right",
            )
        condition_type = value.get("type")
        if condition_type == ConditionType.EQUAL.value:
            raise PydanticCustomError(
                "unsupported_condition_type",
                "equal is recognized but not executable",
            )
        try:
            ConditionType(condition_type)
        except (TypeError, ValueError) as exc:
            raise PydanticCustomError(
                "unknown_condition_type",
                "unknown condition type '{condition_type}'",
                {"condition_type": condition_type},
            ) from exc
        return value

    @model_validator(mode="after")
    def validate_operands(self) -> StrategyCondition:
        if self.type in {ConditionType.CROSS_ABOVE, ConditionType.CROSS_BELOW}:
            if self.left.value is not None or self.right.value is not None:
                raise PydanticCustomError(
                    "invalid_condition_operand",
                    "cross conditions require two time-series operands",
                )
        elif self.left.value is not None and self.right.value is not None:
            raise PydanticCustomError(
                "invalid_condition_operand",
                "comparison requires at least one time-series operand",
            )
        return self


class StrategyRule(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    operator: LogicalOperator
    conditions: list[StrategyCondition] = Field(min_length=1)

    @model_validator(mode="before")
    @classmethod
    def validate_flat_shape(cls, value: object) -> object:
        if not isinstance(value, dict) or set(value) != {"operator", "conditions"}:
            raise PydanticCustomError(
                "unsupported_rule_shape",
                "rule must contain exactly operator and conditions",
            )
        operator = value.get("operator")
        try:
            LogicalOperator(operator)
        except (TypeError, ValueError) as exc:
            raise PydanticCustomError(
                "unknown_logical_operator",
                "unknown logical operator '{operator}'",
                {"operator": operator},
            ) from exc
        conditions = value.get("conditions")
        if not isinstance(conditions, list) or not conditions:
            raise PydanticCustomError(
                "unsupported_rule_shape",
                "rule conditions must be a nonempty flat list",
            )
        if any(
            not isinstance(condition, dict)
            or "operator" in condition
            or "conditions" in condition
            for condition in conditions
        ):
            raise PydanticCustomError(
                "unsupported_rule_shape",
                "nested rule groups are not executable",
            )
        return value


class StrategyUniverse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    market: Market
    symbols: list[str]

    @model_validator(mode="before")
    @classmethod
    def validate_universe_size(cls, value: object) -> object:
        if not isinstance(value, dict):
            return value
        symbols = value.get("symbols")
        if not isinstance(symbols, list) or len(symbols) != 1:
            raise PydanticCustomError(
                "unsupported_universe_size",
                "exactly one universe symbol is required",
            )
        return value

    @field_validator("symbols")
    @classmethod
    def normalize_symbols(cls, values: list[str]) -> list[str]:
        normalized = [symbol.strip().upper() for symbol in values]
        if any(not symbol for symbol in normalized):
            raise ValueError("universe symbols must not be blank")
        return normalized


class StrategyRisk(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    position_size_percent: Decimal
    stop_loss_percent: Decimal | None = None
    take_profit_percent: Decimal | None = None

    @model_validator(mode="before")
    @classmethod
    def reject_risk_controls(cls, value: object) -> object:
        if isinstance(value, dict) and (
            value.get("stop_loss_percent") is not None
            or value.get("take_profit_percent") is not None
        ):
            raise PydanticCustomError(
                "unsupported_risk_control",
                "stop-loss and take-profit are not executable in this MVP",
            )
        return value

    @field_validator(
        "position_size_percent",
        "stop_loss_percent",
        "take_profit_percent",
        mode="before",
    )
    @classmethod
    def reject_float_values(cls, value: object) -> object:
        return _require_decimal_string(value)

    @model_validator(mode="after")
    def validate_percentages(self) -> StrategyRisk:
        if self.position_size_percent <= 0 or self.position_size_percent > 100:
            raise ValueError("position_size_percent must be greater than 0 and at most 100")
        return self


class StrategyDefinition(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    name: str
    version: int = Field(ge=1)
    universe: StrategyUniverse
    factors: list[StrategyFactor] = Field(min_length=1)
    entry: StrategyRule
    exit: StrategyRule
    risk: StrategyRisk

    @field_validator("name")
    @classmethod
    def validate_name(cls, value: str) -> str:
        normalized = value.strip()
        if not normalized:
            raise ValueError("strategy name must not be blank")
        return normalized

    @model_validator(mode="after")
    def validate_factor_graph(self) -> StrategyDefinition:
        factor_ids = [factor.id for factor in self.factors]
        if len(factor_ids) != len(set(factor_ids)):
            raise PydanticCustomError(
                "duplicate_factor_id",
                "factor ids must be unique",
            )
        known_factors = set(factor_ids)
        for rule in (self.entry, self.exit):
            for condition in rule.conditions:
                for reference in (condition.left, condition.right):
                    if reference.factor is not None and reference.factor not in known_factors:
                        raise PydanticCustomError(
                            "unknown_factor_reference",
                            "condition references unknown factor '{factor}'",
                            {"factor": reference.factor},
                        )
        return self


def strategy_snapshot_json(strategy: StrategyDefinition) -> str:
    payload = strategy.model_dump(mode="json", exclude_none=True)
    payload["risk"]["stop_loss_percent"] = None
    payload["risk"]["take_profit_percent"] = None
    return json.dumps(payload, sort_keys=True, separators=(",", ":"), ensure_ascii=False)


class BacktestRunCreateRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True, populate_by_name=True)

    market: Market
    cost_profile: CostProfileId = Field(
        validation_alias=AliasChoices("costProfile", "cost_profile"),
        serialization_alias="costProfile",
    )
    symbol: str
    resolution: str = "daily"
    start_date: date = Field(
        validation_alias=AliasChoices("startDate", "start_date"),
        serialization_alias="startDate",
    )
    end_date: date = Field(
        validation_alias=AliasChoices("endDate", "end_date"),
        serialization_alias="endDate",
    )
    initial_cash: Decimal = Field(
        validation_alias=AliasChoices("initialCash", "initial_cash"),
        serialization_alias="initialCash",
    )
    currency: Currency | None = None
    strategy: StrategyDefinition = Field(
        validation_alias=AliasChoices("strategy", "strategyJson", "strategy_json")
    )

    @model_validator(mode="before")
    @classmethod
    def require_cost_profile(cls, value: object) -> object:
        if isinstance(value, dict) and not any(
            key in value for key in ("costProfile", "cost_profile")
        ):
            raise PydanticCustomError(
                "unknown_cost_profile",
                "costProfile is required",
            )
        return value

    @field_validator("cost_profile", mode="before")
    @classmethod
    def validate_cost_profile(cls, value: object, info: ValidationInfo) -> object:
        try:
            profile = resolve_cost_profile(value)  # type: ignore[arg-type]
            market = info.data.get("market")
            if isinstance(market, Market):
                profile = resolve_cost_profile(
                    profile.profile_id,
                    market=market.value,
                )
            return profile.profile_id
        except CostProfileSelectionError as exc:
            raise PydanticCustomError(exc.code, str(exc)) from exc

    @field_validator("initial_cash", mode="before")
    @classmethod
    def reject_float_cash(cls, value: object) -> object:
        return _require_decimal_string(value)

    @field_validator("resolution")
    @classmethod
    def validate_resolution(cls, value: str) -> str:
        if value != "daily":
            raise ValueError("only daily resolution is supported")
        return value

    @model_validator(mode="after")
    def apply_market_policy(self) -> BacktestRunCreateRequest:
        if self.start_date > self.end_date:
            raise PydanticCustomError(
                "invalid_date_range",
                "startDate must be on or before endDate",
            )
        if self.initial_cash <= 0:
            raise ValueError("initialCash must be greater than zero")

        symbol = self.symbol.strip().upper()
        if self.market is Market.KR:
            if re.fullmatch(r"\d{6}", symbol) is None:
                raise ValueError("KR symbols must contain exactly six digits")
        elif re.fullmatch(r"[A-Z][A-Z0-9.-]{0,9}", symbol) is None:
            raise ValueError("US symbols must be a valid uppercase ticker")

        expected_currency = CURRENCY_BY_MARKET[self.market]
        if self.currency is not None and self.currency is not expected_currency:
            raise ValueError(f"{self.market.value} market requires {expected_currency.value}")
        if self.strategy.universe.market is not self.market:
            raise ValueError("strategy universe market must match request market")
        if self.strategy.universe.symbols != [symbol]:
            raise PydanticCustomError(
                "unsupported_universe_size",
                "strategy universe must contain exactly the request symbol",
            )

        object.__setattr__(self, "symbol", symbol)
        object.__setattr__(self, "currency", expected_currency)
        return self


class BacktestSummary(BaseModel):
    model_config = ConfigDict(frozen=True, populate_by_name=True)

    total_return: Decimal = Field(serialization_alias="totalReturn")
    max_drawdown: Decimal = Field(serialization_alias="maxDrawdown")
    annualized_return: Decimal = Field(serialization_alias="annualizedReturn")
    sharpe: Decimal
    calmar: Decimal
    win_rate: Decimal = Field(serialization_alias="winRate")
    total_trades: int = Field(ge=0, serialization_alias="totalTrades")

    @field_validator(
        "total_return",
        "max_drawdown",
        "annualized_return",
        "sharpe",
        "calmar",
        "win_rate",
        mode="before",
    )
    @classmethod
    def reject_float_metrics(cls, value: object) -> object:
        return _require_decimal_input(value)


class BacktestRun(BaseModel):
    model_config = ConfigDict(frozen=True)

    run_id: UUID
    status: RunStatus
    market: Market
    symbol: str
    resolution: str
    start_date: date
    end_date: date
    initial_cash: Decimal
    currency: Currency
    cost_profile: CostProfileId
    strategy: StrategyDefinition
    artifact_path: str | None = None
    error_message: str | None = None
    summary: BacktestSummary | None = None
    created_at: datetime
    started_at: datetime | None = None
    finished_at: datetime | None = None
