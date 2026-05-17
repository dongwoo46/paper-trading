ALTER TABLE position_exit_triggers
ADD COLUMN IF NOT EXISTS skip_reason VARCHAR(32);
