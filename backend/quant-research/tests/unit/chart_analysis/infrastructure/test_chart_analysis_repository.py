"""ChartAnalysisRepository 단위 테스트 — mock connect_fn 사용."""
from __future__ import annotations

import sys
import os
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', '..', '..', '..', 'src'))

import json
from datetime import date, datetime, timezone
from decimal import Decimal
from unittest.mock import MagicMock

import pytest

from chart_analysis.infrastructure.chart_analysis_repository import PostgresChartAnalysisRepository
from chart_analysis.domain.chart_analysis_result import ChartAnalysisResult
from chart_analysis.domain.value_objects import (
    Direction,
    Grade,
    LevelSet,
    Recommendation,
    ReportSource,
    Strength,
    TradePlan,
    TrendAnalysis,
    VolumeAnalysis,
)


def _make_connect_fn(rows=None, fetchone_result=None):
    """mock connect_fn.

    MagicMock의 context manager 기본 지원(__enter__/__exit__)을 활용.
    conn.__enter__() → conn 반환, cur.__enter__() → cur 반환.
    """
    mock_cur = MagicMock()
    mock_cur.__enter__ = lambda s: s
    mock_cur.__exit__ = MagicMock(return_value=False)
    if rows is not None:
        mock_cur.fetchall.return_value = rows
    # fetchone은 항상 명시적으로 세팅 (기본값 None)
    mock_cur.fetchone.return_value = fetchone_result

    mock_conn = MagicMock()
    mock_conn.__enter__ = lambda s: s
    mock_conn.__exit__ = MagicMock(return_value=False)
    mock_conn.cursor.return_value = mock_cur

    connect_fn = MagicMock()
    connect_fn.return_value = mock_conn
    return connect_fn, mock_conn, mock_cur


def _make_result() -> ChartAnalysisResult:
    """테스트용 최소 ChartAnalysisResult 생성."""
    now = datetime(2024, 1, 2, tzinfo=timezone.utc)
    trend = TrendAnalysis(
        direction=Direction.UPTREND,
        strength=Strength.STRONG,
        ma_alignment="MA20>MA60",
        adx=Decimal("25"),
        hh_ll_structure="HH",
    )
    levels = LevelSet(supports=[Decimal("68000")], resistances=[Decimal("72000")])
    trade_plan = TradePlan(
        entry_price=Decimal("70000"),
        stop_loss=Decimal("68000"),
        target_price=Decimal("72000"),
        risk_reward_ratio=Decimal("1.0"),
    )
    volume = VolumeAnalysis(trend="flat", spike_detected=False, avg_ratio=Decimal("1.0"))
    rec = Recommendation(grade=Grade.BUY, confidence=Decimal("0.7"))

    return ChartAnalysisResult(
        symbol="005930",
        window="1M",
        interval="D",
        snapshot_hash="abc123",
        trend=trend,
        levels=levels,
        trade_plan=trade_plan,
        patterns=[],
        indicator_signals=[],
        volume_analysis=volume,
        recommendation=rec,
        report=None,
        report_source=ReportSource.NONE,
        numeric_computed_at=now,
        llm_computed_at=None,
    )


def _make_db_row() -> tuple:
    """DB SELECT row — all JSONB fields as dict (psycopg이 파싱해줌)."""
    now = datetime(2024, 1, 2, tzinfo=timezone.utc)
    levels_data = {
        "supports": ["68000"],
        "resistances": ["72000"],
        "entry": "70000",
        "stop_loss": "68000",
        "target": "72000",
        "risk_reward": "1.0",
    }
    trend_data = {
        "direction": "uptrend",
        "strength": "strong",
        "ma_alignment": "MA20>MA60",
        "adx": "25",
        "hh_ll_structure": "HH",
    }
    patterns_data = []
    signals_data = []
    volume_data = {"trend": "flat", "spike_detected": False, "avg_ratio": "1.0"}

    return (
        "005930", "1M", "D", "abc123",
        "BUY", "0.7",
        levels_data, trend_data, patterns_data, signals_data, volume_data,
        None, "none",
        now, None,
    )


class TestPostgresChartAnalysisRepository:

    def test_upsert_executes_sql(self):
        """upsert() → SQL 실행 확인."""
        connect_fn, mock_conn, mock_cur = _make_connect_fn()
        repo = PostgresChartAnalysisRepository(connect_fn)
        result = _make_result()

        repo.upsert(result)

        assert mock_cur.execute.called
        # conn.commit() 호출 확인
        assert mock_conn.commit.called

    def test_find_by_symbol_returns_results(self):
        """find_by_symbol() → 1개 row 반환 시 ChartAnalysisResult 목록 길이 1."""
        row = _make_db_row()
        connect_fn, mock_conn, mock_cur = _make_connect_fn(rows=[row])
        repo = PostgresChartAnalysisRepository(connect_fn)

        results = repo.find_by_symbol("005930")

        assert len(results) == 1
        # isinstance 대신 속성으로 검증 (class identity 문제 회피)
        assert results[0].symbol == "005930"
        assert results[0].window == "1M"
        assert results[0].interval == "D"

    def test_find_one_returns_none_when_no_row(self):
        """find_one() — cursor fetchone() → None 시 None 반환."""
        connect_fn, mock_conn, mock_cur = _make_connect_fn(fetchone_result=None)
        repo = PostgresChartAnalysisRepository(connect_fn)

        result = repo.find_one("005930", "1M", "D")

        assert result is None
