"""PrecomputePipelineService — 시장별 수치+LLM 사전계산 배치 Application Service.

윈도우별 로직:
1. OhlcvRepository.find_window(symbol, window, interval) → candles
2. candles 부족 시 스킵 + 경고 로그
3. IndicatorCalculator.calculate(candles) → indicators
4. ChartSnapshot 생성 → compute_hash()
5. 기존 row hash 비교 → 동일 시 numeric_computed_at만 update + LLM 스킵
6. 다름 시: 5개 계산기 호출 → ChartAnalysisResult 조립
7. is_popular=True 일 때만 LlmReportGenerator.generate(...) 호출
8. ChartAnalysisRepository.upsert(result)
"""
from __future__ import annotations

import logging
from datetime import datetime, timezone
from decimal import Decimal
from typing import Optional

from src.chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
from src.chart_analysis.domain.chart_snapshot import ChartSnapshot
from src.chart_analysis.domain.value_objects import (
    CandlePattern,
    Grade,
    IndicatorSignal,
    LevelSet,
    NarrativeReport,
    Recommendation,
    ReportSource,
    TradePlan,
    TrendAnalysis,
    VolumeAnalysis,
)

logger = logging.getLogger(__name__)

# 7개 윈도우 정의
_WINDOWS: list[tuple[str, str]] = [
    ("1M", "D"),
    ("3M", "D"),
    ("6M", "D"),
    ("1Y", "D"),
    ("1Y", "W"),
    ("2Y", "W"),
    ("MAX", "W"),
]

# 최소 봉 수
_MIN_CANDLES = 5


class PrecomputePipelineService:
    """시장별 수치/LLM 사전계산 배치 서비스.

    의존성은 생성자에서 주입 (DI). 도메인 순수 — 인프라 직접 import 금지.
    """

    def __init__(
        self,
        ohlcv_repo,
        chart_analysis_repo,
        indicator_calculator,
        sr_finder,
        pattern_detector,
        trend_classifier,
        confidence_scorer,
        llm_generator,
        slack_notifier,
    ) -> None:
        self._ohlcv_repo = ohlcv_repo
        self._chart_repo = chart_analysis_repo
        self._indicator_calc = indicator_calculator
        self._sr_finder = sr_finder
        self._pattern_detector = pattern_detector
        self._trend_classifier = trend_classifier
        self._confidence_scorer = confidence_scorer
        self._llm_generator = llm_generator
        self._slack = slack_notifier

    async def run_for_symbol(self, symbol: str, is_popular: bool) -> dict:
        """7 윈도우 순회 — 성공/실패/스킵 카운트 반환."""
        success = 0
        failed = 0
        skipped = 0

        for window, interval in _WINDOWS:
            try:
                outcome = await self._process_window(symbol, window, interval, is_popular)
                if outcome == "skipped":
                    skipped += 1
                else:
                    success += 1
            except Exception as exc:  # noqa: BLE001
                failed += 1
                logger.warning(
                    "precompute:window_failed symbol=%s window=%s interval=%s error=%s",
                    symbol, window, interval, str(exc),
                )

        return {"success": success, "failed": failed, "skipped": skipped}

    async def run_for_market(
        self, market: str, symbols: list[str], popular_set: set[str]
    ) -> dict:
        """시장별 종목 순회 → 완료 후 notify_batch_completed 호출."""
        total_success = 0
        total_failed = 0

        for symbol in symbols:
            is_popular = symbol in popular_set
            try:
                result = await self.run_for_symbol(symbol, is_popular=is_popular)
                total_success += result["success"]
                total_failed += result["failed"]
            except Exception as exc:  # noqa: BLE001
                total_failed += 1
                logger.warning(
                    "precompute:symbol_failed market=%s symbol=%s error=%s",
                    market, symbol, str(exc),
                )

        await self._slack.notify_batch_completed(market, total_success, total_failed)
        return {"market": market, "success": total_success, "failed": total_failed}

    async def _process_window(
        self, symbol: str, window: str, interval: str, is_popular: bool
    ) -> str:
        """단일 윈도우 처리. 반환값: 'done' | 'skipped'."""
        # 1. OHLCV 조회
        candles = self._ohlcv_repo.find_window(symbol, window, interval)

        # 2. 봉 부족 시 스킵
        if not candles or len(candles) < _MIN_CANDLES:
            logger.warning(
                "precompute:skip_empty_candles symbol=%s window=%s interval=%s candles=%d",
                symbol, window, interval, len(candles) if candles else 0,
            )
            return "skipped"

        # 3. 보조지표 계산
        indicators = self._indicator_calc.calculate(candles)

        # 4. ChartSnapshot + hash 계산
        snapshot = ChartSnapshot(symbol, window, interval, candles, indicators)
        new_hash = snapshot.compute_hash()

        # 5. 기존 row hash 비교
        now = datetime.now(timezone.utc)
        existing = self._chart_repo.find_one(symbol, window, interval)

        if existing is not None and existing.snapshot_hash == new_hash:
            # 동일 hash → numeric_computed_at만 갱신 (LLM 스킵)
            import dataclasses
            updated = dataclasses.replace(existing, numeric_computed_at=now)
            self._chart_repo.upsert(updated)
            logger.debug(
                "precompute:hash_unchanged symbol=%s window=%s interval=%s",
                symbol, window, interval,
            )
            return "done"

        # 6. 수치 분석 재실행
        trend = self._trend_classifier.classify(candles, indicators)
        patterns: list[CandlePattern] = self._pattern_detector.detect(candles)
        levels: LevelSet = self._sr_finder.find(candles, indicators.atr14)
        indicator_signals: list[IndicatorSignal] = _build_indicator_signals(indicators)
        volume_analysis: VolumeAnalysis = _compute_volume_analysis(candles, indicators)
        recommendation: Recommendation = self._confidence_scorer.score(
            trend, patterns, indicator_signals, volume_analysis
        )
        trade_plan = _build_trade_plan(levels, candles[-1].close)

        result = ChartAnalysisResult(
            symbol=symbol,
            window=window,
            interval=interval,
            snapshot_hash=new_hash,
            trend=trend,
            levels=levels,
            trade_plan=trade_plan,
            patterns=patterns,
            indicator_signals=indicator_signals,
            volume_analysis=volume_analysis,
            recommendation=recommendation,
            report=None,
            report_source=ReportSource.NONE,
            numeric_computed_at=now,
            llm_computed_at=None,
        )

        # 7. is_popular=True 일 때만 LLM 호출
        if is_popular:
            try:
                narrative: NarrativeReport = self._llm_generator.generate(snapshot, result)
                result = result.with_report(narrative, narrative.source)
            except Exception as exc:  # noqa: BLE001
                logger.warning(
                    "precompute:llm_failed symbol=%s window=%s interval=%s error=%s",
                    symbol, window, interval, str(exc),
                )

        # 8. upsert
        self._chart_repo.upsert(result)
        return "done"


