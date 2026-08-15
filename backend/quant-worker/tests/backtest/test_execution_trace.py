from __future__ import annotations

from decimal import Decimal

from src.backtest.lean_template.cost_profiles import resolve_cost_profile
from src.backtest.lean_template.runtime import (
    BASIS_POINTS,
    DailyBar,
    ExecutionTraceEngine,
    adverse_fill_price,
    calculate_buy_quantity,
    calculate_fill,
    calculate_moo_buy_quantity,
)

D = Decimal


def bar(day: int, *, open_price: str, close: str) -> DailyBar:
    close_value = D(close)
    return DailyBar(
        date=f"2024-01-{day:02d}",
        open=D(open_price),
        high=max(D(open_price), close_value),
        low=min(D(open_price), close_value),
        close=close_value,
        volume=D(1000),
    )


def strategy(
    *,
    entry_threshold: str = "10",
    exit_threshold: str = "8",
    warmup_exit: bool = False,
) -> dict[str, object]:
    factors: list[dict[str, object]] = [
        {
            "id": "close_price",
            "category": "price",
            "indicator": "price",
            "params": {},
            "source": {"field": "close"},
        }
    ]
    exit_left: dict[str, str] = {"field": "close"}
    if warmup_exit:
        factors.append(
            {
                "id": "sma_2",
                "category": "technical",
                "indicator": "sma",
                "params": {"window": 2},
                "source": {"field": "close"},
            }
        )
        exit_left = {"factor": "sma_2"}
    return {
        "factors": factors,
        "entry": {
            "operator": "and",
            "conditions": [
                {
                    "type": "greater_than",
                    "left": {"field": "close"},
                    "right": {"value": entry_threshold},
                }
            ],
        },
        "exit": {
            "operator": "and",
            "conditions": [
                {
                    "type": "less_than",
                    "left": exit_left,
                    "right": {"value": exit_threshold},
                }
            ],
        },
        "risk": {"position_size_percent": "100"},
    }


def engine(
    strategy_definition: dict[str, object] | None = None,
    *,
    market: str = "US",
    cash: str = "100",
) -> ExecutionTraceEngine:
    profile_id = "US_DEFAULT_V1" if market == "US" else "KR_DEFAULT_V1"
    return ExecutionTraceEngine(
        strategy_definition or strategy(),
        initial_cash=D(cash),
        cost_profile=resolve_cost_profile(profile_id, market=market),
    )


def test_moo_signal_close_sizing_matches_decimal_golden_vector() -> None:
    profile = resolve_cost_profile("US_DEFAULT_V1", market="US")
    gap_reference_price = D("20") * (D(1) + D("500") / BASIS_POINTS)
    reference_fill_price = adverse_fill_price(
        profile,
        "BUY",
        gap_reference_price,
    )

    assert gap_reference_price == D("21.00")
    assert reference_fill_price == D("21.02100")
    assert calculate_moo_buy_quantity(
        profile,
        signal_close_price=D("20"),
        target_value=D("100"),
        available_cash=D("100"),
    ) == 4


def test_buy_quantity_applies_each_floor_cap_independently() -> None:
    profile = resolve_cost_profile("US_DEFAULT_V1", market="US")
    reference_fill_price = D("21.02100")
    commission_multiplier = D("1.0005")
    one_share_with_commission = reference_fill_price * commission_multiplier

    assert calculate_buy_quantity(
        profile,
        reference_fill_price,
        target_value=D("100"),
        available_cash=D("1000"),
    ) == 4
    assert calculate_buy_quantity(
        profile,
        reference_fill_price,
        target_value=D("1000"),
        available_cash=D("100"),
    ) == 4
    assert calculate_buy_quantity(
        profile,
        reference_fill_price,
        target_value=reference_fill_price * 5 - D("0.00001"),
        available_cash=D("1000"),
    ) == 4
    assert calculate_buy_quantity(
        profile,
        reference_fill_price,
        target_value=reference_fill_price * 5,
        available_cash=D("1000"),
    ) == 5
    assert calculate_buy_quantity(
        profile,
        reference_fill_price,
        target_value=D("1000"),
        available_cash=one_share_with_commission * 5 - D("0.00001"),
    ) == 4
    assert calculate_buy_quantity(
        profile,
        reference_fill_price,
        target_value=D("1000"),
        available_cash=one_share_with_commission * 5,
    ) == 5


