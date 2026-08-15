from __future__ import annotations

from collections.abc import Mapping, Sequence
from dataclasses import dataclass, replace
from decimal import ROUND_FLOOR, Decimal
from typing import Any

try:
    from cost_profiles import CostProfile
except ImportError:  # Imported as part of the quant-worker package in unit tests.
    from src.backtest.lean_template.cost_profiles import CostProfile

ZERO = Decimal(0)
ONE = Decimal(1)
HUNDRED = Decimal(100)
BASIS_POINTS = Decimal(10000)
MOO_EXECUTION_POLICY_ID = "MOO_CLOSE_BUFFER_V1"
MOO_GAP_BUFFER_BPS = Decimal(500)
MOO_BUYING_POWER_REJECTED = "moo_buying_power_rejected"
MOO_UNFILLED_AT_END = "moo_unfilled_at_end"


class RuntimeContractError(ValueError):
    def __init__(self, code: str, context: str, message: str) -> None:
        super().__init__(message)
        self.code = code
        self.context = context


@dataclass(frozen=True)
class ValueFrame:
    fields: Mapping[str, Decimal]
    factors: Mapping[str, Decimal | None]

    def __post_init__(self) -> None:
        values = [*self.fields.values(), *self.factors.values()]
        if any(
            value is not None
            and (not isinstance(value, Decimal) or not value.is_finite())
            for value in values
        ):
            raise TypeError("runtime values must be finite Decimal instances")


@dataclass(frozen=True)
class DailyBar:
    date: str
    open: Decimal
    high: Decimal
    low: Decimal
    close: Decimal
    volume: Decimal

    def __post_init__(self) -> None:
        values = (self.open, self.high, self.low, self.close, self.volume)
        if any(
            not isinstance(value, Decimal) or not value.is_finite()
            for value in values
        ):
            raise TypeError("daily bar values must be finite Decimal instances")

    def fields(self) -> dict[str, Decimal]:
        return {
            "open": self.open,
            "high": self.high,
            "low": self.low,
            "close": self.close,
            "volume": self.volume,
        }


@dataclass(frozen=True)
class SignalDecision:
    ready: bool
    entry: bool
    exit: bool


@dataclass(frozen=True)
class Fill:
    side: str
    quantity: int
    base_price: Decimal
    fill_price: Decimal
    notional: Decimal
    commission: Decimal
    sell_tax: Decimal


@dataclass(frozen=True)
class TraceStep:
    date: str
    ready: bool
    entry_signal: bool
    exit_signal: bool
    scheduled_side: str | None
    submitted_quantity: int | None
    fill: Fill | None
    audit_codes: tuple[str, ...]
    ending_cash: Decimal
    ending_quantity: int
    equity_at_close: Decimal


@dataclass(frozen=True)
class _PendingOrder:
    side: str
    quantity: int


