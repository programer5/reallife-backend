CREATE TABLE message_capsules (
    id BINARY(16) PRIMARY KEY,
    message_id BINARY(16) NOT NULL,
    conversation_id BINARY(16) NOT NULL,
    creator_id BINARY(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    unlock_at DATETIME(6) NOT NULL,
    opened_at DATETIME(6) NULL
);
CREATE INDEX idx_message_capsules_conversation_unlock ON message_capsules(conversation_id, unlock_at DESC);
