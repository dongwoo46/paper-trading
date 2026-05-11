-- V2__create_chart_analysis_tables.sql
-- 차트 분석 결과, 요청 큐, 인기 종목 테이블 생성

-- 1. chart_analysis_result: 차트 수치 분석 결과 (7 윈도우)
CREATE TABLE IF NOT EXISTS chart_analysis_result (
    symbol                  VARCHAR(20)     NOT NULL,
    window                  VARCHAR(10)     NOT NULL,
    interval                VARCHAR(5)      NOT NULL,
    snapshot_hash           VARCHAR(64)     NOT NULL,
    recommendation          VARCHAR(15)     NOT NULL,
    confidence              NUMERIC(4,3)    NOT NULL,
    levels                  JSONB           NOT NULL,
    trend                   JSONB           NOT NULL,
    patterns                JSONB           NOT NULL,
    indicator_signals       JSONB           NOT NULL,
    volume_analysis         JSONB           NOT NULL,
    llm_report              JSONB,
    llm_report_source       VARCHAR(20)     NOT NULL DEFAULT 'none',
    numeric_computed_at     TIMESTAMPTZ     NOT NULL,
    llm_computed_at         TIMESTAMPTZ,
    created_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_chart_analysis_result PRIMARY KEY (symbol, window, interval)
);

CREATE INDEX IF NOT EXISTS idx_chart_analysis_symbol
    ON chart_analysis_result (symbol);

CREATE INDEX IF NOT EXISTS idx_chart_analysis_numeric_computed_at
    ON chart_analysis_result (numeric_computed_at);


-- 2. analysis_request_queue: LLM 분석 요청 큐 (비인기 종목)
CREATE TABLE IF NOT EXISTS analysis_request_queue (
    id              BIGSERIAL       PRIMARY KEY,
    symbol          VARCHAR(20)     NOT NULL,
    window          VARCHAR(10)     NOT NULL,
    interval        VARCHAR(5)      NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'pending',
    requested_count INT             NOT NULL DEFAULT 1,
    requested_at    TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    processed_at    TIMESTAMPTZ
);

-- 동일 (symbol, window, interval) 조합에서 pending/processing 상태는 유일
CREATE UNIQUE INDEX IF NOT EXISTS uq_request_queue_active
    ON analysis_request_queue (symbol, window, interval)
    WHERE status IN ('pending', 'processing');

CREATE INDEX IF NOT EXISTS idx_request_queue_status_requested_at
    ON analysis_request_queue (status, requested_at);


-- 3. popular_symbols: 인기 종목 TOP 300 (주 1회 갱신)
CREATE TABLE IF NOT EXISTS popular_symbols (
    symbol          VARCHAR(20)     NOT NULL,
    market          VARCHAR(10)     NOT NULL,
    rank            INT             NOT NULL,
    market_cap      NUMERIC(24,0)   NOT NULL,
    avg_volume      NUMERIC(24,0)   NOT NULL,
    score           NUMERIC(10,6)   NOT NULL,
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),

    CONSTRAINT pk_popular_symbols PRIMARY KEY (symbol)
);
