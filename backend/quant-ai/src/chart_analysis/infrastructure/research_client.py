"""ResearchClient — quant-research HTTP 서비스 클라이언트.

5개 계산기 Port(IndicatorCalculator, TrendClassifier, PatternDetector,
SupportResistanceFinder, ConfidenceScorer)를 모두 구현한다.

캐싱 전략:
  calculate() 최초 호출 시 quant-research 에 HTTP 요청 → 전체 결과 캐싱.
  나머지 4개 메서드는 동일 캔들 해시에 대해 캐시에서 반환.
  캐시 키: "{첫봉.date}|{마지막봉.date}|{봉개수}"

금융 안전: 모든 수치는 Decimal 사용. float 절대 금지.
"""
from __future__ import annotations

from datetime import date as date_type
from decimal import Decimal
from typing import Any

import httpx

from src.chart_analysis.domain.value_objects import (
    CandlePattern,
    Direction,
    Grade,
    IndicatorSet,
    LevelSet,
    PatternType,
    Recommendation,
    Strength,
    TrendAnalysis,
)


class _AnalyzeResponse:
    """quant-research /research/analyze 응답 래퍼 (내부 전용)."""

    __slots__ = ("indicators", "trend", "patterns", "levels", "recommendation")

    def __init__(self, data: dict[str, Any]) -> None:
        self.indicators: dict[str, str] = data["indicators"]
        self.trend: dict[str, str] = data["trend"]
        self.patterns: list[dict[str, Any]] = data["patterns"]
        self.levels: dict[str, list[str]] = data["levels"]
        self.recommendation: dict[str, str] = data["recommendation"]


class ResearchClient:
    """quant-research HTTP 서비스 어댑터.

    5개 계산기 Port를 모두 구현하며, 동일 캔들 셋에 대해
    HTTP 요청을 한 번만 수행하고 결과를 캐싱한다.
    """

    def __init__(self, research_service_url: str) -> None:
        self._url = research_service_url.rstrip("/")
        self._cache: dict[str, _AnalyzeResponse] = {}

    # ------------------------------------------------------------------
    # 캐시 키 / HTTP 요청
    # ------------------------------------------------------------------

    def _cache_key(self, candles) -> str:
        """캔들 리스트의 해시 키를 생성한다 (첫봉.date|마지막봉.date|봉개수)."""
        return f"{candles[0].date}|{candles[-1].date}|{len(candles)}"

    def _fetch(self, candles) -> _AnalyzeResponse:
        """캐시 미스 시 quant-research 에 HTTP POST 요청하고 결과를 캐싱한다."""
        key = self._cache_key(candles)
        if key in self._cache:
            return self._cache[key]

        payload = {
            "candles": [
                {
                    "date": str(c.date),
                    "open": str(c.open),
                    "high": str(c.high),
                    "low": str(c.low),
                    "close": str(c.close),
                    "volume": str(c.volume),
                }
                for c in candles
            ]
        }
        resp = httpx.post(
            f"{self._url}/research/analyze",
            json=payload,
            timeout=60.0,
        )
        resp.raise_for_status()
        result = _AnalyzeResponse(resp.json())
        self._cache[key] = result
        return result

    # ------------------------------------------------------------------
    # IndicatorCalculator Port
    # ------------------------------------------------------------------

    def calculate(self, candles) -> IndicatorSet:
        """봉 목록으로 보조지표 세트를 계산한다 (HTTP 위임)."""
        r = self._fetch(candles)
        ind = r.indicators
        return IndicatorSet(
            ma20=Decimal(ind["ma20"]),
            ma60=Decimal(ind["ma60"]),
            ma120=Decimal(ind["ma120"]),
            rsi14=Decimal(ind["rsi14"]),
            macd=Decimal(ind["macd"]),
            macd_signal=Decimal(ind["macd_signal"]),
            macd_hist=Decimal(ind["macd_hist"]),
            bb_upper=Decimal(ind["bb_upper"]),
            bb_lower=Decimal(ind["bb_lower"]),
            atr14=Decimal(ind["atr14"]),
            adx14=Decimal(ind["adx14"]),
            stoch_k=Decimal(ind["stoch_k"]),
            stoch_d=Decimal(ind["stoch_d"]),
            obv=Decimal(ind["obv"]),
            volume_ma20=Decimal(ind["volume_ma20"]),
        )

    # ------------------------------------------------------------------
    # TrendClassifier Port
    # ------------------------------------------------------------------

    def classify(self, candles, indicators) -> TrendAnalysis:
        """봉 + 보조지표로 추세를 분류한다 (캐시에서 반환)."""
        r = self._fetch(candles)
        t = r.trend
        return TrendAnalysis(
            direction=Direction(t["direction"]),
            strength=Strength(t["strength"]),
            ma_alignment=t["ma_alignment"],
            adx=Decimal(t["adx"]),
            hh_ll_structure=t["hh_ll_structure"],
        )

    # ------------------------------------------------------------------
    # PatternDetector Port
    # ------------------------------------------------------------------

    def detect(self, candles) -> list[CandlePattern]:
        """봉 목록에서 캔들 패턴 목록을 반환한다 (캐시에서 반환)."""
        r = self._fetch(candles)
        return [
            CandlePattern(
                type=PatternType(p["type"]),
                index=p["index"],
                date=date_type.fromisoformat(p["date"]),
            )
            for p in r.patterns
        ]

    # ------------------------------------------------------------------
    # SupportResistanceFinder Port
    # ------------------------------------------------------------------

    def find(self, candles, atr14) -> LevelSet:
        """봉 목록과 ATR을 받아 지지·저항 레벨 세트를 반환한다 (캐시에서 반환)."""
        r = self._fetch(candles)
        lv = r.levels
        return LevelSet(
            supports=[Decimal(s) for s in lv["supports"]],
            resistances=[Decimal(s) for s in lv["resistances"]],
        )

    # ------------------------------------------------------------------
    # ConfidenceScorer Port
    # ------------------------------------------------------------------

    def score(self, trend, patterns, indicator_signals, volume_analysis) -> Recommendation:
        """분석 결과를 받아 Recommendation을 반환한다 (캐시 마지막 항목 사용)."""
        if not self._cache:
            raise RuntimeError(
                "ResearchClient.score() 호출 전에 calculate()가 선행되어야 합니다."
            )
        rec = next(reversed(self._cache.values())).recommendation
        return Recommendation(
            grade=Grade(rec["grade"]),
            confidence=Decimal(rec["confidence"]),
        )
