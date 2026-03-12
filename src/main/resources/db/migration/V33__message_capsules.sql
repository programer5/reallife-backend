CREATE TABLE message_capsules (
 id BINARY(16) PRIMARY KEY,
 message_id BINARY(16),
 conversation_id BINARY(16),
 creator_id BINARY(16),
 title VARCHAR(255),
 unlock_at DATETIME,
 opened_at DATETIME
);
