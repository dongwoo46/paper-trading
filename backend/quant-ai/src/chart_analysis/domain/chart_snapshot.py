"""ChartSnapshot Aggregate Root — 분석 입력 단위."""
from __future__ import annotations

import hashlib
import json
from dataclasses import dataclass
from decimal import Decimal

from .value_objects import Candle, IndicatorSet

_VALID_WINDOWS = frozenset({"1M", "3M", "6M", "1Y", "2Y", "MAX"})
_VALID_INTERVALS = frozenset({"D", "W"})


@dataclass(frozen=True)
class ChartSnapshot:
    """분석 입력 Aggregate Root.

    식별자: (symbol, window, interval)
    불변 객체 — __post_init__에서 유효성 검사.
    """

    symbol: str
    window: str
    interval: str
    candles: tuple[Candle, ...]
    indicators: IndicatorSet

    def __init__(
        self,
        symbol: str,
        window: str,
        interval: str,
        candles: list[Candle],
        indicators: IndicatorSet,
    ) -> None:
        object.__setattr__(self, "symbol", symbol)
        object.__setattr__(self, "window", window)
        object.__setattr__(self, "interval", interval)
        object.__setattr__(self, "candles", tuple(candles))
        object.__setattr__(self, "indicators", indicators)
        self.__post_init__()

    def __post_init__(self) -> None:
        if self.window not in _VALID_WINDOWS:
            raise ValueError(
                f"window must be one of {sorted(_VALID_WINDOWS)}, got '{self.window}'"
            )
        if self.interval not in _VALID_INTERVALS:
            raise ValueError(
                f"interval must be one of {sorted(_VALID_INTERVALS)}, got '{self.interval}'"
            )
        if not self.candles:
            raise ValueError("candles must not be empty")

    # ------------------------------------------------------------------
    # Query methods
    # ------------------------------------------------------------------

    def last_close(self) -> Decimal:
        """마지막 봉의 종가를 반환한다."""
        return self.candles[-1].close

    def length(self) -> int:
        """봉 수를 반환한다."""
        return len(self.candles)

    def compute_hash(self) -> str:
        """콘텐트 해시 (sha256).

        모든 Decimal은 str로 변환해 float 연산 없이 직렬화한다.
        결과: 64자 hex digest.
        """
        payload = {
            "symbol": self.symbol,
            "window": self.window,
            "interval": self.interval,
            "candles": [
                {
                    "date": c.date.isoformat(),
                    "open": str(c.open),
                    "high": str(c.high),
                    "low": str(c.low),
                    "close": str(c.close),
                    "volume": str(c.volume),
                }
                for c in self.candles
            ],
            "indicators": {
                "ma20": str(self.indicators.ma20),
                "ma60": str(self.indicators.ma60),
                "ma120": str(self.indicators.ma120),
                "rsi14": str(self.indicators.rsi14),
                "macd": str(self.indicators.macd),
                "macd_signal": str(self.indicators.macd_signal),
                "macd_hist": str(self.indicators.macd_hist),
                "bb_upper": str(self.indicators.bb_upper),
                "bb_lower": str(self.indicators.bb_lower),
                "atr14": str(self.indicators.atr14),
                "adx14": str(self.indicators.adx14),
                "stoch_k": str(self.indicators.stoch_k),
                "stoch_d": str(self.indicators.stoch_d),
                "obv": str(self.indicators.obv),
                "volume_ma20": str(self.indicators.volume_ma20),
            },
        }
        raw = json.dumps(payload, sort_keys=True, ensure_ascii=False)
        return hashlib.sha256(raw.encode("utf-8")).hexdigest()