CREATE TABLE inventory (
                           id BIGINT AUTO_INCREMENT PRIMARY KEY,
                           product_id BIGINT NOT NULL,
                           quantity INT NOT NULL,
                           reserved_quantity INT NOT NULL DEFAULT 0,
                           low_stock_threshold INT NOT NULL DEFAULT 10,
                           version BIGINT,
                           last_updated DATETIME(6),

    -- Ensures one inventory record per product
                           CONSTRAINT uk_inventory_product UNIQUE (product_id),

    -- Foreign Key linking to the 'products' table
                           CONSTRAINT fk_inventory_products FOREIGN KEY (product_id) REFERENCES products (id)
);