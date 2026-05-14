CREATE TABLE IF NOT EXISTS analysis_request_queue (
    id              SERIAL       PRIMARY KEY,
    symbol          VARCHAR(20)  NOT NULL,
    window          VARCHAR(10)  NOT NULL,
    interval        VARCHAR(5)   NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'pending',
    requested_count INT          NOT NULL DEFAULT 1,
    requested_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_analysis_request_queue_active
    ON analysis_request_queue (symbol, window, interval)
    WHERE status IN ('pending', 'processing');

CREATE INDEX IF NOT EXISTS idx_analysis_request_queue_status_requested_at
    ON analysis_request_queue (status, requested_at);
