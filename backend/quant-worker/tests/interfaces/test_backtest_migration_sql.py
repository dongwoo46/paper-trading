from pathlib import Path

MIGRATION = (
    Path(__file__).parents[2]
    / "src"
    / "migrations"
    / "V3__create_backtest_runs.sql"
)


def test_backtest_run_migration_is_idempotent_and_stores_summary_only() -> None:
    sql = MIGRATION.read_text(encoding="utf-8").lower()

    assert "create table if not exists backtest_runs" in sql
    assert "create index if not exists" in sql
    assert "strategy_json" in sql and "jsonb" in sql
    assert "initial_cash" in sql and "numeric" in sql
    assert "cost_profile" in sql
    assert "cost_profile        varchar" in sql
    assert "cost_profile        varchar" in sql and "not null" in sql
    assert "commission_bps" not in sql
    assert "slippage_bps" not in sql
    assert "sell_tax_bps" not in sql
    assert "total_return" in sql and "max_drawdown" in sql
    assert "sharpe" in sql and "calmar" in sql and "win_rate" in sql
    assert "timestamptz" not in sql
    assert "created_at          timestamp" in sql
    assert "started_at          timestamp" in sql
    assert "finished_at         timestamp" in sql
    assert "create table" not in sql.replace("create table if not exists backtest_runs", "")
