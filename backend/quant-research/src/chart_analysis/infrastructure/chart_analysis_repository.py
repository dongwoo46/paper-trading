"""PostgresChartAnalysisRepository — ChartAnalysisRepository 포트 어댑터.

upsert: INSERT ... ON CONFLICT (symbol, window, interval) DO UPDATE SET ...
find_by_symbol: 7 윈도우 반환 (1M-D, 3M-D, 6M-D, 1Y-D, 1Y-W, 2Y-W, MAX-W)
JSONB 직렬화: DecimalJSONEncoder (Decimal → str)
"""
from __future__ import annotations

import json
from decimal import Decimal
from typing import Any, Optional

from src.chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
from src.chart_analysis.domain.value_objects import (
    CandlePattern,
    Direction,
    Grade,
    IndicatorSignal,
    LevelSet,
    NarrativeReport,
    PatternType,
    Recommendation,
    ReportSource,
    Strength,
    TradePlan,
    TrendAnalysis,
    VolumeAnalysis,
)


# ---------------------------------------------------------------------------
# 7 윈도우 순서 (응답 스펙)
# ---------------------------------------------------------------------------
_WINDOW_ORDER = [
    ("1M", "D"), ("3M", "D"), ("6M", "D"), ("1Y", "D"),
    ("1Y", "W"), ("2Y", "W"), ("MAX", "W"),
]


# ---------------------------------------------------------------------------
# Custom JSON Encoder
# ---------------------------------------------------------------------------

class DecimalJSONEncoder(json.JSONEncoder):
    """Decimal → str 직렬화 인코더 (float 금지)."""

    def default(self, obj: Any) -> Any:
        if isinstance(obj, Decimal):
            return str(obj)
        return super().default(obj)


def _dumps(obj: Any) -> str:
    return json.dumps(obj, cls=DecimalJSONEncoder, ensure_ascii=False)


# ---------------------------------------------------------------------------
# Repository
# ---------------------------------------------------------------------------

