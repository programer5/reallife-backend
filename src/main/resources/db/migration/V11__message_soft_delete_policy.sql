-- V11__message_soft_delete.sql

-- messages: 전체 삭제 시각
ALTER TABLE messages
    ADD COLUMN deleted_at DATETIME(6) NULL;

-- 유저별 숨김(나만 삭제)
CREATE TABLE message_hidden (
                                id BINARY(16) NOT NULL,
                                user_id BINARY(16) NOT NULL,
                                message_id BINARY(16) NOT NULL,
                                created_at DATETIME(6) NOT NULL,
                                PRIMARY KEY (id),
                                CONSTRAINT uk_message_hidden UNIQUE (user_id, message_id),
                                INDEX idx_message_hidden_user_message (user_id, message_id),
                                INDEX idx_message_hidden_message (message_id)
);