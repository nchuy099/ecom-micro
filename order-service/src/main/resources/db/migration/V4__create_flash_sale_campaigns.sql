CREATE TABLE flash_sale_campaigns (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    stock INT NOT NULL,
    starts_at TIMESTAMP(6) NOT NULL,
    ends_at TIMESTAMP(6) NOT NULL,
    promo_price DECIMAL(19, 2) NOT NULL,
    max_per_user INT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT chk_flash_sale_stock_non_negative CHECK (stock >= 0),
    CONSTRAINT chk_flash_sale_promo_price_non_negative CHECK (promo_price >= 0),
    CONSTRAINT chk_flash_sale_max_per_user_positive CHECK (max_per_user > 0),
    CONSTRAINT chk_flash_sale_window CHECK (ends_at > starts_at),
    INDEX idx_flash_sale_product_id (product_id),
    INDEX idx_flash_sale_window (starts_at, ends_at)
);
