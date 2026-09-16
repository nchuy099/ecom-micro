CREATE INDEX idx_orders_user_created_id ON orders (user_id, created_at DESC, id DESC);
