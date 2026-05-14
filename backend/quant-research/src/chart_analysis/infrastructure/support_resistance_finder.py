"""ScipyPeakSupportResistanceFinder — scipy.signal.find_peaks 기반 지지·저항선 탐지.

알고리즘:
  1. high 시계열 → find_peaks(prominence=atr×0.3, distance=5) → 저항 후보
  2. -low 시계열 → find_peaks(prominence=atr×0.3, distance=5) → 지지 후보
  3. ATR×0.5 이내 후보 클러스터링(평균)
  4. 시간 가중치(최근 우선), 상위 5개 반환

금융 안전: 모든 입출력 Decimal 사용.
"""
from __future__ import annotations

from decimal import Decimal

import numpy as np
from scipy.signal import find_peaks

from src.chart_analysis.domain.value_objects import Candle, LevelSet

# scipy find_peaks 파라미터
_DISTANCE = 5


def _float(d: Decimal) -> float:
    """Decimal → float 변환 (내부 계산 전용)."""
    return float(d)


def _cluster_levels(
    raw_levels: list[tuple[float, int]],  # (가격, 인덱스)
    atr_float: float,
    cluster_width: float,
) -> list[float]:
    """ATR×cluster_width 이내 레벨을 클러스터링하여 평균 반환.

    시간 가중치: 최근 인덱스(큰 값)를 우선 처리.
    """
    if not raw_levels:
        return []

    # 인덱스 내림차순(최근 우선) 정렬
    sorted_levels = sorted(raw_levels, key=lambda x: x[1], reverse=True)
    clusters: list[list[float]] = []

    for price, _ in sorted_levels:
        merged = False
        for cluster in clusters:
            centroid = sum(cluster) / len(cluster)
            if abs(price - centroid) <= cluster_width:
                cluster.append(price)
                merged = True
                break
        if not merged:
            clusters.append([price])

    return [sum(c) / len(c) for c in clusters]


class ScipyPeakSupportResistanceFinder:
    """scipy.signal.find_peaks 기반 지지·저항선 탐지기.

    SupportResistanceFinder 포트를 구현한다.
    """

    MAX_LEVELS = 5

    def find(self, candles: list[Candle], atr: Decimal) -> LevelSet:
        """봉 목록과 ATR을 받아 지지·저항 레벨 세트를 반환한다.

        Args:
            candles: 시간 순으로 정렬된 Candle 리스트.
            atr: Average True Range (Decimal).

        Returns:
            LevelSet — 지지선/저항선 각 최대 5개, 모두 Decimal.
        """
        if len(candles) < 3:  # peaks 탐지 최소 요구
            return LevelSet(supports=[], resistances=[])

        atr_float = _float(atr)
        prominence = atr_float * 0.3
        cluster_width = atr_float * 0.5

        highs = np.array([_float(c.high) for c in candles])
        lows = np.array([_float(c.low) for c in candles])

        # 저항 후보: 고가 시계열 고점
        resistance_indices, _ = find_peaks(
            highs, distance=_DISTANCE, prominence=prominence
        )
        resistance_raw = [(highs[i], int(i)) for i in resistance_indices]

        # 지지 후보: 저가 시계열 저점 (반전)
        support_indices, _ = find_peaks(
            -lows, distance=_DISTANCE, prominence=prominence
        )
        support_raw = [(lows[i], int(i)) for i in support_indices]

        # 클러스터링
        resistance_clusters = _cluster_levels(resistance_raw, atr_float, cluster_width)
        support_clusters = _cluster_levels(support_raw, atr_float, cluster_width)

        # 상위 MAX_LEVELS 선택 (최근 우선 정렬 후 클러스터 순서 유지)
        top_resistances = resistance_clusters[: self.MAX_LEVELS]
        top_supports = support_clusters[: self.MAX_LEVELS]

        return LevelSet(
            resistances=[Decimal(str(round(r, 0))) for r in top_resistances],
            supports=[Decimal(str(round(s, 0))) for s in top_supports],
        )
