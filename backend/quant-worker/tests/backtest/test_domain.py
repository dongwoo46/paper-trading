from __future__ import annotations

import json
from decimal import Decimal

import pytest
from pydantic import ValidationError

from src.backtest.domain import (
    BacktestRunCreateRequest,
    BacktestSummary,
    Currency,
    Market,
    StrategyDefinition,
    strategy_snapshot_json,
)


def strategy_payload(*, market: str = "KR") -> dict[str, object]:
    symbol = "005930" if market == "KR" else "AAPL"
    return {
        "name": "price and technical strategy",
        "version": 1,
        "universe": {"market": market, "symbols": [symbol]},
        "factors": [
            {
                "id": "close_price",
                "category": "price",
                "indicator": "price",
                "params": {},
                "source": {"field": "close"},
            },
            {
                "id": "sma_20",
                "category": "technical",
                "indicator": "sma",
                "params": {"window": 20},
                "source": {"field": "close"},
            },
        ],
        "entry": {
            "operator": "and",
            "conditions": [
                {
                    "type": "cross_above",
                    "left": {"field": "close"},
                    "right": {"factor": "sma_20"},
                }
            ],
        },
        "exit": {
            "operator": "or",
            "conditions": [
                {
                    "type": "cross_below",
                    "left": {"field": "close"},
                    "right": {"factor": "sma_20"},
                }
            ],
        },
        "risk": {
            "position_size_percent": "100",
            "stop_loss_percent": None,
            "take_profit_percent": None,
        },
    }


def run_request_payload(*, market: str = "KR") -> dict[str, object]:
    return {
        "market": market,
        "costProfile": "KR_DEFAULT_V1" if market == "KR" else "US_DEFAULT_V1",
        "symbol": "005930" if market == "KR" else "aapl",
        "resolution": "daily",
        "startDate": "2024-01-01",
        "endDate": "2024-12-31",
        "initialCash": "100000000" if market == "KR" else "100000",
        "strategy": strategy_payload(market=market),
    }


def error_types(exc: ValidationError) -> set[str]:
    return {str(error["type"]) for error in exc.errors()}


def test_dsl_accepts_price_and_technical_factors() -> None:
    strategy = StrategyDefinition.model_validate(strategy_payload())

    assert [factor.category.value for factor in strategy.factors] == ["price", "technical"]
    assert strategy.factors[1].params == {"window": 20}


@pytest.mark.parametrize(
    ("path", "invalid_value"),
    [
        (("factors", 0, "category"), "astrology"),
        (("factors", 1, "indicator"), "oracle"),
        (("entry", "operator"), "xor"),
        (("universe", "market"), "EU"),
    ],
)
def test_dsl_rejects_unknown_vocabulary(path: tuple[object, ...], invalid_value: str) -> None:
    payload = strategy_payload()
    target: object = payload
    for segment in path[:-1]:
        target = target[segment]  # type: ignore[index]
    target[path[-1]] = invalid_value  # type: ignore[index]

    with pytest.raises(ValidationError):
        StrategyDefinition.model_validate(payload)


@pytest.mark.parametrize(
    "category",
    ["flow", "fundamental", "macro", "news_sentiment", "disclosure", "paper_factor", "event"],
)
def test_dsl_rejects_recognized_planned_factor_with_stable_error_code(category: str) -> None:
    payload = strategy_payload()
    payload["factors"][0]["category"] = category  # type: ignore[index]

    with pytest.raises(ValidationError) as raised:
        StrategyDefinition.model_validate(payload)

    assert "unsupported_factor_category" in error_types(raised.value)


@pytest.mark.parametrize(
    ("future_reference", "expected_code"),
    [
        ({"field": "close", "lag": -1}, "unsupported_reference_lag"),
        ({"field": "close", "shift": 1}, "look_ahead_reference"),
    ],
)
def test_dsl_rejects_future_references_with_stable_error_code(
    future_reference: dict[str, object],
    expected_code: str,
) -> None:
    payload = strategy_payload()
    payload["entry"]["conditions"][0]["left"] = future_reference  # type: ignore[index]

    with pytest.raises(ValidationError) as raised:
        StrategyDefinition.model_validate(payload)

    assert expected_code in error_types(raised.value)