class PostgresChartAnalysisRepository:
    """ChartAnalysisRepository 포트를 구현하는 psycopg 어댑터."""

    def __init__(self, connect_fn) -> None:
        self._connect = connect_fn

    # ------------------------------------------------------------------
    # upsert
    # ------------------------------------------------------------------

    def upsert(self, result: ChartAnalysisResult) -> None:
        """분석 결과를 저장하거나 갱신한다 (PK 충돌 시 갱신)."""
        query = _UPSERT_SQL
        params = self._to_params(result)
        with self._connect() as conn:
            with conn.cursor() as cur:
                cur.execute(query, params)
            conn.commit()

    # ------------------------------------------------------------------
    # find_by_symbol
    # ------------------------------------------------------------------

    def find_by_symbol(self, symbol: str) -> list[ChartAnalysisResult]:
        """종목코드로 모든 윈도우 분석 결과를 반환한다 (7 윈도우 순서 유지)."""
        query = (
            "SELECT symbol, window, interval, snapshot_hash, recommendation, confidence, "
            "levels, trend, patterns, indicator_signals, volume_analysis, "
            "llm_report, llm_report_source, numeric_computed_at, llm_computed_at "
            "FROM chart_analysis_result "
            "WHERE symbol = %s "
            "ORDER BY CASE (window, interval) "
            + _build_order_case()
            + " END"
        )
        with self._connect() as conn:
            with conn.cursor() as cur:
                cur.execute(query, (symbol,))
                rows = cur.fetchall()

        return [self._row_to_result(row) for row in rows]

    # ------------------------------------------------------------------
    # find_one
    # ------------------------------------------------------------------

    def find_one(
        self, symbol: str, window: str, interval: str
    ) -> Optional[ChartAnalysisResult]:
        """단일 (symbol, window, interval) 분석 결과를 반환한다."""
        query = (
            "SELECT symbol, window, interval, snapshot_hash, recommendation, confidence, "
            "levels, trend, patterns, indicator_signals, volume_analysis, "
            "llm_report, llm_report_source, numeric_computed_at, llm_computed_at "
            "FROM chart_analysis_result "
            "WHERE symbol = %s AND window = %s AND interval = %s"
        )
        with self._connect() as conn:
            with conn.cursor() as cur:
                cur.execute(query, (symbol, window, interval))
                row = cur.fetchone()
        if row is None:
            return None
        return self._row_to_result(row)

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _to_params(self, result: ChartAnalysisResult) -> tuple:
        levels_json = _dumps({
            "supports": list(result.levels.supports),
            "resistances": list(result.levels.resistances),
            "entry": result.trade_plan.entry_price,
            "stop_loss": result.trade_plan.stop_loss,
            "target": result.trade_plan.target_price,
            "risk_reward": result.trade_plan.risk_reward_ratio,
        })
        trend_json = _dumps({
            "direction": result.trend.direction.value,
            "strength": result.trend.strength.value,
            "ma_alignment": result.trend.ma_alignment,
            "adx": result.trend.adx,
            "hh_ll_structure": result.trend.hh_ll_structure,
        })
        patterns_json = _dumps([
            {"type": p.type.value, "index": p.index, "date": p.date.isoformat()}
            for p in result.patterns
        ])
        signals_json = _dumps([
            {"name": s.name, "value": s.value, "interpretation": s.interpretation}
            for s in result.indicator_signals
        ])
        volume_json = _dumps({
            "trend": result.volume_analysis.trend,
            "spike_detected": result.volume_analysis.spike_detected,
            "avg_ratio": result.volume_analysis.avg_ratio,
        })
        llm_report_json = None
        if result.report is not None:
            llm_report_json = _dumps({
                "trend_section": result.report.trend_section,
                "support_resistance_section": result.report.support_resistance_section,
                "entry_plan_section": result.report.entry_plan_section,
                "signal_evidence_section": result.report.signal_evidence_section,
                "risk_section": result.report.risk_section,
                "source": result.report.source.value,
            })

        return (
            result.symbol,
            result.window,
            result.interval,
            result.snapshot_hash,
            result.recommendation.grade.value,
            str(result.recommendation.confidence),
            levels_json,
            trend_json,
            patterns_json,
            signals_json,
            volume_json,
            llm_report_json,
            result.report_source.value,
            result.numeric_computed_at,
            result.llm_computed_at,
        )

    @staticmethod
    def _row_to_result(row: tuple) -> ChartAnalysisResult:
        (
            symbol, window, interval, snapshot_hash, recommendation_str, confidence_val,
            levels_raw, trend_raw, patterns_raw, signals_raw, volume_raw,
            llm_report_raw, llm_report_source_str, numeric_computed_at, llm_computed_at,
        ) = row

        # JSONB — psycopg이 dict/list로 변환해줄 수 있음. str이면 parse.
        levels_data = _parse_json(levels_raw)
        trend_data = _parse_json(trend_raw)
        patterns_data = _parse_json(patterns_raw)
        signals_data = _parse_json(signals_raw)
        volume_data = _parse_json(volume_raw)

        trend = TrendAnalysis(
            direction=Direction(trend_data["direction"]),
            strength=Strength(trend_data["strength"]),
            ma_alignment=trend_data["ma_alignment"],
            adx=Decimal(str(trend_data["adx"])),
            hh_ll_structure=trend_data["hh_ll_structure"],
        )
        levels = LevelSet(
            supports=[Decimal(str(v)) for v in levels_data.get("supports", [])],
            resistances=[Decimal(str(v)) for v in levels_data.get("resistances", [])],
        )
        trade_plan = TradePlan(
            entry_price=Decimal(str(levels_data["entry"])),
            stop_loss=Decimal(str(levels_data["stop_loss"])),
            target_price=Decimal(str(levels_data["target"])),
            risk_reward_ratio=Decimal(str(levels_data["risk_reward"])),
        )
        patterns = [
            CandlePattern(
                type=PatternType(p["type"]),
                index=p["index"],
                date=_parse_date(p["date"]),
            )
            for p in patterns_data
        ]
        indicator_signals = [
            IndicatorSignal(
                name=s["name"],
                value=Decimal(str(s["value"])),
                interpretation=s["interpretation"],
            )
            for s in signals_data
        ]
        volume_analysis = VolumeAnalysis(
            trend=volume_data["trend"],
            spike_detected=bool(volume_data["spike_detected"]),
            avg_ratio=Decimal(str(volume_data["avg_ratio"])),
        )
        recommendation = Recommendation(
            grade=Grade(recommendation_str),
            confidence=Decimal(str(confidence_val)),
        )

        # LLM 리포트
        report: Optional[NarrativeReport] = None
        if llm_report_raw is not None:
            report_data = _parse_json(llm_report_raw)
            report_source_enum = ReportSource(report_data.get("source", "none"))
            report = NarrativeReport(
                trend_section=report_data["trend_section"],
                support_resistance_section=report_data["support_resistance_section"],
                entry_plan_section=report_data["entry_plan_section"],
                signal_evidence_section=report_data["signal_evidence_section"],
                risk_section=report_data["risk_section"],
                source=report_source_enum,
            )
        else:
            report_source_enum = ReportSource(llm_report_source_str or "none")

        return ChartAnalysisResult(
            symbol=symbol,
            window=window,
            interval=interval,
            snapshot_hash=snapshot_hash,
            trend=trend,
            levels=levels,
            trade_plan=trade_plan,
            patterns=patterns,
            indicator_signals=indicator_signals,
            volume_analysis=volume_analysis,
            recommendation=recommendation,
            report=report,
            report_source=report_source_enum,
            numeric_computed_at=numeric_computed_at,
            llm_computed_at=llm_computed_at,
        )


