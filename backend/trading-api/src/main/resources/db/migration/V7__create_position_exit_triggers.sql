DROP TABLE IF EXISTS position_exit_triggers;

CREATE TABLE position_exit_triggers (
  id BIGSERIAL PRIMARY KEY,
  position_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  ticker VARCHAR(32) NOT NULL,
  trigger_type VARCHAR(16) NOT NULL,
  trigger_percent NUMERIC(8,4),
  trigger_price NUMERIC(20,4),
  price_basis_policy VARCHAR(32) NOT NULL,
  exit_ratio_percent NUMERIC(8,4) NOT NULL DEFAULT 100.0000,
  state VARCHAR(16) NOT NULL,
  skip_reason VARCHAR(32),
  triggered_at TIMESTAMP,
  last_evaluated_at TIMESTAMP,
  last_evaluated_price NUMERIC(20,4),
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_position_exit_triggers_position_state
ON position_exit_triggers(position_id, state);

CREATE INDEX idx_position_exit_triggers_ticker_state
ON position_exit_triggers(ticker, state);

CREATE INDEX idx_position_exit_triggers_group_scan
ON position_exit_triggers(ticker, position_id, trigger_type, state);
