-- V21__conversation_pins_source_message_id.sql (MySQL 8 safe, UUID=BINARY(16))

ALTER TABLE conversation_pins
    ADD COLUMN source_message_id BINARY(16) NULL;

ALTER TABLE conversation_pins
    ADD COLUMN source_message_key BINARY(16)
        GENERATED ALWAYS AS (
            CASE
                WHEN deleted = 0 AND source_message_id IS NOT NULL THEN source_message_id
                ELSE NULL
            END
        ) STORED;

CREATE UNIQUE INDEX uk_pins_conversation_source_message
    ON conversation_pins (conversation_id, source_message_key);