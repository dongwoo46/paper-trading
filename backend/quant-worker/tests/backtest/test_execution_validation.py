from __future__ import annotations

from copy import deepcopy

import pytest
from pydantic import ValidationError

from src.backtest.domain import BacktestRunCreateRequest, StrategyDefinition

from .test_domain import run_request_payload, strategy_payload


def validation_error(payload: dict[str, object]) -> ValidationError:
    with pytest.raises(ValidationError) as raised:
        StrategyDefinition.model_validate(payload)
    return raised.value


def assert_error(
    payload: dict[str, object],
    code: str,
    path_prefix: tuple[object, ...],
) -> None:
    errors = validation_error(payload).errors()
    assert any(
        error["type"] == code and error["loc"][: len(path_prefix)] == path_prefix
        for error in errors
    ), errors


@pytest.mark.parametrize(
    "indicator",
    ["returns", "sma", "ema", "wma", "rsi", "roc", "momentum"],
)
def test_accepts_every_scalar_technical_indicator(indicator: str) -> None:
    payload = strategy_payload()
    factor = payload["factors"][1]  # type: ignore[index]
    factor["indicator"] = indicator
    factor["params"] = {"window": 2}

    strategy = StrategyDefinition.model_validate(payload)

    assert strategy.factors[1].indicator.value == indicator


@pytest.mark.parametrize("field", ["open", "high", "low", "close", "volume"])
def test_accepts_every_raw_factor_source(field: str) -> None:
    payload = strategy_payload()
    payload["factors"][0]["source"] = {"field": field, "lag": 0, "shift": 0}  # type: ignore[index]

    strategy = StrategyDefinition.model_validate(payload)

    assert strategy.factors[0].source.field.value == field  # type: ignore[union-attr]


@pytest.mark.parametrize("indicator", ["macd", "bollinger", "atr"])
def test_recognized_complex_indicator_is_stably_rejected(indicator: str) -> None:
    payload = strategy_payload()
    payload["factors"][1]["indicator"] = indicator  # type: ignore[index]

    assert_error(
        payload,
        "unsupported_indicator_for_execution",
        ("factors", 1),
    )


@pytest.mark.parametrize(
    ("category", "indicator"),
    [("price", "sma"), ("technical", "price")],
)
def test_invalid_category_indicator_pair_is_stably_rejected(
    category: str,
    indicator: str,
) -> None:
    payload = strategy_payload()
    factor = payload["factors"][0]  # type: ignore[index]
    factor["category"] = category
    factor["indicator"] = indicator

    assert_error(payload, "invalid_category_indicator_pair", ("factors", 0))


@pytest.mark.parametrize(
    ("params", "code"),
    [
        ({}, "invalid_factor_params"),
        ({"window": 2, "offset": 1}, "invalid_factor_params"),
        ({"window": 0}, "invalid_factor_window"),
        ({"window": True}, "invalid_factor_window"),
        ({"window": "2"}, "invalid_factor_window"),
    ],
)
def test_indicator_parameter_contract_is_exact(
    params: dict[str, object],
    code: str,
) -> None:
    payload = strategy_payload()
    payload["factors"][1]["params"] = params  # type: ignore[index]

    assert_error(payload, code, ("factors", 1))


def test_price_factor_requires_empty_params() -> None:
    payload = strategy_payload()
    payload["factors"][0]["params"] = {"window": 1}  # type: ignore[index]

    assert_error(payload, "invalid_factor_params", ("factors", 0))


@pytest.mark.parametrize(
    ("source", "code"),
    [
        ({"field": "adjusted_close"}, "unsupported_price_field"),
        ({"field": "vwap"}, "unknown_price_field"),
        ({"factor": "close_price"}, "unsupported_factor_source"),
        ({"value": "10"}, "unsupported_factor_source"),
        ({"field": "close", "lag": 1}, "unsupported_reference_lag"),
        ({"field": "close", "shift": 1}, "look_ahead_reference"),
    ],
)
def test_factor_source_contract_has_stable_codes(
    source: dict[str, object],
    code: str,
) -> None:
    payload = strategy_payload()
    payload["factors"][0]["source"] = source  # type: ignore[index]

    assert_error(payload, code, ("factors", 0))


def test_rule_operand_accepts_decimal_string_literal() -> None:
    payload = strategy_payload()
    payload["entry"]["conditions"] = [  # type: ignore[index]
        {
            "type": "greater_than",
            "left": {"factor": "sma_20"},
            "right": {"value": "70.25"},
        }
    ]

    strategy = StrategyDefinition.model_validate(payload)

    assert strategy.entry.conditions[0].right.value == "70.25"


