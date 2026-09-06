CREATE TABLE notification_delivery (
    id BIGINT NOT NULL AUTO_INCREMENT,
    campaign_id VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    attempts INT NOT NULL,
    last_error VARCHAR(500),
    subject VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_notification_delivery_identity UNIQUE (campaign_id, user_id, channel),
    CONSTRAINT chk_notification_delivery_attempts_non_negative CHECK (attempts >= 0),
    INDEX idx_notification_delivery_status (status),
    INDEX idx_notification_delivery_user (user_id)
);

CREATE TABLE notification_dlq (
    id BIGINT NOT NULL AUTO_INCREMENT,
    campaign_id VARCHAR(128) NOT NULL,
    user_id BIGINT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    message VARCHAR(1000) NOT NULL,
    payload TEXT NOT NULL,
    failure_reason VARCHAR(500) NOT NULL,
    attempts INT NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_notification_dlq_attempts_positive CHECK (attempts > 0),
    INDEX idx_notification_dlq_status (status),
    INDEX idx_notification_dlq_channel (channel),
    INDEX idx_notification_dlq_user (user_id)
);
