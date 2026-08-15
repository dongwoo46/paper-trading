-- V3__create_backtest_runs.sql
-- Backtest run metadata and normalized summary metrics. Detailed artifacts stay on disk.

CREATE TABLE IF NOT EXISTS backtest_runs (
    run_id              UUID            PRIMARY KEY,
    status              VARCHAR(20)     NOT NULL,
    market              VARCHAR(2)      NOT NULL,
    symbol              VARCHAR(20)     NOT NULL,
    resolution          VARCHAR(10)     NOT NULL,
    start_date          DATE            NOT NULL,
    end_date            DATE            NOT NULL,
    initial_cash        NUMERIC(30,10)  NOT NULL,
    currency            VARCHAR(3)      NOT NULL,
    cost_profile        VARCHAR(32)     NOT NULL,
    strategy_json       JSONB           NOT NULL,
    artifact_path       TEXT,
    error_message       TEXT,
    total_return        NUMERIC(30,12),
    max_drawdown        NUMERIC(30,12),
    annualized_return   NUMERIC(30,12),
    sharpe              NUMERIC(30,12),
    calmar              NUMERIC(30,12),
    win_rate            NUMERIC(30,12),
    total_trades        BIGINT,
    created_at          TIMESTAMP       NOT NULL,
    started_at          TIMESTAMP,
    finished_at         TIMESTAMP,

    CONSTRAINT ck_backtest_runs_status
        CHECK (status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'CANCELED')),
    CONSTRAINT ck_backtest_runs_market_currency
        CHECK ((market = 'KR' AND currency = 'KRW') OR (market = 'US' AND currency = 'USD')),
    CONSTRAINT ck_backtest_runs_cost_profile
        CHECK ((market = 'KR' AND cost_profile = 'KR_DEFAULT_V1') OR
               (market = 'US' AND cost_profile = 'US_DEFAULT_V1')),
    CONSTRAINT ck_backtest_runs_resolution
        CHECK (resolution = 'daily'),
    CONSTRAINT ck_backtest_runs_date_range
        CHECK (start_date <= end_date),
    CONSTRAINT ck_backtest_runs_initial_cash
        CHECK (initial_cash > 0)
);

CREATE INDEX IF NOT EXISTS idx_backtest_runs_status_created_at
    ON backtest_runs (status, created_at);

CREATE INDEX IF NOT EXISTS idx_backtest_runs_market_symbol_created_at
    ON backtest_runs (market, symbol, created_at DESC);
