ALTER TABLE pykrx_symbol_catalog
    ADD COLUMN IF NOT EXISTS fetched_until_date DATE,
    ADD COLUMN IF NOT EXISTS last_collected_at TIMESTAMP;

ALTER TABLE yfinance_symbol_catalog
    ADD COLUMN IF NOT EXISTS fetched_until_date DATE,
    ADD COLUMN IF NOT EXISTS last_collected_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_pykrx_symbol_catalog_fetched_until_date ON pykrx_symbol_catalog (fetched_until_date);
CREATE INDEX IF NOT EXISTS idx_yfinance_symbol_catalog_fetched_until_date ON yfinance_symbol_catalog (fetched_until_date);