def test_whole_strategy_warmup_suppresses_both_rules() -> None:
    trace = engine(strategy(warmup_exit=True)).run(
        [
            bar(1, open_price="9", close="11"),
            bar(2, open_price="10", close="12"),
            bar(3, open_price="10", close="12"),
        ]
    )

    assert trace[0].ready is False
    assert trace[0].entry_signal is False
    assert trace[0].scheduled_side is None
    assert trace[1].ready is True
    assert trace[1].scheduled_side == "BUY"
    assert trace[1].fill is None
    assert trace[2].fill is not None
    assert trace[2].fill.side == "BUY"


def test_unused_declared_factor_still_participates_in_whole_strategy_warmup() -> None:
    definition = strategy()
    definition["factors"].append(  # type: ignore[union-attr]
        {
            "id": "unused_sma_2",
            "category": "technical",
            "indicator": "sma",
            "params": {"window": 2},
            "source": {"field": "close"},
        }
    )

    trace = engine(definition).run(
        [
            bar(1, open_price="9", close="11"),
            bar(2, open_price="10", close="12"),
        ]
    )

    assert trace[0].ready is False
    assert trace[0].scheduled_side is None
    assert trace[1].ready is True
    assert trace[1].scheduled_side == "BUY"


def test_cross_requires_a_ready_previous_pair_after_factor_warmup() -> None:
    definition = strategy(warmup_exit=True)
    definition["entry"] = {
        "operator": "and",
        "conditions": [
            {
                "type": "cross_above",
                "left": {"field": "close"},
                "right": {"factor": "sma_2"},
            }
        ],
    }

    trace = engine(definition).run(
        [
            bar(1, open_price="8", close="10"),
            bar(2, open_price="8", close="8"),
            bar(3, open_price="12", close="12"),
        ]
    )

    assert [step.ready for step in trace] == [False, False, True]
    assert trace[2].entry_signal is True
    assert trace[2].fill is None


def test_signal_on_completed_bar_fills_only_at_next_open_with_whole_shares() -> None:
    trace = engine().run(
        [
            bar(1, open_price="50", close="20"),
            bar(2, open_price="10", close="12"),
        ]
    )

    assert trace[0].scheduled_side == "BUY"
    assert trace[0].submitted_quantity == 4
    assert trace[0].fill is None
    fill = trace[1].fill
    assert fill is not None
    assert fill.base_price == D(10)
    assert fill.fill_price == D("10.010")
    assert fill.quantity == 4
    assert fill.commission == D("0.0200200")
    assert trace[1].ending_quantity == 4
    assert trace[1].ending_cash == D("59.9399800")


def test_submitted_moo_quantity_never_depends_on_future_open() -> None:
    affordable = engine().run(
        [
            bar(1, open_price="50", close="20"),
            bar(2, open_price="10", close="12"),
        ]
    )
    gapped = engine().run(
        [
            bar(1, open_price="50", close="20"),
            bar(2, open_price="25", close="12"),
        ]
    )

    assert affordable[0].submitted_quantity == 4
    assert gapped[0].submitted_quantity == 4


def test_unaffordable_moo_rejects_atomically_and_rearms_after_false_then_true() -> None:
    expected_rejected_fill = calculate_fill(
        resolve_cost_profile("US_DEFAULT_V1", market="US"),
        "BUY",
        4,
        D("25"),
    )
    trace = engine().run(
        [
            bar(1, open_price="20", close="20"),
            bar(2, open_price="25", close="20"),
            bar(3, open_price="20", close="20"),
            bar(4, open_price="20", close="9"),
            bar(5, open_price="20", close="20"),
        ]
    )

    assert trace[0].submitted_quantity == 4
    assert expected_rejected_fill.fill_price == D("25.025")
    assert expected_rejected_fill.notional + expected_rejected_fill.commission == D(
        "100.1500500"
    )
    assert trace[1].fill is None
    assert trace[1].audit_codes == ("moo_buying_power_rejected",)
    assert trace[1].ending_cash == D("100")
    assert trace[1].ending_quantity == 0
    assert trace[1].scheduled_side is None
    assert trace[2].scheduled_side is None
    assert trace[3].scheduled_side is None
    assert trace[4].scheduled_side == "BUY"
    assert trace[4].submitted_quantity == 4


