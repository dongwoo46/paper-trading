"""골든 fixture 로더 헬퍼.

JSON fixture 파일을 로드하여 dict를 반환하고,
input_candles 배열을 Candle 도메인 객체 리스트로 변환하는 유틸리티를 제공한다.
"""
from __future__ import annotations

import json
import os
from datetime import date, datetime
from decimal import Decimal
from pathlib import Path
from typing import Any

from src.chart_analysis.domain.value_objects import Candle

_FIXTURES_DIR = Path(__file__).parent


def load_fixture(relative_path: str) -> dict[str, Any]:
    """fixtures/chart_analysis/ 기준 상대 경로로 JSON fixture를 로드한다.

    Args:
        relative_path: 예) "patterns/hammer_01.json"

    Returns:
        파싱된 dict.
    """
    path = _FIXTURES_DIR / relative_path
    with path.open(encoding="utf-8") as f:
        return json.load(f)


def candles_from_fixture(fixture: dict[str, Any]) -> list[Candle]:
    """fixture dict의 "input_candles" 항목을 Candle 리스트로 변환한다.

    모든 가격/거래량 필드는 Decimal(str_value) 로 변환한다.
    """
    candles: list[Candle] = []
    for raw in fixture["input_candles"]:
        candles.append(
            Candle(
                date=datetime.strptime(raw["date"], "%Y-%m-%d").date(),
                open=Decimal(raw["open"]),
                high=Decimal(raw["high"]),
                low=Decimal(raw["low"]),
                close=Decimal(raw["close"]),
                volume=Decimal(raw["volume"]),
            )
        )
    return candles


def indicator_set_from_fixture(fixture: dict[str, Any]):
    """fixture dict의 "mock_indicators" 항목을 IndicatorSet으로 변환한다."""
    from src.chart_analysis.domain.value_objects import IndicatorSet

    raw = fixture["mock_indicators"]
    return IndicatorSet(
        ma20=Decimal(raw["ma20"]),
        ma60=Decimal(raw["ma60"]),
        ma120=Decimal(raw["ma120"]),
        rsi14=Decimal(raw["rsi14"]),
        macd=Decimal(raw["macd"]),
        macd_signal=Decimal(raw["macd_signal"]),
        macd_hist=Decimal(raw["macd_hist"]),
        bb_upper=Decimal(raw["bb_upper"]),
        bb_lower=Decimal(raw["bb_lower"]),
        atr14=Decimal(raw["atr14"]),
        adx14=Decimal(raw["adx14"]),
        stoch_k=Decimal(raw["stoch_k"]),
        stoch_d=Decimal(raw["stoch_d"]),
        obv=Decimal(raw["obv"]),
        volume_ma20=Decimal(raw["volume_ma20"]),
    )