def test_dsl_rejects_negative_indicator_window() -> None:
    payload = strategy_payload()
    payload["factors"][1]["params"]["window"] = -1  # type: ignore[index]

    with pytest.raises(ValidationError) as raised:
        StrategyDefinition.model_validate(payload)

    assert "invalid_factor_window" in error_types(raised.value)


def test_kr_and_us_requests_derive_currency_and_normalize_symbol() -> None:
    kr_request = BacktestRunCreateRequest.model_validate(run_request_payload(market="KR"))
    us_request = BacktestRunCreateRequest.model_validate(run_request_payload(market="US"))

    assert (kr_request.market, kr_request.currency, kr_request.symbol) == (
        Market.KR,
        Currency.KRW,
        "005930",
    )
    assert (us_request.market, us_request.currency, us_request.symbol) == (
        Market.US,
        Currency.USD,
        "AAPL",
    )
    assert kr_request.cost_profile.value == "KR_DEFAULT_V1"
    assert us_request.cost_profile.value == "US_DEFAULT_V1"


@pytest.mark.parametrize(
    ("market", "cost_profile", "expected_code"),
    [
        ("KR", "UNKNOWN", "unknown_cost_profile"),
        ("KR", "US_DEFAULT_V1", "cost_profile_market_mismatch"),
        ("US", "KR_DEFAULT_V1", "cost_profile_market_mismatch"),
    ],
)
def test_request_rejects_invalid_cost_profile_with_stable_code(
    market: str,
    cost_profile: str,
    expected_code: str,
) -> None:
    payload = run_request_payload(market=market)
    payload["costProfile"] = cost_profile

    with pytest.raises(ValidationError) as raised:
        BacktestRunCreateRequest.model_validate(payload)

    assert expected_code in error_types(raised.value)
    if expected_code == "cost_profile_market_mismatch":
        assert any(
            error["type"] == expected_code and error["loc"] == ("costProfile",)
            for error in raised.value.errors()
        )


def test_request_requires_cost_profile_with_stable_code() -> None:
    payload = run_request_payload()
    del payload["costProfile"]

    with pytest.raises(ValidationError) as raised:
        BacktestRunCreateRequest.model_validate(payload)

    assert "unknown_cost_profile" in error_types(raised.value)


def test_request_rejects_float_cash_and_inverted_date_window() -> None:
    float_payload = run_request_payload()
    float_payload["initialCash"] = 100000000.0
    with pytest.raises(ValidationError) as float_error:
        BacktestRunCreateRequest.model_validate(float_payload)
    assert "decimal_string_required" in error_types(float_error.value)

    inverted_payload = run_request_payload()
    inverted_payload["startDate"] = "2024-02-01"
    inverted_payload["endDate"] = "2024-01-01"
    with pytest.raises(ValidationError) as date_error:
        BacktestRunCreateRequest.model_validate(inverted_payload)
    assert "invalid_date_range" in error_types(date_error.value)


def test_strategy_snapshot_is_deterministic_and_decimal_json_is_string_based() -> None:
    first = strategy_payload()
    second = json.loads(json.dumps(first, sort_keys=True))
    strategy_a = StrategyDefinition.model_validate(first)
    strategy_b = StrategyDefinition.model_validate(second)
    summary = BacktestSummary(
        total_return=Decimal("0.123400"),
        max_drawdown=Decimal("0.0500"),
        annualized_return=Decimal("0.1200"),
        sharpe=Decimal("1.2500"),
        calmar=Decimal("2.4000"),
        win_rate=Decimal("0.6000"),
        total_trades=7,
    )

    assert strategy_snapshot_json(strategy_a) == strategy_snapshot_json(strategy_b)
    snapshot = json.loads(strategy_snapshot_json(strategy_a))
    assert snapshot["risk"]["stop_loss_percent"] is None
    assert snapshot["risk"]["take_profit_percent"] is None
    assert "factor" not in snapshot["factors"][0]["source"]
    assert "value" not in snapshot["entry"]["conditions"][0]["left"]
    serialized = summary.model_dump(mode="json")
    assert serialized["total_return"] == "0.123400"
    assert serialized["sharpe"] == "1.2500"
    assert not any(isinstance(value, float) for value in serialized.values())
