-- ============================================================
-- V12: Enhance Cart Tables + Add Wishlist + Recently Viewed
-- Kilimall-style shopping cart features
-- ============================================================

-- 1. Enhance cart_items table
ALTER TABLE cart_items
    ADD COLUMN selected BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN saved_for_later BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN price_at_addition DECIMAL(10,2) NULL,
    ADD COLUMN notes VARCHAR(500) NULL,
    ADD COLUMN max_buy_quantity INT NULL;

-- 2. Enhance carts table
ALTER TABLE carts
    ADD COLUMN expires_at DATETIME NULL,
    ADD COLUMN estimated_shipping_cost DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN selected_items_total DECIMAL(10,2) NOT NULL DEFAULT 0.00,
    ADD COLUMN total_savings DECIMAL(10,2) NOT NULL DEFAULT 0.00;

-- 3. Create wishlists table
CREATE TABLE IF NOT EXISTS wishlists (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_wishlist_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uk_wishlist_user_product UNIQUE (user_id, product_id),
    INDEX idx_wishlist_user_id (user_id),
    INDEX idx_wishlist_product_id (product_id),
    INDEX idx_wishlist_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. Create recently_viewed_products table
CREATE TABLE IF NOT EXISTS recently_viewed_products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    viewed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_rv_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rv_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    INDEX idx_rv_user_id (user_id),
    INDEX idx_rv_product_id (product_id),
    INDEX idx_rv_viewed_at (viewed_at),
    INDEX idx_rv_user_viewed (user_id, viewed_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. Add max_buy_quantity to products table
ALTER TABLE products
    ADD COLUMN max_buy_quantity INT NULL DEFAULT NULL;

-- 6. Backfill price_at_addition for existing cart items
UPDATE cart_items ci
    JOIN products p ON ci.product_id = p.id
SET ci.price_at_addition = ci.unit_price
WHERE ci.price_at_addition IS NULL;

