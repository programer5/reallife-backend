ALTER TABLE conversation_members
    ADD COLUMN last_read_at DATETIME(6) NULL;

CREATE INDEX idx_conv_member_read_at ON conversation_members (conversation_id, user_id, last_read_at);