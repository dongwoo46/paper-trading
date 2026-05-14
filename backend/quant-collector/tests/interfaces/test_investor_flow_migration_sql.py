from __future__ import annotations

from pathlib import Path


def test_investor_flow_migration_has_all_four_tables():
    migration = (
        Path(__file__).resolve().parents[3]
        / "quant-worker"
        / "src"
        / "migrations"
        / "V1__create_investor_flow_tables.sql"
    )
    sql = migration.read_text(encoding="utf-8")

    assert "CREATE TABLE IF NOT EXISTS investor_flow" in sql
    assert "CREATE TABLE IF NOT EXISTS short_selling" in sql
    assert "CREATE TABLE IF NOT EXISTS program_trading" in sql
    assert "CREATE TABLE IF NOT EXISTS foreign_holding" in sql


def test_investor_flow_migration_unique_constraints():
    migration = (
        Path(__file__).resolve().parents[3]
        / "quant-worker"
        / "src"
        / "migrations"
        / "V1__create_investor_flow_tables.sql"
    )
    sql = migration.read_text(encoding="utf-8")

    assert "UNIQUE (trade_date, symbol, market)" in sql


def test_investor_flow_migration_indexes():
    migration = (
        Path(__file__).resolve().parents[3]
        / "quant-worker"
        / "src"
        / "migrations"
        / "V1__create_investor_flow_tables.sql"
    )
    sql = migration.read_text(encoding="utf-8")

    assert "idx_investor_flow_symbol_date" in sql
    assert "idx_investor_flow_date_market" in sql
    assert "idx_short_selling_symbol_date" in sql
    assert "idx_short_selling_date_market" in sql
    assert "idx_program_trading_symbol_date" in sql
    assert "idx_program_trading_date_market" in sql
    assert "idx_foreign_holding_symbol_date" in sql
    assert "idx_foreign_holding_date_market" in sql


def test_investor_flow_migration_numeric_columns():
    migration = (
        Path(__file__).resolve().parents[3]
        / "quant-worker"
        / "src"
        / "migrations"
        / "V1__create_investor_flow_tables.sql"
    )
    sql = migration.read_text(encoding="utf-8")

    # Amount columns must be NUMERIC(20,0)
    assert "NUMERIC(20,0)" in sql
    # Ratio columns must be NUMERIC(8,4)
    assert "NUMERIC(8,4)" in sql
    # collected_at must be TIMESTAMPTZ with DEFAULT NOW()
    assert "TIMESTAMPTZ NOT NULL DEFAULT NOW()" in sql