@pytest.mark.parametrize(
    ("mutator", "code", "path"),
    [
        (
            lambda payload: payload["entry"]["conditions"][0].update(type="equal"),  # type: ignore[index]
            "unsupported_condition_type",
            ("entry", "conditions", 0),
        ),
        (
            lambda payload: payload["entry"]["conditions"][0].update(  # type: ignore[index]
                right={"value": "10"}
            ),
            "invalid_condition_operand",
            ("entry", "conditions", 0),
        ),
        (
            lambda payload: payload["entry"]["conditions"][0].update(  # type: ignore[index]
                type="greater_than",
                left={"value": "20"},
                right={"value": "10"},
            ),
            "invalid_condition_operand",
            ("entry", "conditions", 0),
        ),
        (
            lambda payload: payload["entry"]["conditions"][0].update(  # type: ignore[index]
                type="greater_than", right={"value": 10.0}
            ),
            "decimal_string_required",
            ("entry", "conditions", 0),
        ),
        (
            lambda payload: payload["entry"].update(conditions=[]),  # type: ignore[index]
            "unsupported_rule_shape",
            ("entry",),
        ),
        (
            lambda payload: payload["entry"].update(operator="xor"),  # type: ignore[index]
            "unknown_logical_operator",
            ("entry",),
        ),
        (
            lambda payload: payload["entry"]["conditions"][0].update(type="near"),  # type: ignore[index]
            "unknown_condition_type",
            ("entry", "conditions", 0),
        ),
    ],
)
def test_rule_rejections_use_exact_stable_code_and_path(
    mutator,
    code: str,
    path: tuple[object, ...],
) -> None:
    payload = strategy_payload()
    mutator(payload)

    assert_error(payload, code, path)


def test_rule_rejects_nested_group() -> None:
    payload = strategy_payload()
    payload["entry"]["conditions"] = [deepcopy(payload["exit"])]  # type: ignore[index]

    assert_error(payload, "unsupported_rule_shape", ("entry",))


def test_nonzero_operand_lag_and_shift_have_stable_codes() -> None:
    lagged = strategy_payload()
    lagged["entry"]["conditions"][0]["left"] = {"field": "close", "lag": 1}  # type: ignore[index]
    shifted = strategy_payload()
    shifted["entry"]["conditions"][0]["left"] = {"field": "close", "shift": -1}  # type: ignore[index]

    assert_error(lagged, "unsupported_reference_lag", ("entry", "conditions", 0, "left"))
    assert_error(shifted, "look_ahead_reference", ("entry", "conditions", 0, "left"))


@pytest.mark.parametrize("risk_field", ["stop_loss_percent", "take_profit_percent"])
def test_non_null_risk_controls_are_stably_rejected(risk_field: str) -> None:
    payload = strategy_payload()
    payload["risk"][risk_field] = "5"  # type: ignore[index]

    assert_error(payload, "unsupported_risk_control", ("risk",))


@pytest.mark.parametrize("symbols", [[], ["005930", "000660"]])
def test_strategy_requires_exactly_one_symbol(symbols: list[str]) -> None:
    payload = strategy_payload()
    payload["universe"]["symbols"] = symbols  # type: ignore[index]

    assert_error(payload, "unsupported_universe_size", ("universe",))


def test_request_symbol_must_be_the_only_universe_symbol() -> None:
    payload = run_request_payload()
    payload["strategy"]["universe"]["symbols"] = ["000660"]  # type: ignore[index]

    with pytest.raises(ValidationError) as raised:
        BacktestRunCreateRequest.model_validate(payload)

    assert "unsupported_universe_size" in {
        str(error["type"]) for error in raised.value.errors()
    }


def test_duplicate_factor_id_and_unknown_factor_reference_have_stable_context() -> None:
    duplicate = strategy_payload()
    duplicate["factors"][1]["id"] = "close_price"  # type: ignore[index]
    unknown = strategy_payload()
    unknown["entry"]["conditions"][0]["right"] = {"factor": "missing"}  # type: ignore[index]

    assert_error(duplicate, "duplicate_factor_id", ())
    errors = validation_error(unknown).errors()
    assert any(
        error["type"] == "unknown_factor_reference"
        and error.get("ctx", {}).get("factor") == "missing"
        for error in errors
    )


def test_malformed_operand_and_unknown_rule_field_have_stable_codes() -> None:
    malformed = strategy_payload()
    malformed["entry"]["conditions"][0]["left"] = {  # type: ignore[index]
        "field": "close",
        "factor": "sma_20",
    }
    unknown_field = strategy_payload()
    unknown_field["entry"]["conditions"][0]["left"] = {"field": "vwap"}  # type: ignore[index]

    assert_error(
        malformed,
        "invalid_condition_operand",
        ("entry", "conditions", 0, "left"),
    )
    assert_error(
        unknown_field,
        "unknown_price_field",
        ("entry", "conditions", 0, "left"),
    )


@pytest.mark.parametrize("invalid_value", [100, 100.0, "NaN", "not-a-decimal"])
def test_position_size_requires_a_finite_decimal_string(invalid_value: object) -> None:
    payload = strategy_payload()
    payload["risk"]["position_size_percent"] = invalid_value  # type: ignore[index]

    assert_error(payload, "decimal_string_required", ("risk",))


@pytest.mark.parametrize("invalid_value", [100_000, 100_000.0, "NaN", "invalid"])
def test_initial_cash_requires_a_finite_decimal_string(invalid_value: object) -> None:
    payload = run_request_payload()
    payload["initialCash"] = invalid_value

    with pytest.raises(ValidationError) as raised:
        BacktestRunCreateRequest.model_validate(payload)

    assert any(
        error["type"] == "decimal_string_required"
        and error["loc"] == ("initialCash",)
        for error in raised.value.errors()
    )