# ---------------------------------------------------------------------------
# SQL constants
# ---------------------------------------------------------------------------

_UPSERT_SQL = """
INSERT INTO chart_analysis_result (
    symbol, window, interval, snapshot_hash, recommendation, confidence,
    levels, trend, patterns, indicator_signals, volume_analysis,
    llm_report, llm_report_source, numeric_computed_at, llm_computed_at,
    created_at, updated_at
) VALUES (
    %s, %s, %s, %s, %s, %s,
    %s::jsonb, %s::jsonb, %s::jsonb, %s::jsonb, %s::jsonb,
    %s::jsonb, %s, %s, %s,
    NOW(), NOW()
)
ON CONFLICT (symbol, window, interval) DO UPDATE SET
    snapshot_hash       = EXCLUDED.snapshot_hash,
    recommendation      = EXCLUDED.recommendation,
    confidence          = EXCLUDED.confidence,
    levels              = EXCLUDED.levels,
    trend               = EXCLUDED.trend,
    patterns            = EXCLUDED.patterns,
    indicator_signals   = EXCLUDED.indicator_signals,
    volume_analysis     = EXCLUDED.volume_analysis,
    llm_report          = EXCLUDED.llm_report,
    llm_report_source   = EXCLUDED.llm_report_source,
    numeric_computed_at = EXCLUDED.numeric_computed_at,
    llm_computed_at     = EXCLUDED.llm_computed_at,
    updated_at          = NOW()
"""


def _build_order_case() -> str:
    """CASE (window, interval) WHEN ... THEN n ... 구문 생성."""
    parts = []
    for i, (w, iv) in enumerate(_WINDOW_ORDER):
        parts.append(f"WHEN ('{w}', '{iv}') THEN {i}")
    return " ".join(parts)


def _parse_json(raw: Any) -> Any:
    if isinstance(raw, (dict, list)):
        return raw
    if raw is None:
        return None
    return json.loads(raw)


def _parse_date(date_str: str):
    from datetime import date as date_cls
    return date_cls.fromisoformat(date_str)
