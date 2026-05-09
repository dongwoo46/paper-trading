CREATE TABLE IF NOT EXISTS market_microstructure_source (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    as_of TIMESTAMPTZ NOT NULL,
    bid_price NUMERIC(18, 6),
    ask_price NUMERIC(18, 6),
    bid_size NUMERIC(20, 4),
    ask_size NUMERIC(20, 4),
    spread NUMERIC(18, 6),
    bid_ask_available BOOLEAN NOT NULL DEFAULT TRUE,
    depth_available BOOLEAN NOT NULL DEFAULT TRUE,
    availability_note VARCHAR(64),
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source, symbol, as_of)
);

CREATE TABLE IF NOT EXISTS market_session_ohlcv_source (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    session VARCHAR(16) NOT NULL,
    as_of TIMESTAMPTZ NOT NULL,
    open_price NUMERIC(18, 6) NOT NULL,
    high_price NUMERIC(18, 6) NOT NULL,
    low_price NUMERIC(18, 6) NOT NULL,
    close_price NUMERIC(18, 6) NOT NULL,
    volume NUMERIC(20, 4) NOT NULL,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source, symbol, session, as_of),
    CONSTRAINT chk_market_session_ohlcv_source_session
        CHECK (session IN ('regular', 'pre', 'after'))
) PARTITION BY RANGE (as_of);

CREATE TABLE IF NOT EXISTS market_relative_strength_source (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    benchmark_symbol VARCHAR(32) NOT NULL,
    as_of TIMESTAMPTZ NOT NULL,
    symbol_return NUMERIC(18, 8) NOT NULL,
    benchmark_return NUMERIC(18, 8) NOT NULL,
    relative_strength NUMERIC(18, 8),
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source, symbol, benchmark_symbol, as_of)
);

CREATE TABLE IF NOT EXISTS market_flow_alternative_source (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    as_of TIMESTAMPTZ NOT NULL,
    short_interest NUMERIC(20, 4),
    days_to_cover NUMERIC(18, 6),
    shares_outstanding NUMERIC(24, 4),
    float_shares NUMERIC(24, 4),
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source, symbol, as_of)
);

CREATE TABLE IF NOT EXISTS market_symbol_metadata (
    id BIGSERIAL PRIMARY KEY,
    source VARCHAR(16) NOT NULL,
    symbol VARCHAR(32) NOT NULL,
    as_of TIMESTAMPTZ NOT NULL,
    exchange VARCHAR(32),
    currency VARCHAR(16),
    timezone VARCHAR(64),
    market_cap NUMERIC(24, 4),
    free_float NUMERIC(24, 4),
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (source, symbol, as_of)
);

CREATE INDEX IF NOT EXISTS idx_market_microstructure_symbol_asof ON market_microstructure_source (symbol, as_of DESC);
CREATE INDEX IF NOT EXISTS idx_market_microstructure_source_asof ON market_microstructure_source (source, as_of DESC);
CREATE INDEX IF NOT EXISTS idx_market_session_ohlcv_symbol_session_asof ON market_session_ohlcv_source (symbol, session, as_of DESC);
CREATE INDEX IF NOT EXISTS idx_market_relative_strength_symbol_benchmark_asof ON market_relative_strength_source (symbol, benchmark_symbol, as_of DESC);
CREATE INDEX IF NOT EXISTS idx_market_flow_alternative_symbol_asof ON market_flow_alternative_source (symbol, as_of DESC);
CREATE INDEX IF NOT EXISTS idx_market_symbol_metadata_symbol_asof ON market_symbol_metadata (symbol, as_of DESC);
