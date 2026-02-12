ALTER TABLE conversations
    ADD COLUMN last_message_id BINARY(16) NULL,
  ADD COLUMN last_message_at DATETIME(6) NULL,
  ADD COLUMN last_message_preview VARCHAR(200) NULL;

CREATE INDEX idx_conversations_last_message_at ON conversations (last_message_at);