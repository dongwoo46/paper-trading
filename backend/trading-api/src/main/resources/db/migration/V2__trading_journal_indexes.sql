CREATE INDEX IF NOT EXISTS idx_trading_journals_account_created_at
    ON trading_journals (account_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_trading_journals_account_ticker_created_at
    ON trading_journals (account_id, ticker, created_at DESC);
