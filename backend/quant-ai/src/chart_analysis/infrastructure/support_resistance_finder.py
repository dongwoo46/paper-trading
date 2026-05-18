"""Support/resistance finder with window-specific ranking policy.

Public output remains LevelSet with Decimal price levels. Composite scores are
kept internally for ranking and quality inspection.
"""
from __future__ import annotations

from dataclasses import dataclass
from decimal import Decimal

import numpy as np
from scipy.signal import find_peaks

try:  # test suite imports chart_analysis as a top-level package.
    from chart_analysis.domain.value_objects import Candle, LevelSet
except ImportError:  # production modules may import through src.chart_analysis.
    from src.chart_analysis.domain.value_objects import Candle, LevelSet


@dataclass(frozen=True)
class SupportResistancePolicy:
    peak_distance: int
    prominence_atr_multiplier: float
    cluster_width_atr_multiplier: float
    top_n: int
    recency_decay: float
    endpoint_lookback: int


@dataclass(frozen=True)
class ScoredLevel:
    price: Decimal
    side: str
    touch_count: int
    rejection_strength: Decimal
    recency: Decimal
    volume: Decimal
    price_proximity: Decimal
    endpoint: bool
    score: Decimal


_DEFAULT_POLICY = SupportResistancePolicy(
    peak_distance=5,
    prominence_atr_multiplier=0.30,
    cluster_width_atr_multiplier=0.50,
    top_n=5,
    recency_decay=1.00,
    endpoint_lookback=20,
)

_POLICIES: dict[tuple[str, str], SupportResistancePolicy] = {
    ("1M", "D"): SupportResistancePolicy(4, 0.25, 0.45, 5, 0.85, 12),
    ("3M", "D"): SupportResistancePolicy(5, 0.30, 0.50, 5, 1.00, 20),
    ("6M", "D"): SupportResistancePolicy(7, 0.35, 0.60, 5, 1.25, 25),
    ("1Y", "D"): SupportResistancePolicy(10, 0.45, 0.70, 5, 1.60, 35),
    ("1Y", "W"): SupportResistancePolicy(6, 0.45, 0.75, 5, 1.70, 18),
    ("2Y", "W"): SupportResistancePolicy(8, 0.55, 0.90, 6, 2.20, 26),
    ("MAX", "W"): SupportResistancePolicy(10, 0.60, 1.00, 6, 3.00, 30),
}


def policy_for_window(window: str | None, interval: str | None) -> SupportResistancePolicy:
    return _POLICIES.get((window or "", interval or ""), _DEFAULT_POLICY)


def _float(d: Decimal) -> float:
    return float(d)


def _decimal(value: float) -> Decimal:
    return Decimal(str(round(value, 0)))


def _clamp(value: float, low: float = 0.0, high: float = 1.0) -> float:
    return max(low, min(high, value))


