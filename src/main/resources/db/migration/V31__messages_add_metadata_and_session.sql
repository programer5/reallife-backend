ALTER TABLE messages
    ADD COLUMN metadata_json TEXT NULL,
    ADD COLUMN locked_until DATETIME(6) NULL,
    ADD COLUMN session_id BINARY(16) NULL;