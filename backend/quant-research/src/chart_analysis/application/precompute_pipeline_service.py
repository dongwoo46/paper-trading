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
9. is_popular=True 일 때 처리 완료 후 queue_repo.enqueue(symbol, window, interval) 호출
"""
from __future__ import annotations

import logging
from datetime import datetime, timezone
from decimal import Decimal

try:
    from chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
    from chart_analysis.domain.chart_snapshot import ChartSnapshot
    from chart_analysis.domain.value_objects import (
        CandlePattern,
        IndicatorSignal,
        LevelSet,
        Recommendation,
        ReportSource,
        TradePlan,
        VolumeAnalysis,
    )
except ImportError:
    from src.chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
    from src.chart_analysis.domain.chart_snapshot import ChartSnapshot
    from src.chart_analysis.domain.value_objects import (
        CandlePattern,
        IndicatorSignal,
        LevelSet,
        Recommendation,
        ReportSource,
        TradePlan,
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

_MIN_CANDLES_BY_WINDOW: dict[tuple[str, str], int] = {
    ("1M", "D"): 20,
    ("3M", "D"): 60,
    ("6M", "D"): 120,
    ("1Y", "D"): 240,
    ("1Y", "W"): 52,
    ("2Y", "W"): 104,
    ("MAX", "W"): 120,
}


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
        queue_repo,
        template_generator=None,
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
        self._queue_repo = queue_repo
        self._template_generator = template_generator

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
                    if is_popular:
                        try:
                            self._queue_repo.enqueue(symbol, window, interval)
                        except Exception as exc:  # noqa: BLE001
                            logger.warning(
                                "precompute:enqueue_failed symbol=%s window=%s interval=%s error=%s",
                                symbol, window, interval, str(exc),
                            )
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
        min_candles = _min_candles_for(window, interval)
        if not candles or len(candles) < min_candles:
            logger.warning(
                "precompute:skip_empty_candles symbol=%s window=%s interval=%s candles=%d min=%d",
                symbol, window, interval, len(candles) if candles else 0,
                min_candles,
            )
            return "skipped"

        # 3. 보조지표 계산
        try:
            indicators = self._indicator_calc.calculate(candles)
        except ValueError as exc:
            logger.warning(
                "precompute:skip_indicator_window symbol=%s window=%s interval=%s error=%s",
                symbol, window, interval, str(exc),
            )
            return "skipped"

        # 4. ChartSnapshot + hash 계산
        snapshot = ChartSnapshot(symbol, window, interval, candles, indicators)
        new_hash = snapshot.compute_hash()

        # 5. 기존 row hash 비교
        now = datetime.now(timezone.utc)
        existing = self._chart_repo.find_one(symbol, window, interval)

        if existing is not None and existing.snapshot_hash == new_hash:
            # 동일 hash → numeric_computed_at 갱신. 템플릿 미존재 시 생성.
            import dataclasses
            updated = dataclasses.replace(existing, numeric_computed_at=now)
            if existing.template_report is None and self._template_generator is not None:
                try:
                    tmpl = self._template_generator.generate(snapshot=None, result=updated)
                    updated = updated.with_template_report(tmpl)
                except Exception as exc:  # noqa: BLE001
                    logger.warning(
                        "precompute:template_generation_failed symbol=%s window=%s error=%s",
                        symbol, window, str(exc),
                    )
            self._chart_repo.upsert(updated)
            logger.debug(
                "precompute:hash_unchanged symbol=%s window=%s interval=%s",
                symbol, window, interval,
            )
            return "done"

        # 6. 수치 분석 재실행
        trend = self._trend_classifier.classify(candles, indicators)
        patterns: list[CandlePattern] = self._pattern_detector.detect(candles)
        levels: LevelSet = self._sr_finder.find(
            candles,
            indicators.atr14,
            window=window,
            interval=interval,
            last_close=candles[-1].close,
        )
        indicator_signals: list[IndicatorSignal] = _build_indicator_signals(indicators)
        volume_analysis: VolumeAnalysis = _compute_volume_analysis(candles, indicators)
        trade_plan = _build_trade_plan(levels, candles[-1].close)
        recommendation: Recommendation = self._confidence_scorer.score(
            trend,
            patterns,
            indicator_signals,
            volume_analysis,
            levels=levels,
            trade_plan=trade_plan,
            last_close=candles[-1].close,
        )

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

        # 7. 룰 기반 템플릿 생성 후 저장
        if self._template_generator is not None:
            try:
                tmpl = self._template_generator.generate(snapshot=None, result=result)
                result = result.with_template_report(tmpl)
            except Exception as exc:  # noqa: BLE001
                logger.warning(
                    "precompute:template_generation_failed symbol=%s window=%s error=%s",
                    symbol, window, str(exc),
                )
        self._chart_repo.upsert(result)
        return "done"


# ---------------------------------------------------------------------------
# 순수 헬퍼 함수 (도메인 의존 없이 Decimal 계산)
# ---------------------------------------------------------------------------

def _build_indicator_signals(indicators) -> list[IndicatorSignal]:
    """핵심 보조지표를 IndicatorSignal 목록으로 변환."""
    signals: list[IndicatorSignal] = []
    rsi = indicators.rsi14

    if rsi >= Decimal("70"):
        rsi_interp = "과매수: 단기 상승 피로가 커질 수 있음"
    elif rsi <= Decimal("30"):
        rsi_interp = "과매도: 단기 반등 가능성을 점검"
    else:
        rsi_interp = "중립: 과열/침체 신호는 약함"

    signals.append(IndicatorSignal(name="RSI", value=rsi, interpretation=rsi_interp))

    macd_hist = indicators.macd_hist
    macd_interp = "상승 모멘텀 우위" if macd_hist > Decimal("0") else "하락 모멘텀 우위"
    signals.append(IndicatorSignal(name="MACD_HIST", value=macd_hist, interpretation=macd_interp))

    close_vs_ma20 = indicators.ma20
    ma_align = (
        "단기>중기>장기 정배열: 추세 우호"
        if indicators.ma20 > indicators.ma60 > indicators.ma120
        else "이동평균 정배열 약함: 추세 확인 필요"
    )
    signals.append(IndicatorSignal(name="MA_ALIGNMENT", value=close_vs_ma20, interpretation=ma_align))

    bb_width = indicators.bb_upper - indicators.bb_lower
    signals.append(
        IndicatorSignal(
            name="BOLLINGER_WIDTH",
            value=bb_width,
            interpretation="밴드 폭이 클수록 변동성 확대, 작을수록 변동성 축소",
        )
    )

    signals.append(
        IndicatorSignal(
            name="ATR14",
            value=indicators.atr14,
            interpretation="가격 변동폭 지표: 손절/목표가 거리 판단에 사용",
        )
    )

    adx_interp = "강한 추세 구간" if indicators.adx14 >= Decimal("25") else "추세 강도 약함 또는 횡보 가능"
    signals.append(IndicatorSignal(name="ADX14", value=indicators.adx14, interpretation=adx_interp))

    if indicators.stoch_k >= Decimal("80"):
        stoch_interp = "단기 과열권"
    elif indicators.stoch_k <= Decimal("20"):
        stoch_interp = "단기 침체권"
    else:
        stoch_interp = "중립권"
    signals.append(IndicatorSignal(name="STOCH_K", value=indicators.stoch_k, interpretation=stoch_interp))
    signals.append(
        IndicatorSignal(
            name="OBV",
            value=indicators.obv,
            interpretation="누적 거래량 방향: 가격 움직임을 거래량이 뒷받침하는지 확인",
        )
    )
    signals.append(
        IndicatorSignal(
            name="VOLUME_MA20",
            value=indicators.volume_ma20,
            interpretation="최근 20봉 평균 거래량: 현재 거래량 스파이크 판단 기준",
        )
    )

    return signals


def _min_candles_for(window: str, interval: str) -> int:
    return _MIN_CANDLES_BY_WINDOW.get((window, interval), 5)


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

    valid_supports = [level for level in levels.supports if level < last_close]
    valid_resistances = [level for level in levels.resistances if level > last_close]

    stop_loss = (
        max(valid_supports)
        if valid_supports
        else (last_close * Decimal("0.95")).quantize(Decimal("0.01"))
    )
    target = (
        min(valid_resistances)
        if valid_resistances
        else (last_close * Decimal("1.10")).quantize(Decimal("0.01"))
    )

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
