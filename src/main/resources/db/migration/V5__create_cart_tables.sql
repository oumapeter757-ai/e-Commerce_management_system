-- 1. Create Carts Table
CREATE TABLE carts (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       user_id BIGINT UNIQUE,
                       session_id VARCHAR(100),
                       total_items INT DEFAULT 0,
                       subtotal DECIMAL(10, 2) DEFAULT 0.00,
                       discount_amount DECIMAL(10, 2) DEFAULT 0.00,
                       tax_amount DECIMAL(10, 2) DEFAULT 0.00,
                       total_amount DECIMAL(10, 2) DEFAULT 0.00,
                       coupon_code VARCHAR(50),
                       created_at DATETIME(6) NOT NULL,
                       updated_at DATETIME(6),

                       CONSTRAINT fk_cart_user
                           FOREIGN KEY (user_id)
                               REFERENCES users (id)
);

CREATE INDEX idx_cart_user_id ON carts (user_id);
CREATE INDEX idx_cart_session_id ON carts (session_id);

-- 2. Create Cart Items Table
CREATE TABLE cart_items (
                            id BIGINT AUTO_INCREMENT PRIMARY KEY,
                            cart_id BIGINT NOT NULL,
                            product_id BIGINT NOT NULL,
                            quantity INT NOT NULL,
                            unit_price DECIMAL(10, 2) NOT NULL,
                            total_price DECIMAL(10, 2) NOT NULL,
                            created_at DATETIME(6) NOT NULL,
                            updated_at DATETIME(6),

                            CONSTRAINT fk_cart_item_cart
                                FOREIGN KEY (cart_id)
                                    REFERENCES carts (id)
                                    ON DELETE CASCADE,
                            CONSTRAINT fk_cart_item_product
                                FOREIGN KEY (product_id)
                                    REFERENCES products (id)
);

CREATE INDEX idx_cart_item_cart_id ON cart_items (cart_id);
CREATE INDEX idx_cart_item_product_id ON cart_items (product_id);