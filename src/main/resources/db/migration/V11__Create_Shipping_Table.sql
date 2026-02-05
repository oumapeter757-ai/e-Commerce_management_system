CREATE TABLE shipping (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          order_id BIGINT NOT NULL,
                          tracking_number VARCHAR(100),
                          carrier VARCHAR(100) NOT NULL,
                          status VARCHAR(30) NOT NULL,
                          shipping_method VARCHAR(50),
                          shipping_cost DECIMAL(10, 2) NOT NULL,
                          shipping_address_id BIGINT NOT NULL,

    -- Dates
                          estimated_delivery_date DATETIME(6),
                          actual_delivery_date DATETIME(6),
                          shipped_at DATETIME(6),
                          delivered_at DATETIME(6),
                          created_at DATETIME(6) NOT NULL,
                          updated_at DATETIME(6),

    -- Package Details
                          weight DECIMAL(10, 2),
                          dimensions VARCHAR(50),
                          package_count INT DEFAULT 1,
                          tracking_url VARCHAR(500),
                          delivery_instructions TEXT,
                          signature_required BOOLEAN DEFAULT FALSE,
                          insurance_amount DECIMAL(10, 2),

    -- Notes
                          notes TEXT,
                          exception_reason TEXT,

    -- Constraints
                          CONSTRAINT uk_shipping_order UNIQUE (order_id),
                          CONSTRAINT uk_shipping_tracking UNIQUE (tracking_number),

    -- Foreign Keys
                          CONSTRAINT fk_shipping_order FOREIGN KEY (order_id) REFERENCES orders (id),
                          CONSTRAINT fk_shipping_address FOREIGN KEY (shipping_address_id) REFERENCES addresses (id)
);

-- Indexes defined in your Entity
CREATE INDEX idx_shipping_order_id ON shipping(order_id);
CREATE INDEX idx_shipping_tracking ON shipping(tracking_number);
CREATE INDEX idx_shipping_status ON shipping(status);