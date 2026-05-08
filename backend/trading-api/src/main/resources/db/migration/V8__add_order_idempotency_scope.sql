ALTER TABLE orders DROP CONSTRAINT IF EXISTS uk_orders_account_idempotency;
ALTER TABLE orders ADD CONSTRAINT uk_orders_account_scope_idempotency UNIQUE (account_id, idempotency_key);
