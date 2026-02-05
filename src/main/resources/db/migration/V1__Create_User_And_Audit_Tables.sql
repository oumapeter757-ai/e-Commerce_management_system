CREATE TABLE IF NOT EXISTS users (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     first_name VARCHAR(255) NOT NULL,
                                     last_name VARCHAR(255) NOT NULL,
                                     email VARCHAR(255) NOT NULL UNIQUE,
                                     password VARCHAR(255) NOT NULL,
                                     phone_number VARCHAR(50) NOT NULL,
                                     role VARCHAR(50) NOT NULL,
                                     is_enabled BOOLEAN NOT NULL DEFAULT FALSE,
                                     email_verified TINYINT(1) DEFAULT 0,
                                     created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                                     updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

                                     INDEX idx_email (email),
                                     INDEX idx_role (role),
                                     INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================
-- Create Audit Logs Table
-- ============================================
CREATE TABLE IF NOT EXISTS audit_logs (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          user_email VARCHAR(255),
                                          user_id BIGINT,
                                          action VARCHAR(100) NOT NULL,
                                          resource_type VARCHAR(50),
                                          resource_id VARCHAR(100),
                                          ip_address VARCHAR(45),
                                          user_agent VARCHAR(500),
                                          request_method VARCHAR(10),
                                          request_url VARCHAR(500),
                                          details TEXT,
                                          status VARCHAR(20) NOT NULL,
                                          error_message VARCHAR(1000),
                                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          session_id VARCHAR(100),
                                          execution_time_ms BIGINT,

                                          INDEX idx_user_email (user_email),
                                          INDEX idx_action (action),
                                          INDEX idx_created_at (created_at),
                                          INDEX idx_ip_address (ip_address),
                                          INDEX idx_status (status),
                                          INDEX idx_resource_type (resource_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

