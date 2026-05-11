"""Integration test: V2 마이그레이션 테이블 검증.

마커 `integration` — 실제 PostgreSQL 연결 필요. CI에서 skip.
실행: pytest -m integration tests/integration/chart_analysis/test_migration_v2.py
"""
import os

import psycopg
import pytest


@pytest.fixture(scope="module")
def db_conn():
    """테스트용 DB 연결 — 환경변수에서 DSN 읽기."""
    dsn = os.environ.get(
        "TEST_DATABASE_URL",
        "postgresql://postgres:postgres@localhost:5432/quant_test",
    )
    with psycopg.connect(dsn) as conn:
        yield conn


def _table_exists(conn, table_name: str) -> bool:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT EXISTS (
                SELECT 1 FROM information_schema.tables
                WHERE table_schema = 'public'
                AND table_name = %s
            )
            """,
            (table_name,),
        )
        return cur.fetchone()[0]


def _column_names(conn, table_name: str) -> set[str]:
    with conn.cursor() as cur:
        cur.execute(
            """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'public'
              AND table_name = %s
            """,
            (table_name,),
        )
        return {row[0] for row in cur.fetchall()}


@pytest.mark.integration
class TestMigrationV2:
    """V2 마이그레이션으로 생성된 테이블 3개와 컬럼 검증."""

    def test_chart_analysis_result_table_exists(self, db_conn):
        assert _table_exists(db_conn, "chart_analysis_result"), \
            "chart_analysis_result 테이블이 없습니다."

    def test_chart_analysis_result_columns(self, db_conn):
        expected = {
            "symbol", "window", "interval", "snapshot_hash",
            "recommendation", "confidence",
            "levels", "trend", "patterns", "indicator_signals", "volume_analysis",
            "llm_report", "llm_report_source",
            "numeric_computed_at", "llm_computed_at",
            "created_at", "updated_at",
        }
        actual = _column_names(db_conn, "chart_analysis_result")
        missing = expected - actual
        assert not missing, f"누락된 컬럼: {missing}"

    def test_analysis_request_queue_table_exists(self, db_conn):
        assert _table_exists(db_conn, "analysis_request_queue"), \
            "analysis_request_queue 테이블이 없습니다."

    def test_analysis_request_queue_columns(self, db_conn):
        expected = {
            "id", "symbol", "window", "interval",
            "status", "requested_count", "requested_at", "processed_at",
        }
        actual = _column_names(db_conn, "analysis_request_queue")
        missing = expected - actual
        assert not missing, f"누락된 컬럼: {missing}"

    def test_popular_symbols_table_exists(self, db_conn):
        assert _table_exists(db_conn, "popular_symbols"), \
            "popular_symbols 테이블이 없습니다."

    def test_popular_symbols_columns(self, db_conn):
        expected = {"symbol", "market", "rank", "market_cap", "avg_volume", "score", "updated_at"}
        actual = _column_names(db_conn, "popular_symbols")
        missing = expected - actual
        assert not missing, f"누락된 컬럼: {missing}"
