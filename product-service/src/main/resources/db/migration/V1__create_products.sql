CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(160) NOT NULL,
    sku VARCHAR(80) NOT NULL,
    price DECIMAL(19, 2) NOT NULL,
    stock INT NOT NULL,
    category_id BIGINT,
    version BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT chk_products_price_non_negative CHECK (price >= 0),
    CONSTRAINT chk_products_stock_non_negative CHECK (stock >= 0),
    INDEX idx_products_category_id (category_id),
    INDEX idx_products_created_at (created_at)
);