# ---------------------------------------------------------------------------
# 순수 헬퍼 함수 (도메인 의존 없이 Decimal 계산)
# ---------------------------------------------------------------------------

def _build_indicator_signals(indicators) -> list[IndicatorSignal]:
    """핵심 보조지표를 IndicatorSignal 목록으로 변환."""
    from src.chart_analysis.domain.value_objects import IndicatorSignal

    signals = []
    rsi = indicators.rsi14

    if rsi >= Decimal("70"):
        rsi_interp = "overbought"
    elif rsi <= Decimal("30"):
        rsi_interp = "oversold"
    else:
        rsi_interp = "neutral"

    signals.append(IndicatorSignal(name="RSI", value=rsi, interpretation=rsi_interp))

    macd_hist = indicators.macd_hist
    macd_interp = "bullish" if macd_hist > Decimal("0") else "bearish"
    signals.append(IndicatorSignal(name="MACD_HIST", value=macd_hist, interpretation=macd_interp))

    return signals


def _compute_volume_analysis(candles, indicators) -> VolumeAnalysis:
    """최근 봉의 거래량 추이를 간단히 분석."""
    if not candles:
        return VolumeAnalysis(trend="flat", spike_detected=False, avg_ratio=Decimal("1"))

    recent_vol = candles[-1].volume
    vol_ma = indicators.volume_ma20
    if vol_ma == Decimal("0"):
        ratio = Decimal("1")
    else:
        ratio = (recent_vol / vol_ma).quantize(Decimal("0.01"))

    spike = ratio > Decimal("2")
    trend = "increasing" if ratio > Decimal("1.1") else ("decreasing" if ratio < Decimal("0.9") else "flat")

    return VolumeAnalysis(trend=trend, spike_detected=spike, avg_ratio=ratio)


def _build_trade_plan(levels: LevelSet, last_close: Decimal) -> TradePlan:
    """레벨셋과 현재가로 기본 Trade Plan 생성."""
    entry = last_close

    stop_loss = levels.supports[0] if levels.supports else (last_close * Decimal("0.95")).quantize(Decimal("0.01"))
    target = levels.resistances[0] if levels.resistances else (last_close * Decimal("1.10")).quantize(Decimal("0.01"))

    risk = entry - stop_loss
    reward = target - entry

    if risk > Decimal("0"):
        rr = (reward / risk).quantize(Decimal("0.01"))
    else:
        rr = Decimal("0")

    return TradePlan(
        entry_price=entry,
        stop_loss=stop_loss,
        target_price=target,
        risk_reward_ratio=rr,
    )