ALTER TABLE tax_summaries
    ADD COLUMN IF NOT EXISTS computed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE tax_summaries
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'DRAFT';

CREATE TABLE IF NOT EXISTS tax_summary_runs
(
    id            BIGSERIAL PRIMARY KEY,
    account_id    BIGINT       NOT NULL,
    tax_year      INT          NOT NULL,
    run_type      VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    started_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at   TIMESTAMP    NULL,
    error_message VARCHAR(500) NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tax_summary_runs_account_year_status
    ON tax_summary_runs (account_id, tax_year, status);

CREATE INDEX IF NOT EXISTS idx_tax_summary_runs_started_at
    ON tax_summary_runs (started_at DESC);
