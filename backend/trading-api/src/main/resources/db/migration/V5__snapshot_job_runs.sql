CREATE TABLE IF NOT EXISTS snapshot_job_runs
(
    id            BIGSERIAL PRIMARY KEY,
    account_id    BIGINT       NOT NULL,
    business_date DATE         NOT NULL,
    status        VARCHAR(20)  NOT NULL,
    started_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at   TIMESTAMP    NULL,
    error_message VARCHAR(500) NULL,
    created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

DO
$$
    BEGIN
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'fk_snapshot_job_runs_account_id'
        ) THEN
            ALTER TABLE snapshot_job_runs
                ADD CONSTRAINT fk_snapshot_job_runs_account_id
                    FOREIGN KEY (account_id) REFERENCES accounts (id);
        END IF;
    END
$$;

CREATE INDEX IF NOT EXISTS idx_snapshot_job_runs_account_date_status
    ON snapshot_job_runs (account_id, business_date, status);

CREATE UNIQUE INDEX IF NOT EXISTS uq_snapshot_job_runs_running_account_date
    ON snapshot_job_runs (account_id, business_date)
    WHERE status = 'RUNNING';