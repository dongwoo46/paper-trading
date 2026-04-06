CREATE TABLE IF NOT EXISTS fred_series_catalog (
    id BIGSERIAL PRIMARY KEY,
    series_id VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    frequency VARCHAR(32) NOT NULL,
    units VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (series_id)
);

CREATE INDEX IF NOT EXISTS idx_fred_series_catalog_category ON fred_series_catalog (category);
CREATE INDEX IF NOT EXISTS idx_fred_series_catalog_enabled ON fred_series_catalog (enabled);

INSERT INTO fred_series_catalog (series_id, title, category, frequency, units, enabled, is_default) VALUES
    ('CPIAUCSL', 'Consumer Price Index for All Urban Consumers: All Items', 'inflation', 'monthly', 'index_1982_84_100', TRUE, TRUE),
    ('PCEPI', 'Personal Consumption Expenditures: Chain-type Price Index', 'inflation', 'monthly', 'index_2017_100', TRUE, TRUE),
    ('FEDFUNDS', 'Federal Funds Effective Rate', 'rates', 'monthly', 'percent', TRUE, TRUE),
    ('DFF', 'Federal Funds Effective Rate (Daily)', 'rates', 'daily', 'percent', TRUE, TRUE),
    ('GS10', '10-Year Treasury Constant Maturity Rate', 'rates', 'monthly', 'percent', TRUE, TRUE),
    ('GS2', '2-Year Treasury Constant Maturity Rate', 'rates', 'monthly', 'percent', TRUE, TRUE),
    ('UNRATE', 'Unemployment Rate', 'labor', 'monthly', 'percent', TRUE, TRUE),
    ('PAYEMS', 'All Employees, Total Nonfarm', 'labor', 'monthly', 'thousands', TRUE, TRUE),
    ('GDPC1', 'Real Gross Domestic Product', 'growth', 'quarterly', 'billions_chained_2017_dollars', TRUE, TRUE),
    ('INDPRO', 'Industrial Production: Total Index', 'growth', 'monthly', 'index_2017_100', TRUE, TRUE),
    ('VIXCLS', 'CBOE Volatility Index: VIX', 'risk', 'daily', 'index', TRUE, TRUE),
    ('M2SL', 'M2 Money Stock', 'liquidity', 'monthly', 'billions_of_dollars', FALSE, FALSE),
    ('T10Y2Y', '10-Year Treasury Constant Maturity Minus 2-Year Treasury Constant Maturity', 'rates', 'daily', 'percent', FALSE, FALSE),
    ('BAA10Y', 'Moody Baa Corporate Bond Yield Minus 10-Year Treasury Yield', 'credit', 'daily', 'percent', FALSE, FALSE),
    ('DGS1MO', '1-Month Treasury Constant Maturity Rate', 'rates', 'daily', 'percent', FALSE, FALSE),
    ('DGS3MO', '3-Month Treasury Constant Maturity Rate', 'rates', 'daily', 'percent', FALSE, FALSE),
    ('DEXKOUS', 'U.S. / Korea Foreign Exchange Rate', 'fx', 'daily', 'krw_per_usd', FALSE, FALSE),
    ('DEXUSEU', 'U.S. / Euro Foreign Exchange Rate', 'fx', 'daily', 'usd_per_eur', FALSE, FALSE),
    ('HOUST', 'Housing Starts: Total', 'housing', 'monthly', 'thousands', FALSE, FALSE),
    ('UMCSENT', 'University of Michigan: Consumer Sentiment', 'sentiment', 'monthly', 'index_1966q1_100', FALSE, FALSE),
    ('NFCI', 'Chicago Fed National Financial Conditions Index', 'risk', 'weekly', 'index', FALSE, FALSE)
ON CONFLICT (series_id) DO UPDATE SET
    title = EXCLUDED.title,
    category = EXCLUDED.category,
    frequency = EXCLUDED.frequency,
    units = EXCLUDED.units,
    enabled = EXCLUDED.enabled,
    is_default = EXCLUDED.is_default,
    updated_at = CURRENT_TIMESTAMP;

