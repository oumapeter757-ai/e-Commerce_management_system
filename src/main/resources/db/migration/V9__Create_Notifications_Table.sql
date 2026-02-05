CREATE TABLE notifications (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               user_id BIGINT NOT NULL,
                               type VARCHAR(50) NOT NULL,
                               title VARCHAR(200) NOT NULL,
                               message TEXT NOT NULL,
                               action_url VARCHAR(500),
                               reference_id BIGINT,
                               reference_type VARCHAR(50),

    -- Boolean flags with defaults
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               is_sent BOOLEAN NOT NULL DEFAULT FALSE,
                               is_email_sent BOOLEAN NOT NULL DEFAULT FALSE,
                               is_sms_sent BOOLEAN NOT NULL DEFAULT FALSE,

    -- Timestamps
                               read_at DATETIME(6),
                               sent_at DATETIME(6),
                               email_sent_at DATETIME(6),
                               sms_sent_at DATETIME(6),
                               expires_at DATETIME(6),
                               created_at DATETIME(6) NOT NULL,

                               priority INT DEFAULT 1,
                               metadata TEXT,

    -- Foreign Key to Users table
                               CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Indexes defined in your Entity
CREATE INDEX idx_user_id ON notifications(user_id);
CREATE INDEX idx_is_read ON notifications(is_read);
CREATE INDEX idx_type ON notifications(type);
CREATE INDEX idx_created_at ON notifications(created_at);