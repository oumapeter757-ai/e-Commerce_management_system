CREATE TABLE inventory (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           product_id BIGINT NOT NULL,
                           sku VARCHAR(255) NOT NULL,
                           available_stock INT NOT NULL,
                           reserved_stock INT NOT NULL DEFAULT 0,
                           low_stock_threshold INT DEFAULT 10,
                           restock_quantity INT,
                           last_restocked DATETIME(6),
                           created_at DATETIME(6),
                           updated_at DATETIME(6),

    -- Ensures one inventory record per product
                           CONSTRAINT uk_inventory_product UNIQUE (product_id),

    -- Foreign Key linking to the 'products' table
                           CONSTRAINT fk_inventory_products FOREIGN KEY (product_id) REFERENCES products (id)
);