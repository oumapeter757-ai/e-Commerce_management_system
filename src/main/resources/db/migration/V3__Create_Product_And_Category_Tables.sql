-- ============================================
-- Create Categories Table
-- ============================================
CREATE TABLE IF NOT EXISTS categories (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          name VARCHAR(100) NOT NULL UNIQUE,
                                          slug VARCHAR(150) NOT NULL UNIQUE,
                                          description TEXT,
                                          parent_id BIGINT,
                                          image_url VARCHAR(500),
                                          icon VARCHAR(100),
                                          display_order INT DEFAULT 0,
                                          is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                          product_count BIGINT DEFAULT 0,
                                          meta_title TEXT,
                                          meta_description TEXT,
                                          meta_keywords TEXT,
                                          created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                          updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                          INDEX idx_category_name (name),
                                          INDEX idx_category_slug (slug),
                                          INDEX idx_parent_id (parent_id),
                                          INDEX idx_is_active (is_active),
                                          INDEX idx_display_order (display_order),

                                          CONSTRAINT fk_category_parent
                                              FOREIGN KEY (parent_id) REFERENCES categories(id)
                                                  ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Create Products Table
-- ============================================
CREATE TABLE IF NOT EXISTS products (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        name VARCHAR(255) NOT NULL,
                                        sku VARCHAR(100) NOT NULL UNIQUE,
                                        description TEXT,
                                        short_description VARCHAR(500),
                                        price DECIMAL(10, 2) NOT NULL,
                                        discount_price DECIMAL(10, 2),
                                        cost_price DECIMAL(10, 2),
                                        category_id BIGINT NOT NULL,
                                        seller_id BIGINT,
                                        stock_quantity INT NOT NULL DEFAULT 0,
                                        low_stock_threshold INT DEFAULT 10,
                                        image_url VARCHAR(500),
                                        additional_images TEXT,
                                        brand VARCHAR(50) NOT NULL,
                                        manufacturer VARCHAR(100),
                                        weight DECIMAL(10, 2) DEFAULT 0.00,
                                        dimensions VARCHAR(100),
                                        color VARCHAR(50),
                                        size VARCHAR(50),
                                        tags TEXT,
                                        is_active BOOLEAN NOT NULL DEFAULT TRUE,
                                        is_featured BOOLEAN NOT NULL DEFAULT FALSE,
                                        view_count BIGINT DEFAULT 0,
                                        sold_count BIGINT DEFAULT 0,
                                        average_rating DECIMAL(3, 2) DEFAULT 0.00,
                                        review_count INT DEFAULT 0,
                                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                        INDEX idx_product_name (name),
                                        INDEX idx_product_sku (sku),
                                        INDEX idx_category_id (category_id),
                                        INDEX idx_seller_id (seller_id),
                                        INDEX idx_brand (brand),
                                        INDEX idx_is_active (is_active),
                                        INDEX idx_is_featured (is_featured),
                                        INDEX idx_created_at (created_at),
                                        INDEX idx_price (price),
                                        INDEX idx_stock_quantity (stock_quantity),

                                        CONSTRAINT fk_product_category
                                            FOREIGN KEY (category_id) REFERENCES categories(id)
                                                ON DELETE RESTRICT,

                                        CONSTRAINT fk_product_seller
                                            FOREIGN KEY (seller_id) REFERENCES users(id)
                                                ON DELETE SET NULL,

                                        CONSTRAINT chk_price_positive
                                            CHECK (price > 0),

                                        CONSTRAINT chk_discount_less_than_price
                                            CHECK (discount_price IS NULL OR discount_price < price),

                                        CONSTRAINT chk_stock_non_negative
                                            CHECK (stock_quantity >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
