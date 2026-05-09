from __future__ import annotations

from pathlib import Path


def test_indicator_source_migration_has_tables_indexes_and_partition_hints():
    migration = Path(__file__).resolve().parents[3] / "collector-api" / "src" / "main" / "resources" / "db" / "migration" / "V15__create_indicator_source_tables.sql"
    sql = migration.read_text(encoding="utf-8")
    assert "market_microstructure_source" in sql
    assert "market_session_ohlcv_source" in sql
    assert "market_relative_strength_source" in sql
    assert "market_flow_alternative_source" in sql
    assert "market_symbol_metadata" in sql
    assert "UNIQUE (source, symbol, as_of)" in sql
    assert "PARTITION BY RANGE (as_of)" in sql
    assert "CHECK (session IN ('regular', 'pre', 'after'))" in sql
    assert "benchmark_symbol" in sql
    assert "relative_strength NUMERIC(18, 8)," in sql
    assert "bid_ask_available BOOLEAN NOT NULL DEFAULT TRUE" in sql
    assert "depth_available BOOLEAN NOT NULL DEFAULT TRUE" in sql
