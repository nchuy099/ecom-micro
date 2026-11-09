CREATE TABLE IF NOT EXISTS saga_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    topic VARCHAR(100) NOT NULL,
    order_id VARCHAR(36) NOT NULL,
    payload LONGTEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS order_idempotency (
    user_id VARCHAR(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin NOT NULL,
    idempotency_key CHAR(36) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    request_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    http_status INT NULL,
    response_body LONGTEXT NULL,
    expires_at TIMESTAMP(6) NULL,
    PRIMARY KEY (user_id, idempotency_key)
);
