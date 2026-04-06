CREATE TABLE IF NOT EXISTS market_daily_ohlcv (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    market VARCHAR(32) NOT NULL,
    trade_date DATE NOT NULL,
    open_price NUMERIC(18, 6) NOT NULL,
    high_price NUMERIC(18, 6) NOT NULL,
    low_price NUMERIC(18, 6) NOT NULL,
    close_price NUMERIC(18, 6) NOT NULL,
    volume NUMERIC(20, 4) NOT NULL,
    adj_close_price NUMERIC(18, 6),
    provider VARCHAR(32) NOT NULL DEFAULT 'unknown',
    interval VARCHAR(8) NOT NULL DEFAULT '1d',
    is_adjusted BOOLEAN NOT NULL DEFAULT FALSE,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source, symbol, trade_date)
);

CREATE INDEX IF NOT EXISTS idx_market_daily_ohlcv_symbol_date ON market_daily_ohlcv (symbol, trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_market_daily_ohlcv_source_date ON market_daily_ohlcv (source, trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_market_daily_ohlcv_market_date ON market_daily_ohlcv (market, trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_market_daily_ohlcv_provider ON market_daily_ohlcv (provider);
