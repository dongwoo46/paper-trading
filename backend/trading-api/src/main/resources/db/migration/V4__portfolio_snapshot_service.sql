CREATE TABLE IF NOT EXISTS daily_balances
(
    id                 BIGSERIAL PRIMARY KEY,
    account_id         BIGINT         NOT NULL,
    business_date      DATE           NOT NULL,
    cash_balance       NUMERIC(20, 4) NOT NULL,
    stock_market_value NUMERIC(20, 4) NOT NULL,
    total_asset_value  NUMERIC(20, 4) NOT NULL,
    pnl_amount         NUMERIC(20, 4) NOT NULL,
    pnl_rate           NUMERIC(10, 6) NOT NULL,
    created_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_daily_balances_account_id'
        ) THEN
            ALTER TABLE daily_balances
                ADD CONSTRAINT fk_daily_balances_account_id
                    FOREIGN KEY (account_id) REFERENCES accounts (id);
        END IF;
    END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_daily_balances_account_date
    ON daily_balances (account_id, business_date);

CREATE INDEX IF NOT EXISTS idx_daily_balances_account_date_desc
    ON daily_balances (account_id, business_date DESC);

CREATE TABLE IF NOT EXISTS portfolio_snapshots
(
    id             BIGSERIAL PRIMARY KEY,
    account_id     BIGINT         NOT NULL,
    business_date  DATE           NOT NULL,
    ticker         VARCHAR(20)    NOT NULL,
    quantity       NUMERIC(20, 8) NOT NULL,
    avg_buy_price  NUMERIC(20, 4) NOT NULL,
    close_price    NUMERIC(20, 4) NOT NULL,
    market_value   NUMERIC(20, 4) NOT NULL,
    weight         NUMERIC(10, 6) NOT NULL,
    unrealized_pnl NUMERIC(20, 4) NOT NULL,
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_portfolio_snapshots_account_id'
        ) THEN
            ALTER TABLE portfolio_snapshots
                ADD CONSTRAINT fk_portfolio_snapshots_account_id
                    FOREIGN KEY (account_id) REFERENCES accounts (id);
        END IF;
    END
$$;

CREATE UNIQUE INDEX IF NOT EXISTS uk_portfolio_snapshots_account_date_ticker
    ON portfolio_snapshots (account_id, business_date, ticker);

CREATE INDEX IF NOT EXISTS idx_portfolio_snapshots_account_date
    ON portfolio_snapshots (account_id, business_date);