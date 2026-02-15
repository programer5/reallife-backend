CREATE TABLE refresh_tokens (
                                id BINARY(16) NOT NULL,
                                user_id BINARY(16) NOT NULL,
                                token_hash CHAR(64) NOT NULL,
                                expires_at DATETIME(6) NOT NULL,
                                revoked_at DATETIME(6) NULL,
                                created_at DATETIME(6) NOT NULL,
                                PRIMARY KEY (id),
                                UNIQUE KEY uk_refresh_tokens_token_hash (token_hash),
                                KEY idx_refresh_tokens_user_id (user_id),
                                KEY idx_refresh_tokens_expires_at (expires_at)
);