CREATE TABLE payments (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          order_id BIGINT NOT NULL,
                          user_id BIGINT NOT NULL,
                          amount DECIMAL(19, 2) NOT NULL,
                          payment_method VARCHAR(50) NOT NULL,
                          status VARCHAR(50) NOT NULL,

    -- Transaction Identifiers
                          transaction_id VARCHAR(255),
                          merchant_request_id VARCHAR(255),
                          checkout_request_id VARCHAR(255),
                          mpesa_receipt_number VARCHAR(255),  -- ADDED
                          payment_details TEXT,  -- Changed from VARCHAR(255) to TEXT
                          phone_number VARCHAR(255),  -- ADDED
                          callback_metadata TEXT,  -- ADDED (this was the missing column)
                          error_message TEXT,  -- ADDED

                          created_at DATETIME(6),
                          updated_at DATETIME(6),

                          CONSTRAINT uk_payments_order UNIQUE (order_id),
                          CONSTRAINT uk_payments_transaction UNIQUE (transaction_id),
                          CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders (id),
                          CONSTRAINT fk_payments_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Indexes from the entity
CREATE INDEX idx_payment_order_id ON payments(order_id);
CREATE INDEX idx_payment_user_id ON payments(user_id);
CREATE INDEX idx_payment_transaction_id ON payments(transaction_id);
CREATE INDEX idx_payment_status ON payments(status);
CREATE INDEX idx_payment_created_at ON payments(created_at);
CREATE INDEX idx_payments_merchant_req_id ON payments(merchant_request_id);