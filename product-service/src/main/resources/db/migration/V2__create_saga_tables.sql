CREATE TABLE outbox_event (
    id VARCHAR(36) NOT NULL,
    aggregate_type VARCHAR(120) NOT NULL,
    aggregate_id VARCHAR(120) NOT NULL,
    type VARCHAR(120) NOT NULL,
    payload VARCHAR(4096) NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_outbox_event_aggregate (aggregate_type, aggregate_id),
    INDEX idx_outbox_event_type (type),
    INDEX idx_outbox_event_occurred_at (occurred_at)
);

CREATE TABLE processed_event (
    id VARCHAR(200) NOT NULL,
    type VARCHAR(120) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_processed_event_type (type),
    INDEX idx_processed_event_processed_at (processed_at)
);

CREATE TABLE stock_reservation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    released BOOLEAN NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_stock_reservation_order_product UNIQUE (order_id, product_id),
    CONSTRAINT chk_stock_reservation_quantity_positive CHECK (quantity > 0),
    INDEX idx_stock_reservation_order_id (order_id),
    INDEX idx_stock_reservation_released (released)
);
