CREATE TABLE processed_event (
    id VARCHAR(200) NOT NULL,
    type VARCHAR(120) NOT NULL,
    processed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_processed_event_type (type),
    INDEX idx_processed_event_processed_at (processed_at)
);
