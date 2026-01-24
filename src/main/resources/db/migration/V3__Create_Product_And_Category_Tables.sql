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
