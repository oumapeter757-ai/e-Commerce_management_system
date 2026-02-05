
-- ============================================
-- Create Verification Tokens Table
-- ============================================
CREATE TABLE IF NOT EXISTS verification_tokens (
                                                   id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                                   token VARCHAR(100) NOT NULL UNIQUE,
                                                   user_id BIGINT NOT NULL,
                                                   token_type VARCHAR(30) NOT NULL,
                                                   expires_at TIMESTAMP NOT NULL,
                                                   confirmed_at TIMESTAMP NULL,
                                                   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                                   ip_address VARCHAR(45),

                                                   INDEX idx_token (token),
                                                   INDEX idx_user_id (user_id),
                                                   INDEX idx_expires_at (expires_at),
                                                   INDEX idx_token_type (token_type),

                                                   CONSTRAINT fk_verification_token_user
                                                       FOREIGN KEY (user_id) REFERENCES users(id)
                                                           ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
