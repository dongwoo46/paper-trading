CREATE TABLE IF NOT EXISTS chart_analysis_result (
    symbol              VARCHAR(20)  NOT NULL,
    window              VARCHAR(10)  NOT NULL,
    interval            VARCHAR(5)   NOT NULL,
    snapshot_hash       VARCHAR(64),
    recommendation      VARCHAR(20),
    confidence          NUMERIC(6,4),
    levels              JSONB,
    trend               JSONB,
    patterns            JSONB,
    indicator_signals   JSONB,
    volume_analysis     JSONB,
    llm_report          JSONB,
    llm_report_source   VARCHAR(20),
    numeric_computed_at TIMESTAMPTZ,
    llm_computed_at     TIMESTAMPTZ,
    created_at          TIMESTAMPTZ  DEFAULT NOW(),
    updated_at          TIMESTAMPTZ  DEFAULT NOW(),
    CONSTRAINT chart_analysis_result_pkey PRIMARY KEY (symbol, window, interval)
);

CREATE INDEX IF NOT EXISTS idx_chart_analysis_result_symbol
    ON chart_analysis_result (symbol);