def test_repeated_entry_does_not_pyramid_or_rebalance() -> None:
    trace = engine().run(
        [
            bar(1, open_price="10", close="11"),
            bar(2, open_price="10", close="12"),
            bar(3, open_price="10", close="13"),
        ]
    )

    assert [step.fill.side if step.fill else None for step in trace] == [None, "BUY", None]
    assert trace[2].ending_quantity == trace[1].ending_quantity
    assert trace[2].scheduled_side is None


def test_exit_precedence_keeps_flat_state_flat_when_both_rules_true() -> None:
    both_true = strategy(entry_threshold="10", exit_threshold="20")

    trace = engine(both_true).run([bar(1, open_price="10", close="11")])

    assert trace[0].entry_signal is True
    assert trace[0].exit_signal is True
    assert trace[0].scheduled_side is None


def test_exit_precedence_liquidates_full_invested_position() -> None:
    definition = strategy(entry_threshold="10", exit_threshold="8")
    definition["exit"] = {
        "operator": "and",
        "conditions": [
            {
                "type": "greater_than",
                "left": {"field": "close"},
                "right": {"value": "20"},
            }
        ],
    }
    trace = engine(definition).run(
        [
            bar(1, open_price="10", close="11"),
            bar(2, open_price="10", close="25"),
            bar(3, open_price="20", close="20"),
        ]
    )

    assert trace[1].ending_quantity == 8
    assert trace[1].entry_signal is True
    assert trace[1].exit_signal is True
    assert trace[1].scheduled_side == "SELL"
    assert trace[1].submitted_quantity == -8
    assert trace[2].fill is not None
    assert trace[2].fill.side == "SELL"
    assert trace[2].fill.quantity == 8
    assert trace[2].ending_quantity == 0


def test_signal_on_last_bar_has_no_fill() -> None:
    trace = engine().run([bar(1, open_price="50", close="11")])

    assert trace[0].scheduled_side == "BUY"
    assert trace[0].fill is None
    assert trace[0].ending_quantity == 0
    assert trace[0].audit_codes == ("moo_unfilled_at_end",)


def test_kr_costs_apply_adverse_slippage_commission_and_sell_tax() -> None:
    definition = strategy(entry_threshold="10", exit_threshold="8")
    trace_engine = engine(definition, market="KR", cash="1000")
    trace = trace_engine.run(
        [
            bar(1, open_price="10", close="11"),
            bar(2, open_price="10", close="7"),
            bar(3, open_price="12", close="12"),
        ]
    )

    buy = trace[1].fill
    sell = trace[2].fill
    assert buy is not None and sell is not None
    assert buy.fill_price == D("10.010")
    assert buy.commission == buy.notional * D("5") / D(10000)
    assert buy.sell_tax == D(0)
    assert sell.fill_price == D("11.988")
    assert sell.commission == sell.notional * D("5") / D(10000)
    assert sell.sell_tax == sell.notional * D("18") / D(10000)
    assert trace[2].ending_cash == (
        D(1000)
        - buy.notional
        - buy.commission
        + sell.notional
        - sell.commission
        - sell.sell_tax
    )


def test_us_sell_tax_is_zero() -> None:
    definition = strategy(entry_threshold="10", exit_threshold="8")
    trace = engine(definition, cash="1000").run(
        [
            bar(1, open_price="10", close="11"),
            bar(2, open_price="10", close="7"),
            bar(3, open_price="12", close="12"),
        ]
    )

    assert trace[2].fill is not None
    assert trace[2].fill.sell_tax == D(0)