class ScipyPeakSupportResistanceFinder:
    """scipy.signal.find_peaks based support/resistance finder."""

    MAX_LEVELS = 5

    def __init__(self) -> None:
        self.last_scored_levels: tuple[ScoredLevel, ...] = ()

    def find(
        self,
        candles: list[Candle],
        atr: Decimal,
        *,
        window: str | None = None,
        interval: str | None = None,
        last_close: Decimal | None = None,
    ) -> LevelSet:
        if len(candles) < 3:
            self.last_scored_levels = ()
            return LevelSet(supports=[], resistances=[])

        policy = policy_for_window(window, interval)
        close = last_close if last_close is not None else candles[-1].close
        scored = self._find_scored(candles, atr, close, policy)
        self.last_scored_levels = tuple(scored)

        supports = [level.price for level in scored if level.side == "support"][: policy.top_n]
        resistances = [level.price for level in scored if level.side == "resistance"][: policy.top_n]
        return LevelSet(supports=supports, resistances=resistances)

    def _find_scored(
        self,
        candles: list[Candle],
        atr: Decimal,
        last_close: Decimal,
        policy: SupportResistancePolicy,
    ) -> list[ScoredLevel]:
        atr_float = max(_float(atr), 0.0001)
        prominence = atr_float * policy.prominence_atr_multiplier
        cluster_width = max(atr_float * policy.cluster_width_atr_multiplier, 0.0001)

        highs = np.array([_float(c.high) for c in candles])
        lows = np.array([_float(c.low) for c in candles])
        avg_volume = sum((_float(c.volume) for c in candles), 0.0) / len(candles)

        resistance_indices, _ = find_peaks(
            highs, distance=policy.peak_distance, prominence=prominence
        )
        support_indices, _ = find_peaks(
            -lows, distance=policy.peak_distance, prominence=prominence
        )

        raw = []
        for i in resistance_indices:
            raw.append(self._candidate(candles, int(i), "resistance", highs[int(i)], atr_float, avg_volume))
        for i in support_indices:
            raw.append(self._candidate(candles, int(i), "support", lows[int(i)], atr_float, avg_volume))

        raw.extend(self._endpoint_candidates(candles, atr_float, avg_volume, policy))

        scored: list[ScoredLevel] = []
        for side in ("support", "resistance"):
            clusters = self._cluster([c for c in raw if c["side"] == side], cluster_width)
            scored.extend(
                self._score_cluster(cluster, side, len(candles), last_close, atr_float, policy)
                for cluster in clusters
            )

        return sorted(scored, key=lambda level: (level.score, level.recency, level.price), reverse=True)

    @staticmethod
    def _candidate(
        candles: list[Candle],
        index: int,
        side: str,
        price: float,
        atr_float: float,
        avg_volume: float,
        *,
        endpoint: bool = False,
    ) -> dict:
        candle = candles[index]
        if side == "resistance":
            rejection = max(0.0, (price - _float(max(candle.open, candle.close))) / atr_float)
        else:
            rejection = max(0.0, (_float(min(candle.open, candle.close)) - price) / atr_float)
        volume_ratio = 1.0 if avg_volume == 0 else _float(candle.volume) / avg_volume
        return {
            "price": price,
            "index": index,
            "side": side,
            "rejection": rejection,
            "volume_ratio": volume_ratio,
            "endpoint": endpoint,
        }

    def _endpoint_candidates(
        self,
        candles: list[Candle],
        atr_float: float,
        avg_volume: float,
        policy: SupportResistancePolicy,
    ) -> list[dict]:
        lookback = min(policy.endpoint_lookback, len(candles))
        offset = len(candles) - lookback
        recent = candles[-lookback:]

        high_idx, high_candle = max(enumerate(recent), key=lambda item: item[1].high)
        low_idx, low_candle = min(enumerate(recent), key=lambda item: item[1].low)
        return [
            self._candidate(
                candles,
                offset + high_idx,
                "resistance",
                _float(high_candle.high),
                atr_float,
                avg_volume,
                endpoint=True,
            ),
            self._candidate(
                candles,
                offset + low_idx,
                "support",
                _float(low_candle.low),
                atr_float,
                avg_volume,
                endpoint=True,
            ),
        ]

    @staticmethod
    def _cluster(candidates: list[dict], cluster_width: float) -> list[list[dict]]:
        clusters: list[list[dict]] = []
        for candidate in sorted(candidates, key=lambda c: c["index"], reverse=True):
            for cluster in clusters:
                centroid = sum(c["price"] for c in cluster) / len(cluster)
                if abs(candidate["price"] - centroid) <= cluster_width:
                    cluster.append(candidate)
                    break
            else:
                clusters.append([candidate])
        return clusters

    @staticmethod
    def _score_cluster(
        cluster: list[dict],
        side: str,
        candle_count: int,
        last_close: Decimal,
        atr_float: float,
        policy: SupportResistancePolicy,
    ) -> ScoredLevel:
        touch_count = len(cluster)
        price = sum(c["price"] for c in cluster) / touch_count
        latest_index = max(c["index"] for c in cluster)
        age_ratio = 0.0 if candle_count <= 1 else (candle_count - 1 - latest_index) / (candle_count - 1)
        recency = pow(2.718281828, -policy.recency_decay * age_ratio)
        rejection = sum(c["rejection"] for c in cluster) / touch_count
        volume = sum(c["volume_ratio"] for c in cluster) / touch_count
        proximity = 1.0 - _clamp(abs(price - _float(last_close)) / (atr_float * 12.0))
        endpoint = any(c["endpoint"] for c in cluster)

        score = (
            _clamp(touch_count / 5.0) * 0.30
            + _clamp(rejection / 2.0) * 0.20
            + recency * 0.20
            + _clamp(volume / 2.0) * 0.15
            + proximity * 0.10
            + (0.05 if endpoint else 0.0)
        )

        return ScoredLevel(
            price=_decimal(price),
            side=side,
            touch_count=touch_count,
            rejection_strength=Decimal(str(round(rejection, 4))),
            recency=Decimal(str(round(recency, 4))),
            volume=Decimal(str(round(volume, 4))),
            price_proximity=Decimal(str(round(proximity, 4))),
            endpoint=endpoint,
            score=Decimal(str(round(score, 4))),
        )
