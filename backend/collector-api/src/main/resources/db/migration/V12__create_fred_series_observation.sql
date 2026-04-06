CREATE TABLE IF NOT EXISTS fred_series_observation (
    id BIGSERIAL PRIMARY KEY,
    series_id VARCHAR(64) NOT NULL,
    observation_date DATE NOT NULL,
    raw_value VARCHAR(128) NOT NULL,
    numeric_value NUMERIC(20, 8),
    fetched_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_fred_series_observation UNIQUE (series_id, observation_date)
);

CREATE INDEX IF NOT EXISTS idx_fred_series_observation_series_date
    ON fred_series_observation (series_id, observation_date DESC);
