-- 1. Create the table
CREATE TABLE addresses (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           user_id BIGINT NOT NULL,
                           full_name VARCHAR(100) NOT NULL,
                           phone_number VARCHAR(20) NOT NULL,
                           address_line1 VARCHAR(255) NOT NULL,
                           address_line2 VARCHAR(255),
                           city VARCHAR(100) NOT NULL,
                           state VARCHAR(100),
                           postal_code VARCHAR(20) NOT NULL,
                           country VARCHAR(100) NOT NULL,
                           address_type VARCHAR(20),
                           is_default BOOLEAN NOT NULL DEFAULT FALSE,
                           landmark TEXT,
                           latitude DECIMAL(10, 7),
                           longitude DECIMAL(10, 7),
                           created_at DATETIME(6) NOT NULL,
                           updated_at DATETIME(6),

    -- Make sure 'users' matches your actual database table name!
    -- If your table is named 'user', change 'users' to 'user' below.
                           CONSTRAINT fk_addresses_user
                               FOREIGN KEY (user_id)
                                   REFERENCES users (id)
                                   ON DELETE CASCADE
);

-- 2. Create Indexes (These MUST match the @Index names in Address.java exactly)
CREATE INDEX idx_user_id ON addresses (user_id);
CREATE INDEX idx_is_default ON addresses (is_default);

-- 1. Create Orders Table
CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        order_number VARCHAR(50) NOT NULL UNIQUE,
                        user_id BIGINT NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        shipping_address_id BIGINT NOT NULL,
                        billing_address_id BIGINT NOT NULL,
                        subtotal DECIMAL(10, 2) NOT NULL,
                        discount_amount DECIMAL(10, 2) DEFAULT 0.00,
                        tax_amount DECIMAL(10, 2) DEFAULT 0.00,
                        shipping_cost DECIMAL(10, 2) DEFAULT 0.00,
                        total_amount DECIMAL(10, 2) NOT NULL,
                        coupon_code VARCHAR(50),
                        customer_notes TEXT,
                        admin_notes TEXT,
                        tracking_number VARCHAR(100),
                        carrier VARCHAR(100),
                        estimated_delivery_date DATETIME(6),
                        delivered_at DATETIME(6),
                        cancelled_at DATETIME(6),
                        cancellation_reason TEXT,
                        created_at DATETIME(6) NOT NULL,
                        updated_at DATETIME(6),

                        CONSTRAINT fk_order_user
                            FOREIGN KEY (user_id)
                                REFERENCES users (id),
                        CONSTRAINT fk_order_shipping_address
                            FOREIGN KEY (shipping_address_id)
                                REFERENCES addresses (id),
                        CONSTRAINT fk_order_billing_address
                            FOREIGN KEY (billing_address_id)
                                REFERENCES addresses (id)
);

CREATE INDEX idx_order_user_id ON orders (user_id);
CREATE INDEX idx_order_number ON orders (order_number);
CREATE INDEX idx_order_status ON orders (status);
CREATE INDEX idx_order_created_at ON orders (created_at);

-- 2. Create Order Items Table
CREATE TABLE order_items (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             product_id BIGINT NOT NULL,
                             product_name VARCHAR(255) NOT NULL,
                             product_sku VARCHAR(100) NOT NULL,
                             quantity INT NOT NULL,
                             unit_price DECIMAL(10, 2) NOT NULL,
                             discount_price DECIMAL(10, 2),
                             total_price DECIMAL(10, 2) NOT NULL,
                             tax_amount DECIMAL(10, 2) DEFAULT 0.00,
                             product_image_url VARCHAR(500),
                             created_at DATETIME(6) NOT NULL,

                             CONSTRAINT fk_order_item_order
                                 FOREIGN KEY (order_id)
                                     REFERENCES orders (id)
                                     ON DELETE CASCADE,
                             CONSTRAINT fk_order_item_product
                                 FOREIGN KEY (product_id)
                                     REFERENCES products (id)
);

CREATE INDEX idx_order_item_order_id ON order_items (order_id);
CREATE INDEX idx_order_item_product_id ON order_items (product_id);

-- 3. Create Reviews Table
CREATE TABLE reviews (
                         id BIGINT AUTO_INCREMENT PRIMARY KEY,
                         product_id BIGINT NOT NULL,
                         user_id BIGINT NOT NULL,
                         order_id BIGINT,
                         rating INT NOT NULL,
                         title VARCHAR(200) NOT NULL,
                         comment TEXT NOT NULL,
                         verified_purchase BIT(1) NOT NULL DEFAULT 0,
                         is_approved BIT(1) NOT NULL DEFAULT 0,
                         helpful_count INT DEFAULT 0,
                         not_helpful_count INT DEFAULT 0,
                         images TEXT,
                         admin_response TEXT,
                         responded_at DATETIME(6),
                         created_at DATETIME(6) NOT NULL,
                         updated_at DATETIME(6),

                         CONSTRAINT fk_review_product
                             FOREIGN KEY (product_id)
                                 REFERENCES products (id),
                         CONSTRAINT fk_review_user
                             FOREIGN KEY (user_id)
                                 REFERENCES users (id),
                         CONSTRAINT fk_review_order
                             FOREIGN KEY (order_id)
                                 REFERENCES orders (id)
);

CREATE INDEX idx_review_product_id ON reviews (product_id);
CREATE INDEX idx_review_user_id ON reviews (user_id);
CREATE INDEX idx_review_rating ON reviews (rating);
CREATE INDEX idx_review_verified_purchase ON reviews (verified_purchase);
CREATE INDEX idx_review_created_at ON reviews (created_at);