def evaluate_factor_series(
    *,
    factor_id: str,
    indicator: str,
    params: Mapping[str, Any],
    source_values: Sequence[Decimal],
) -> list[Decimal | None]:
    values = list(source_values)
    if any(
        not isinstance(value, Decimal) or not value.is_finite()
        for value in values
    ):
        raise TypeError("factor source values must be finite Decimal instances")
    if indicator == "price":
        return list(values)

    window = params.get("window")
    if isinstance(window, bool) or not isinstance(window, int) or window <= 0:
        raise RuntimeContractError(
            "invalid_factor_window",
            factor_id,
            f"invalid window for factor {factor_id}",
        )
    result: list[Decimal | None] = [None] * len(values)

    if indicator in {"returns", "roc", "momentum"}:
        for index in range(window, len(values)):
            current = values[index]
            previous = values[index - window]
            if indicator in {"returns", "roc"} and previous == ZERO:
                raise RuntimeContractError(
                    "factor_division_by_zero",
                    factor_id,
                    f"zero denominator for factor {factor_id}",
                )
            if indicator == "returns":
                result[index] = current / previous - ONE
            elif indicator == "roc":
                result[index] = (current / previous - ONE) * HUNDRED
            else:
                result[index] = current - previous
        return result

    if indicator == "sma":
        divisor = Decimal(str(window))
        for index in range(window - 1, len(values)):
            result[index] = sum(values[index - window + 1 : index + 1], ZERO) / divisor
        return result

    if indicator == "wma":
        denominator = Decimal(str(window * (window + 1) // 2))
        weights = [Decimal(str(weight)) for weight in range(1, window + 1)]
        for index in range(window - 1, len(values)):
            selected = values[index - window + 1 : index + 1]
            result[index] = sum(
                (value * weight for value, weight in zip(selected, weights, strict=True)),
                ZERO,
            ) / denominator
        return result

    if indicator == "ema":
        if len(values) < window:
            return result
        divisor = Decimal(str(window))
        alpha = Decimal(2) / Decimal(str(window + 1))
        ema = sum(values[:window], ZERO) / divisor
        result[window - 1] = ema
        for index in range(window, len(values)):
            ema = alpha * values[index] + (ONE - alpha) * ema
            result[index] = ema
        return result

    if indicator == "rsi":
        if len(values) <= window:
            return result
        divisor = Decimal(str(window))
        differences = [
            values[index] - values[index - 1]
            for index in range(1, len(values))
        ]
        seed = differences[:window]
        average_gain = sum((max(delta, ZERO) for delta in seed), ZERO) / divisor
        average_loss = sum((max(-delta, ZERO) for delta in seed), ZERO) / divisor
        result[window] = _rsi_value(average_gain, average_loss)
        multiplier = Decimal(str(window - 1))
        for source_index in range(window + 1, len(values)):
            delta = differences[source_index - 1]
            average_gain = (average_gain * multiplier + max(delta, ZERO)) / divisor
            average_loss = (average_loss * multiplier + max(-delta, ZERO)) / divisor
            result[source_index] = _rsi_value(average_gain, average_loss)
        return result

    raise RuntimeContractError(
        "unsupported_indicator_for_execution",
        factor_id,
        f"unsupported indicator for factor {factor_id}: {indicator}",
    )


def _rsi_value(average_gain: Decimal, average_loss: Decimal) -> Decimal:
    if average_loss == ZERO:
        return HUNDRED
    relative_strength = average_gain / average_loss
    return HUNDRED - HUNDRED / (ONE + relative_strength)


def evaluate_rule(
    rule: Mapping[str, Any],
    *,
    current: ValueFrame,
    previous: ValueFrame | None,
) -> bool | None:
    results: list[bool] = []
    for condition in rule["conditions"]:
        result = _evaluate_condition(condition, current=current, previous=previous)
        if result is None:
            return None
        results.append(result)
    if rule["operator"] == "and":
        return all(results)
    if rule["operator"] == "or":
        return any(results)
    raise RuntimeContractError(
        "unknown_logical_operator",
        "rule.operator",
        f"unknown logical operator: {rule['operator']}",
    )


def _evaluate_condition(
    condition: Mapping[str, Any],
    *,
    current: ValueFrame,
    previous: ValueFrame | None,
) -> bool | None:
    left = _operand_value(condition["left"], current)
    right = _operand_value(condition["right"], current)
    if left is None or right is None:
        return None
    condition_type = condition["type"]
    if condition_type == "greater_than":
        return left > right
    if condition_type == "less_than":
        return left < right
    if condition_type in {"cross_above", "cross_below"}:
        if previous is None:
            return None
        previous_left = _operand_value(condition["left"], previous)
        previous_right = _operand_value(condition["right"], previous)
        if previous_left is None or previous_right is None:
            return None
        if condition_type == "cross_above":
            return previous_left <= previous_right and left > right
        return previous_left >= previous_right and left < right
    raise RuntimeContractError(
        "unsupported_condition_type",
        "condition.type",
        f"unsupported condition type: {condition_type}",
    )


def _operand_value(operand: Mapping[str, Any], frame: ValueFrame) -> Decimal | None:
    if "field" in operand:
        return frame.fields.get(operand["field"])
    if "factor" in operand:
        return frame.factors.get(operand["factor"])
    if "value" in operand:
        return Decimal(operand["value"])
    return None


class StrategySignalEvaluator:
    def __init__(self, strategy: Mapping[str, Any]) -> None:
        self._strategy = strategy
        self._bars: list[DailyBar] = []
        self._previous: ValueFrame | None = None

    def update(self, bar: DailyBar) -> SignalDecision:
        self._bars.append(bar)
        factor_values: dict[str, Decimal | None] = {}
        for factor in self._strategy["factors"]:
            source_field = factor["source"]["field"]
            source_values = [item.fields()[source_field] for item in self._bars]
            series = evaluate_factor_series(
                factor_id=factor["id"],
                indicator=factor["indicator"],
                params=factor["params"],
                source_values=source_values,
            )
            factor_values[factor["id"]] = series[-1]
        current = ValueFrame(fields=bar.fields(), factors=factor_values)
        entry = evaluate_rule(
            self._strategy["entry"],
            current=current,
            previous=self._previous,
        )
        exit_signal = evaluate_rule(
            self._strategy["exit"],
            current=current,
            previous=self._previous,
        )
        self._previous = current
        ready = (
            all(value is not None for value in factor_values.values())
            and entry is not None
            and exit_signal is not None
        )
        return SignalDecision(
            ready=ready,
            entry=bool(entry) if ready else False,
            exit=bool(exit_signal) if ready else False,
        )


class ExecutionTraceEngine:
    def __init__(
        self,
        strategy: Mapping[str, Any],
        *,
        initial_cash: Decimal,
        cost_profile: CostProfile,
    ) -> None:
        if not isinstance(initial_cash, Decimal):
            raise TypeError("initial_cash must be Decimal")
        self._strategy = strategy
        self._signals = StrategySignalEvaluator(strategy)
        self._profile = cost_profile
        self._cash = initial_cash
        self._quantity = 0
        self._pending: _PendingOrder | None = None
        self._entry_armed = True

    def run(self, bars: Sequence[DailyBar]) -> list[TraceStep]:
        trace = [self.process_bar(bar) for bar in bars]
        if trace and self._pending is not None:
            trace[-1] = replace(
                trace[-1],
                audit_codes=(*trace[-1].audit_codes, MOO_UNFILLED_AT_END),
            )
            self._pending = None
        return trace

    def process_bar(self, bar: DailyBar) -> TraceStep:
        fill, audit_codes = self._fill_pending(bar.open)
        decision = self._signals.update(bar)
        scheduled_side: str | None = None
        submitted_quantity: int | None = None
        if decision.ready:
            if not decision.entry:
                self._entry_armed = True
            if decision.exit:
                if self._quantity > 0:
                    self._pending = _PendingOrder(
                        side="SELL",
                        quantity=self._quantity,
                    )
                    scheduled_side = "SELL"
                    submitted_quantity = -self._quantity
            elif decision.entry and self._quantity == 0 and self._entry_armed:
                position_percent = Decimal(
                    self._strategy["risk"]["position_size_percent"]
                )
                portfolio_value = self._cash + Decimal(str(self._quantity)) * bar.close
                quantity = calculate_moo_buy_quantity(
                    self._profile,
                    signal_close_price=bar.close,
                    target_value=portfolio_value * position_percent / HUNDRED,
                    available_cash=self._cash,
                )
                if quantity > 0:
                    self._pending = _PendingOrder(side="BUY", quantity=quantity)
                    scheduled_side = "BUY"
                    submitted_quantity = quantity
        equity = self._cash + Decimal(str(self._quantity)) * bar.close
        return TraceStep(
            date=bar.date,
            ready=decision.ready,
            entry_signal=decision.entry,
            exit_signal=decision.exit,
            scheduled_side=scheduled_side,
            submitted_quantity=submitted_quantity,
            fill=fill,
            audit_codes=audit_codes,
            ending_cash=self._cash,
            ending_quantity=self._quantity,
            equity_at_close=equity,
        )

    def _fill_pending(
        self,
        base_price: Decimal,
    ) -> tuple[Fill | None, tuple[str, ...]]:
        pending = self._pending
        self._pending = None
        if pending is None:
            return None, ()
        if pending.side == "BUY":
            fill = calculate_fill(
                self._profile,
                "BUY",
                pending.quantity,
                base_price,
            )
            required_cash = fill.notional + fill.commission
            if required_cash > self._cash:
                self._entry_armed = False
                return None, (MOO_BUYING_POWER_REJECTED,)
            self._cash -= fill.notional + fill.commission
            self._quantity += pending.quantity
            return fill, ()
        quantity = pending.quantity
        if quantity <= 0:
            return None, ()
        fill = calculate_fill(
            self._profile,
            "SELL",
            quantity,
            base_price,
        )
        self._cash += fill.notional - fill.commission - fill.sell_tax
        self._quantity -= quantity
        return fill, ()



def adverse_fill_price(
    profile: CostProfile,
    side: str,
    base_price: Decimal,
) -> Decimal:
    slippage_rate = Decimal(profile.slippage_bps_per_fill) / BASIS_POINTS
    if side == "BUY":
        return base_price * (ONE + slippage_rate)
    if side == "SELL":
        return base_price * (ONE - slippage_rate)
    raise ValueError(f"unknown order side: {side}")


def calculate_buy_quantity(
    profile: CostProfile,
    fill_price: Decimal,
    *,
    target_value: Decimal,
    available_cash: Decimal,
) -> int:
    commission_rate = Decimal(profile.commission_bps_per_fill) / BASIS_POINTS
    target_limit = (target_value / fill_price).to_integral_value(
        rounding=ROUND_FLOOR
    )
    cash_limit = (
        available_cash / (fill_price * (ONE + commission_rate))
    ).to_integral_value(rounding=ROUND_FLOOR)
    return max(0, int(min(target_limit, cash_limit)))


def calculate_moo_buy_quantity(
    profile: CostProfile,
    *,
    signal_close_price: Decimal,
    target_value: Decimal,
    available_cash: Decimal,
    gap_buffer_bps: Decimal = MOO_GAP_BUFFER_BPS,
) -> int:
    gap_reference_price = signal_close_price * (
        ONE + gap_buffer_bps / BASIS_POINTS
    )
    reference_fill_price = adverse_fill_price(
        profile,
        "BUY",
        gap_reference_price,
    )
    return calculate_buy_quantity(
        profile,
        reference_fill_price,
        target_value=target_value,
        available_cash=available_cash,
    )


def calculate_fill(
    profile: CostProfile,
    side: str,
    quantity: int,
    base_price: Decimal,
) -> Fill:
    fill_price = adverse_fill_price(profile, side, base_price)
    notional = fill_price * Decimal(str(quantity))
    commission = (
        notional * Decimal(profile.commission_bps_per_fill) / BASIS_POINTS
    )
    sell_tax = ZERO
    if side == "SELL":
        sell_tax = notional * Decimal(profile.sell_tax_bps) / BASIS_POINTS
    return Fill(
        side=side,
        quantity=quantity,
        base_price=base_price,
        fill_price=fill_price,
        notional=notional,
        commission=commission,
        sell_tax=sell_tax,
    )
