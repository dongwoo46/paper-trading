CREATE TABLE IF NOT EXISTS account_exit_trigger_defaults (
  id BIGSERIAL PRIMARY KEY,
  account_id BIGINT NOT NULL UNIQUE,
  enabled BOOLEAN NOT NULL,
  stop_loss_percent NUMERIC(8,4),
  take_profit_percent NUMERIC(8,4),
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  updated_at TIMESTAMP NOT NULL DEFAULT now()
);
