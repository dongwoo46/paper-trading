CREATE TABLE IF NOT EXISTS position_exit_triggers (
  id BIGSERIAL PRIMARY KEY,
  position_id BIGINT NOT NULL UNIQUE,
  account_id BIGINT NOT NULL,
  ticker VARCHAR(32) NOT NULL,
  enabled BOOLEAN NOT NULL,
  stop_loss_percent NUMERIC(8,4),
  take_profit_percent NUMERIC(8,4),
  stop_loss_state VARCHAR(16) NOT NULL,
  take_profit_state VARCHAR(16) NOT NULL,
  triggered_by VARCHAR(16),
  triggered_at TIMESTAMP,
  last_evaluated_at TIMESTAMP,
  last_evaluated_price NUMERIC(18,4),
  trigger_version BIGINT NOT NULL DEFAULT 1,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_position_exit_triggers_ticker_state
ON position_exit_triggers(ticker, enabled, stop_loss_state, take_profit_state);
