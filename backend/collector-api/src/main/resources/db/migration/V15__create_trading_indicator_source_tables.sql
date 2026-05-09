CREATE TABLE IF NOT EXISTS market_microstructure (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    trade_date DATE NOT NULL,
    spread_bps NUMERIC(18, 6) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source, symbol, trade_date)
);

CREATE TABLE IF NOT EXISTS market_session_ohlcv (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    trade_date DATE NOT NULL,
    open_price NUMERIC(18, 6) NOT NULL,
    high_price NUMERIC(18, 6) NOT NULL,
    low_price NUMERIC(18, 6) NOT NULL,
    close_price NUMERIC(18, 6) NOT NULL,
    volume NUMERIC(20, 4) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source, symbol, trade_date)
);

CREATE TABLE IF NOT EXISTS market_relative_strength (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    trade_date DATE NOT NULL,
    close_price NUMERIC(18, 6) NOT NULL,
    volume NUMERIC(20, 4) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source, symbol, trade_date)
);

CREATE TABLE IF NOT EXISTS market_alternative_flow (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    trade_date DATE NOT NULL,
    retail_flow NUMERIC(20, 4) NOT NULL,
    institution_flow NUMERIC(20, 4) NOT NULL,
    foreign_flow NUMERIC(20, 4) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source, symbol, trade_date)
);

CREATE TABLE IF NOT EXISTS market_symbol_metadata (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    market VARCHAR(32) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    provider VARCHAR(32) NOT NULL,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source, symbol)
);

CREATE INDEX IF NOT EXISTS idx_market_microstructure_symbol_date ON market_microstructure (symbol, trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_market_session_ohlcv_symbol_date ON market_session_ohlcv (symbol, trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_market_relative_strength_symbol_date ON market_relative_strength (symbol, trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_market_alternative_flow_symbol_date ON market_alternative_flow (symbol, trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_market_symbol_metadata_symbol ON market_symbol_metadata (symbol);

-- Partition-ready index baseline for future date-range partition routing.
CREATE INDEX IF NOT EXISTS idx_market_microstructure_trade_date ON market_microstructure (trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_market_session_ohlcv_trade_date ON market_session_ohlcv (trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_market_relative_strength_trade_date ON market_relative_strength (trade_date DESC);
CREATE INDEX IF NOT EXISTS idx_market_alternative_flow_trade_date ON market_alternative_flow (trade_date DESC);
