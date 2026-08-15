from __future__ import annotations

from decimal import Decimal

import pytest

from src.backtest.lean_template.cost_profiles import (
    resolve_cost_profile,
    snapshot_cost_profile_json,
)
from src.backtest.lean_template.runtime import (
    RuntimeContractError,
    ValueFrame,
    evaluate_factor_series,
    evaluate_rule,
)

D = Decimal


def test_factor_and_rule_runtime_rejects_binary_floating_point_inputs() -> None:
    with pytest.raises(TypeError):
        evaluate_factor_series(
            factor_id="float_source",
            indicator="price",
            params={},
            source_values=[1.0],  # type: ignore[list-item]
        )
    with pytest.raises(TypeError):
        ValueFrame(
            fields={"close": 1.0},  # type: ignore[dict-item]
            factors={"sma": D(1)},
        )


@pytest.mark.parametrize(
    ("indicator", "expected"),
    [
        ("price", [D(1), D(2), D(3), D(4), D(5)]),
        ("returns", [None, None, None, D(3), D("1.5")]),
        ("sma", [None, None, D(2), D(3), D(4)]),
        ("ema", [None, None, D(2), D(3), D(4)]),
        (
            "wma",
            [None, None, D(14) / D(6), D(20) / D(6), D(26) / D(6)],
        ),
        ("rsi", [None, None, None, D(100), D(100)]),
        ("roc", [None, None, None, D(300), D(150)]),
        ("momentum", [None, None, None, D(3), D(3)]),
    ],
)
def test_scalar_factor_golden_vectors_and_first_ready_bar(
    indicator: str,
    expected: list[Decimal | None],
) -> None:
    params = {} if indicator == "price" else {"window": 3}

    actual = evaluate_factor_series(
        factor_id=f"factor_{indicator}",
        indicator=indicator,
        params=params,
        source_values=[D(1), D(2), D(3), D(4), D(5)],
    )

    assert actual == expected
    assert all(value is None for value in actual[: expected.index(next(v for v in expected if v is not None))])


def test_wilder_rsi_uses_seed_then_recursive_smoothing() -> None:
    actual = evaluate_factor_series(
        factor_id="rsi_2",
        indicator="rsi",
        params={"window": 2},
        source_values=[D(1), D(2), D(1), D(3), D(2)],
    )

    assert actual == [
        None,
        None,
        D(50),
        D(100) - D(100) / D(6),
        D(50),
    ]


def test_ema_uses_seed_then_recursive_smoothing_not_a_rolling_mean() -> None:
    actual = evaluate_factor_series(
        factor_id="ema_3",
        indicator="ema",
        params={"window": 3},
        source_values=[D(1), D(2), D(3), D(8), D(5)],
    )

    assert actual == [None, None, D(2), D(5), D(5)]


@pytest.mark.parametrize("indicator", ["returns", "roc"])
def test_ratio_factor_zero_denominator_has_stable_factor_context(
    indicator: str,
) -> None:
    with pytest.raises(RuntimeContractError) as raised:
        evaluate_factor_series(
            factor_id="zero_base",
            indicator=indicator,
            params={"window": 2},
            source_values=[D(0), D(1), D(2)],
        )

    assert raised.value.code == "factor_division_by_zero"
    assert raised.value.context == "zero_base"


def frame(
    *,
    close: str = "10",
    sma: str | None = "10",
) -> ValueFrame:
    return ValueFrame(
        fields={"close": D(close)},
        factors={"sma": None if sma is None else D(sma)},
    )


