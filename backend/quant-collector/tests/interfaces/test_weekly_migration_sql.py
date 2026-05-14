from __future__ import annotations

from pathlib import Path


def test_weekly_migration_has_unique_constraint_and_indexes():
    migration = Path(__file__).resolve().parents[3] / "collector-api" / "src" / "main" / "resources" / "db" / "migration" / "V13__create_market_weekly_ohlcv.sql"
    sql = migration.read_text(encoding="utf-8")

    assert "UNIQUE (source, symbol, trade_date)" in sql
    assert "idx_market_weekly_ohlcv_symbol_date" in sql
    assert "idx_market_weekly_ohlcv_source_date" in sql
    assert "idx_market_weekly_ohlcv_market_date" in sql
    assert "idx_market_weekly_ohlcv_provider" in sql
