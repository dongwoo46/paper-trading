ALTER TABLE orders
ADD COLUMN IF NOT EXISTS order_group_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_orders_order_group_id
ON orders(order_group_id);