@pytest.mark.parametrize(
    ("condition_type", "previous", "current", "expected"),
    [
        ("cross_above", frame(close="10", sma="10"), frame(close="11", sma="10"), True),
        ("cross_above", frame(close="11", sma="10"), frame(close="12", sma="10"), False),
        ("cross_above", frame(close="10", sma="10"), frame(close="10", sma="10"), False),
        ("cross_below", frame(close="10", sma="10"), frame(close="9", sma="10"), True),
        ("cross_below", frame(close="9", sma="10"), frame(close="8", sma="10"), False),
        ("cross_below", frame(close="10", sma="10"), frame(close="10", sma="10"), False),
    ],
)
def test_cross_truth_table_uses_previous_and_current_strict_boundaries(
    condition_type: str,
    previous: ValueFrame,
    current: ValueFrame,
    expected: bool,
) -> None:
    rule = {
        "operator": "and",
        "conditions": [
            {
                "type": condition_type,
                "left": {"field": "close"},
                "right": {"factor": "sma"},
            }
        ],
    }

    assert evaluate_rule(rule, current=current, previous=previous) is expected


@pytest.mark.parametrize(
    ("condition_type", "close", "literal", "expected"),
    [
        ("greater_than", "11", "10", True),
        ("greater_than", "10", "10", False),
        ("less_than", "9", "10", True),
        ("less_than", "10", "10", False),
    ],
)
def test_comparison_truth_table_supports_decimal_literals(
    condition_type: str,
    close: str,
    literal: str,
    expected: bool,
) -> None:
    rule = {
        "operator": "and",
        "conditions": [
            {
                "type": condition_type,
                "left": {"field": "close"},
                "right": {"value": literal},
            }
        ],
    }

    assert evaluate_rule(rule, current=frame(close=close), previous=None) is expected


def test_flat_and_or_rules_and_unready_cross() -> None:
    conditions = [
        {
            "type": "greater_than",
            "left": {"field": "close"},
            "right": {"value": "5"},
        },
        {
            "type": "less_than",
            "left": {"field": "close"},
            "right": {"value": "20"},
        },
    ]
    assert evaluate_rule(
        {"operator": "and", "conditions": conditions},
        current=frame(close="10"),
        previous=None,
    ) is True
    false_conditions = [conditions[0], {**conditions[1], "right": {"value": "9"}}]
    assert evaluate_rule(
        {"operator": "or", "conditions": false_conditions},
        current=frame(close="10"),
        previous=None,
    ) is True
    cross = {
        "operator": "and",
        "conditions": [
            {
                "type": "cross_above",
                "left": {"field": "close"},
                "right": {"factor": "sma"},
            }
        ],
    }
    assert evaluate_rule(cross, current=frame(), previous=None) is None
    assert evaluate_rule(cross, current=frame(sma=None), previous=frame()) is None


@pytest.mark.parametrize(
    ("profile_id", "market", "expected_tax"),
    [
        ("KR_DEFAULT_V1", "KR", "18"),
        ("US_DEFAULT_V1", "US", "0"),
    ],
)
def test_resolved_cost_profiles_have_exact_decimal_strings(
    profile_id: str,
    market: str,
    expected_tax: str,
) -> None:
    profile = resolve_cost_profile(profile_id, market=market)

    assert profile.commission_bps_per_fill == "5"
    assert profile.slippage_bps_per_fill == "10"
    assert profile.sell_tax_bps == expected_tax


@pytest.mark.parametrize(
    ("profile_id", "expected"),
    [
        (
            "KR_DEFAULT_V1",
            (
                '{"commission_bps_per_fill":"5","market":"KR",'
                '"profile_id":"KR_DEFAULT_V1","sell_tax_bps":"18",'
                '"slippage_bps_per_fill":"10"}\n'
            ),
        ),
        (
            "US_DEFAULT_V1",
            (
                '{"commission_bps_per_fill":"5","market":"US",'
                '"profile_id":"US_DEFAULT_V1","sell_tax_bps":"0",'
                '"slippage_bps_per_fill":"10"}\n'
            ),
        ),
    ],
)
def test_cost_profile_snapshot_json_is_byte_deterministic(
    profile_id: str,
    expected: str,
) -> None:
    first = snapshot_cost_profile_json(resolve_cost_profile(profile_id))
    second = snapshot_cost_profile_json(resolve_cost_profile(profile_id))

    assert first == second
    assert first == expected
