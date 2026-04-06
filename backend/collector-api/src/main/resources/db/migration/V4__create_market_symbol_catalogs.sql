CREATE TABLE IF NOT EXISTS pykrx_symbol_catalog (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    market VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (symbol)
);

CREATE INDEX IF NOT EXISTS idx_pykrx_symbol_catalog_market ON pykrx_symbol_catalog (market);
CREATE INDEX IF NOT EXISTS idx_pykrx_symbol_catalog_enabled ON pykrx_symbol_catalog (enabled);

INSERT INTO pykrx_symbol_catalog (symbol, name, market, enabled, is_default) VALUES
    ('005930', 'Samsung Electronics', 'KOSPI', TRUE, TRUE),
    ('000660', 'SK hynix', 'KOSPI', TRUE, TRUE),
    ('035420', 'NAVER', 'KOSPI', TRUE, TRUE),
    ('035720', 'Kakao', 'KOSPI', FALSE, FALSE),
    ('207940', 'Samsung Biologics', 'KOSPI', FALSE, FALSE),
    ('051910', 'LG Chem', 'KOSPI', FALSE, FALSE),
    ('373220', 'LG Energy Solution', 'KOSPI', FALSE, FALSE),
    ('247540', 'EcoPro BM', 'KOSDAQ', FALSE, FALSE),
    ('086520', 'EcoPro', 'KOSDAQ', FALSE, FALSE),
    ('293490', 'Kakao Games', 'KOSDAQ', FALSE, FALSE),
    ('122630', 'KODEX Leverage', 'ETF', FALSE, FALSE),
    ('252670', 'KODEX 200 Futures Inverse 2X', 'ETF', FALSE, FALSE)
ON CONFLICT (symbol) DO UPDATE SET
    name = EXCLUDED.name,
    market = EXCLUDED.market,
    enabled = EXCLUDED.enabled,
    is_default = EXCLUDED.is_default,
    updated_at = CURRENT_TIMESTAMP;

CREATE TABLE IF NOT EXISTS yfinance_symbol_catalog (
    id BIGSERIAL PRIMARY KEY,
    ticker VARCHAR(32) NOT NULL,
    name VARCHAR(128) NOT NULL,
    market VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (ticker)
);

CREATE INDEX IF NOT EXISTS idx_yfinance_symbol_catalog_market ON yfinance_symbol_catalog (market);
CREATE INDEX IF NOT EXISTS idx_yfinance_symbol_catalog_enabled ON yfinance_symbol_catalog (enabled);

INSERT INTO yfinance_symbol_catalog (ticker, name, market, enabled, is_default) VALUES
    ('SPY', 'SPDR S&P 500 ETF Trust', 'US', TRUE, TRUE),
    ('QQQ', 'Invesco QQQ Trust', 'US', TRUE, TRUE),
    ('DIA', 'SPDR Dow Jones Industrial Average ETF Trust', 'US', FALSE, FALSE),
    ('IWM', 'iShares Russell 2000 ETF', 'US', FALSE, FALSE),
    ('AAPL', 'Apple Inc.', 'US', TRUE, TRUE),
    ('MSFT', 'Microsoft Corporation', 'US', TRUE, TRUE),
    ('NVDA', 'NVIDIA Corporation', 'US', TRUE, TRUE),
    ('AMZN', 'Amazon.com, Inc.', 'US', FALSE, FALSE),
    ('GOOGL', 'Alphabet Inc.', 'US', FALSE, FALSE),
    ('META', 'Meta Platforms, Inc.', 'US', FALSE, FALSE),
    ('TSLA', 'Tesla, Inc.', 'US', FALSE, FALSE),
    ('^GSPC', 'S&P 500 Index', 'INDEX', FALSE, FALSE),
    ('^IXIC', 'NASDAQ Composite Index', 'INDEX', FALSE, FALSE),
    ('^VIX', 'CBOE Volatility Index', 'INDEX', FALSE, FALSE)
ON CONFLICT (ticker) DO UPDATE SET
    name = EXCLUDED.name,
    market = EXCLUDED.market,
    enabled = EXCLUDED.enabled,
    is_default = EXCLUDED.is_default,
    updated_at = CURRENT_TIMESTAMP;

