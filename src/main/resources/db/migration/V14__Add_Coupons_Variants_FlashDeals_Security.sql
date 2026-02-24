-- =====================================================
-- V14: Coupons, Product Variants, Flash Deals & Security
-- =====================================================
-- NOTE: All CREATE TABLE use IF NOT EXISTS for idempotency.
-- ALTER TABLE statements assume columns don't exist yet.
-- If re-running after partial failure, use: flyway repair
-- then re-run. Or drop + recreate the database in dev.
-- =====================================================

-- 1. COUPONS TABLE
CREATE TABLE IF NOT EXISTS coupons (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(50) NOT NULL UNIQUE,
    description TEXT,
    discount_type VARCHAR(30) NOT NULL,
    discount_value DECIMAL(10,2) NOT NULL,
    min_order_amount DECIMAL(10,2) DEFAULT 0.00,
    max_discount_amount DECIMAL(10,2),
    usage_limit INT,
    usage_count INT DEFAULT 0,
    per_user_limit INT DEFAULT 1,
    starts_at DATETIME NOT NULL,
    expires_at DATETIME NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_coupon_code (code),
    INDEX idx_coupon_active (is_active),
    INDEX idx_coupon_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. COUPON USAGE TRACKING TABLE
CREATE TABLE IF NOT EXISTS coupon_usages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    coupon_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    order_id BIGINT,
    used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (coupon_id) REFERENCES coupons(id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (order_id) REFERENCES orders(id),
    INDEX idx_coupon_user (coupon_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. PRODUCT VARIANTS TABLE
CREATE TABLE IF NOT EXISTS product_variants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    sku VARCHAR(100) NOT NULL UNIQUE,
    variant_name VARCHAR(100),
    color VARCHAR(50),
    size VARCHAR(50),
    material VARCHAR(100),
    price_adjustment DECIMAL(10,2) DEFAULT 0.00,
    stock_quantity INT NOT NULL DEFAULT 0,
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_variant_product (product_id),
    INDEX idx_variant_sku (sku),
    INDEX idx_variant_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 4. FLASH DEALS TABLE
CREATE TABLE IF NOT EXISTS flash_deals (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    deal_name VARCHAR(255),
    deal_price DECIMAL(10,2) NOT NULL,
    original_price DECIMAL(10,2) NOT NULL,
    discount_percentage DECIMAL(5,2),
    starts_at DATETIME NOT NULL,
    ends_at DATETIME NOT NULL,
    stock_limit INT,
    sold_count INT DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_flash_product (product_id),
    INDEX idx_flash_active (is_active),
    INDEX idx_flash_dates (starts_at, ends_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. OPTIMISTIC LOCKING: Add version columns
ALTER TABLE carts ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE cart_items ADD COLUMN version BIGINT DEFAULT 0;
ALTER TABLE inventory ADD COLUMN version BIGINT DEFAULT 0;

-- 6. PAYMENT IDEMPOTENCY
ALTER TABLE payments ADD COLUMN idempotency_key VARCHAR(64) UNIQUE;

-- 7. Add variant references
ALTER TABLE cart_items ADD COLUMN product_variant_id BIGINT NULL;
ALTER TABLE cart_items ADD CONSTRAINT fk_cart_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id);

ALTER TABLE order_items ADD COLUMN product_variant_id BIGINT NULL;
ALTER TABLE order_items ADD CONSTRAINT fk_order_item_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id);
