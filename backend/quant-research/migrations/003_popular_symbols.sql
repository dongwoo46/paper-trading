CREATE TABLE IF NOT EXISTS popular_symbols (
    id         SERIAL        PRIMARY KEY,
    symbol     VARCHAR(20)   NOT NULL UNIQUE,
    market     VARCHAR(10),
    rank       INT,
    market_cap NUMERIC(20,2),
    avg_volume NUMERIC(20,2),
    score      NUMERIC(10,6),
    updated_at TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_popular_symbols_market_rank
    ON popular_symbols (market, rank);
