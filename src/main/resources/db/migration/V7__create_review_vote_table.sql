CREATE TABLE review_vote (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             is_helpful BOOLEAN NOT NULL,
                             review_id BIGINT NOT NULL,
                             user_id BIGINT NOT NULL,

    -- Foreign Keys
                             CONSTRAINT fk_review_vote_review
                                 FOREIGN KEY (review_id)
                                     REFERENCES reviews (id)
                                     ON DELETE CASCADE, -- If review is deleted, delete the votes

                             CONSTRAINT fk_review_vote_user
                                 FOREIGN KEY (user_id)
                                     REFERENCES users (id)
                                     ON DELETE CASCADE,

    -- UNIQUE CONSTRAINT: Crucial for preventing double voting
                             CONSTRAINT uk_review_vote_user_review
                                 UNIQUE (review_id, user_id)
);