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
