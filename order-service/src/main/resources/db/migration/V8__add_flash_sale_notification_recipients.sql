ALTER TABLE flash_sale_campaigns
    ADD COLUMN notification_subject VARCHAR(255);

ALTER TABLE flash_sale_campaigns
    ADD COLUMN notification_message VARCHAR(1000);

CREATE TABLE flash_sale_campaign_recipients (
    id BIGINT NOT NULL AUTO_INCREMENT,
    campaign_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    task_published_at TIMESTAMP(6),
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_flash_sale_recipient_campaign
        FOREIGN KEY (campaign_id) REFERENCES flash_sale_campaigns(id),
    CONSTRAINT uk_flash_sale_campaign_recipient UNIQUE (campaign_id, user_id),
    INDEX idx_flash_sale_recipient_status (campaign_id, status)
);
