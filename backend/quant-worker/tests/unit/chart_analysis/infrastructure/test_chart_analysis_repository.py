"""단위 테스트: PostgresChartAnalysisRepository."""
from __future__ import annotations

import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..", "..", "..", "src"))

import json
from datetime import datetime, timezone
from decimal import Decimal
from typing import Any
from unittest.mock import MagicMock, call

import pytest

from chart_analysis.infrastructure.chart_analysis_repository import (
    DecimalJSONEncoder,
    PostgresChartAnalysisRepository,
)
from chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
from chart_analysis.domain.value_objects import (
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
# Fixture helpers
# ---------------------------------------------------------------------------

def _make_result(
    symbol: str = "005930",
    window: str = "1M",
    interval: str = "D",
) -> ChartAnalysisResult:
    trend = TrendAnalysis(
        direction=Direction.UPTREND,
        strength=Strength.MEDIUM,
        ma_alignment="MA20>MA60",
        adx=Decimal("23.4"),
        hh_ll_structure="HH",
    )
    levels = LevelSet(
        supports=[Decimal("68500"), Decimal("67200")],
        resistances=[Decimal("71200"), Decimal("72500")],
    )
    trade_plan = TradePlan(
        entry_price=Decimal("69400"),
        stop_loss=Decimal("67100"),
        target_price=Decimal("72500"),
        risk_reward_ratio=Decimal("1.35"),
    )
    patterns: list[CandlePattern] = []
    indicator_signals = [
        IndicatorSignal(name="RSI", value=Decimal("58.2"), interpretation="neutral_bullish"),
    ]
    volume_analysis = VolumeAnalysis(
        trend="increasing",
        spike_detected=True,
        avg_ratio=Decimal("1.42"),
    )
    recommendation = Recommendation(grade=Grade.BUY, confidence=Decimal("0.612"))

    return ChartAnalysisResult(
        symbol=symbol,
        window=window,
        interval=interval,
        snapshot_hash="abc123",
        trend=trend,
        levels=levels,
        trade_plan=trade_plan,
        patterns=patterns,
        indicator_signals=indicator_signals,
        volume_analysis=volume_analysis,
        recommendation=recommendation,
        report=None,
        report_source=ReportSource.NONE,
        numeric_computed_at=datetime(2026, 5, 11, 8, 0, 0, tzinfo=timezone.utc),
        llm_computed_at=None,
    )


def _make_repo() -> tuple[PostgresChartAnalysisRepository, MagicMock]:
    mock_conn = MagicMock()
    mock_cursor = MagicMock()
    mock_cursor.__enter__ = MagicMock(return_value=mock_cursor)
    mock_cursor.__exit__ = MagicMock(return_value=False)
    mock_conn.cursor.return_value = mock_cursor
    mock_conn.__enter__ = MagicMock(return_value=mock_conn)
    mock_conn.__exit__ = MagicMock(return_value=False)

    repo = PostgresChartAnalysisRepository.__new__(PostgresChartAnalysisRepository)
    repo._connect = MagicMock(return_value=mock_conn)
    return repo, mock_cursor


# ---------------------------------------------------------------------------
# DecimalJSONEncoder 테스트
# ---------------------------------------------------------------------------

class TestDecimalJSONEncoder:
    """Decimal → str 직렬화 검증."""

    def test_decimal_serialized_to_string(self):
        data = {"price": Decimal("69400.50"), "ratio": Decimal("1.35")}
        encoded = json.dumps(data, cls=DecimalJSONEncoder)
        decoded = json.loads(encoded)
        assert decoded["price"] == "69400.50"
        assert decoded["ratio"] == "1.35"

    def test_non_decimal_values_pass_through(self):
        data = {"name": "005930", "count": 7, "flag": True}
        encoded = json.dumps(data, cls=DecimalJSONEncoder)
        decoded = json.loads(encoded)
        assert decoded == data

    def test_nested_decimal(self):
        data = {"supports": [Decimal("68500"), Decimal("67200")]}
        encoded = json.dumps(data, cls=DecimalJSONEncoder)
        decoded = json.loads(encoded)
        assert decoded["supports"] == ["68500", "67200"]


# ---------------------------------------------------------------------------
# upsert 테스트
# ---------------------------------------------------------------------------

class TestUpsert:
    """upsert(result) — INSERT ... ON CONFLICT UPDATE 검증."""

    def test_upsert_executes_sql(self):
        repo, mock_cursor = _make_repo()
        result = _make_result()
        repo.upsert(result)

        assert mock_cursor.execute.called, "upsert가 execute를 호출해야 합니다."

    def test_upsert_uses_on_conflict(self):
        repo, mock_cursor = _make_repo()
        result = _make_result()
        repo.upsert(result)

        query: str = mock_cursor.execute.call_args[0][0].upper()
        assert "ON CONFLICT" in query, "ON CONFLICT 구문이 없습니다."
        assert "DO UPDATE" in query, "DO UPDATE 구문이 없습니다."

    def test_upsert_params_include_symbol_window_interval(self):
        repo, mock_cursor = _make_repo()
        result = _make_result(symbol="035720", window="3M", interval="D")
        repo.upsert(result)

        params = mock_cursor.execute.call_args[0][1]
        # 파라미터 튜플에 symbol, window, interval 포함
        assert "035720" in params
        assert "3M" in params
        assert "D" in params

    def test_upsert_jsonb_confidence_as_decimal(self):
        """confidence 파라미터가 str/Decimal이고 float가 아님을 검증."""
        repo, mock_cursor = _make_repo()
        result = _make_result()
        repo.upsert(result)

        params = mock_cursor.execute.call_args[0][1]
        # confidence 값 찾기 (Decimal("0.612"))
        for p in params:
            if isinstance(p, float):
                pytest.fail(f"float 타입 파라미터 발견: {p} — Decimal 또는 str만 허용됩니다.")


# ---------------------------------------------------------------------------
# find_by_symbol 테스트
# ---------------------------------------------------------------------------

def _make_db_row(window: str, interval: str) -> tuple:
    """DB row mock — fetchall 반환값 모사."""
    return (
        "005930", window, interval,
        "hash123", "BUY", Decimal("0.612"),
        json.dumps({"supports": ["68500"], "resistances": ["71200"],
                    "entry": "69400", "stop_loss": "67100",
                    "target": "72500", "risk_reward": "1.35"}),
        json.dumps({"direction": "uptrend", "strength": "medium",
                    "ma_alignment": "MA20>MA60", "adx": "23.4", "hh_ll_structure": "HH"}),
        json.dumps([]),
        json.dumps([{"name": "RSI", "value": "58.2", "interpretation": "neutral_bullish"}]),
        json.dumps({"trend": "increasing", "spike_detected": True, "avg_ratio": "1.42"}),
        None,  # llm_report
        "none",  # llm_report_source
        datetime(2026, 5, 11, 8, 0, 0, tzinfo=timezone.utc),
        None,  # llm_computed_at
    )


class TestFindBySymbol:
    """find_by_symbol(symbol) → 7 windows 반환 순서 검증."""

    _EXPECTED_ORDER = [
        ("1M", "D"), ("3M", "D"), ("6M", "D"), ("1Y", "D"),
        ("1Y", "W"), ("2Y", "W"), ("MAX", "W"),
    ]

    def test_returns_seven_windows(self):
        """7 윈도우를 모두 반환한다."""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.__enter__ = MagicMock(return_value=mock_cursor)
        mock_cursor.__exit__ = MagicMock(return_value=False)
        mock_cursor.fetchall.return_value = [
            _make_db_row(window, interval)
            for window, interval in self._EXPECTED_ORDER
        ]
        mock_conn.cursor.return_value = mock_cursor
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)

        repo = PostgresChartAnalysisRepository.__new__(PostgresChartAnalysisRepository)
        repo._connect = MagicMock(return_value=mock_conn)

        results = repo.find_by_symbol("005930")
        assert len(results) == 7

    def test_result_window_interval_values(self):
        """반환된 결과의 window/interval 값 확인."""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.__enter__ = MagicMock(return_value=mock_cursor)
        mock_cursor.__exit__ = MagicMock(return_value=False)
        mock_cursor.fetchall.return_value = [
            _make_db_row(window, interval)
            for window, interval in self._EXPECTED_ORDER
        ]
        mock_conn.cursor.return_value = mock_cursor
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)

        repo = PostgresChartAnalysisRepository.__new__(PostgresChartAnalysisRepository)
        repo._connect = MagicMock(return_value=mock_conn)

        results = repo.find_by_symbol("005930")
        pairs = [(r.window, r.interval) for r in results]
        assert pairs == self._EXPECTED_ORDER

    def test_confidence_is_decimal(self):
        """confidence 필드가 Decimal 타입인지 확인."""
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.__enter__ = MagicMock(return_value=mock_cursor)
        mock_cursor.__exit__ = MagicMock(return_value=False)
        mock_cursor.fetchall.return_value = [_make_db_row("1M", "D")]
        mock_conn.cursor.return_value = mock_cursor
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)

        repo = PostgresChartAnalysisRepository.__new__(PostgresChartAnalysisRepository)
        repo._connect = MagicMock(return_value=mock_conn)

        results = repo.find_by_symbol("005930")
        assert isinstance(results[0].recommendation.confidence, Decimal)

    def test_empty_result(self):
        mock_conn = MagicMock()
        mock_cursor = MagicMock()
        mock_cursor.__enter__ = MagicMock(return_value=mock_cursor)
        mock_cursor.__exit__ = MagicMock(return_value=False)
        mock_cursor.fetchall.return_value = []
        mock_conn.cursor.return_value = mock_cursor
        mock_conn.__enter__ = MagicMock(return_value=mock_conn)
        mock_conn.__exit__ = MagicMock(return_value=False)

        repo = PostgresChartAnalysisRepository.__new__(PostgresChartAnalysisRepository)
        repo._connect = MagicMock(return_value=mock_conn)

        results = repo.find_by_symbol("UNKNOWN")
        assert results == []
