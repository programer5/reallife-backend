-- DM 도메인 스키마 (conversations, members, messages, attachments)
-- UUID: BINARY(16), created_at/update_at: DATETIME(6), deleted: TINYINT(1)

CREATE TABLE IF NOT EXISTS conversations (
                                             id         BINARY(16)   NOT NULL,
    type       VARCHAR(20)  NOT NULL,
    deleted    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME(6)  NOT NULL,
    update_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS conversation_members (
                                                    id                   BINARY(16)  NOT NULL,
    conversation_id      BINARY(16)  NOT NULL,
    user_id              BINARY(16)  NOT NULL,
    last_read_message_id BINARY(16)  NULL,
    deleted              TINYINT(1)  NOT NULL DEFAULT 0,
    created_at           DATETIME(6) NOT NULL,
    update_at            DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_conv_user UNIQUE (conversation_id, user_id)
    );

CREATE TABLE IF NOT EXISTS messages (
                                        id              BINARY(16)   NOT NULL,
    conversation_id BINARY(16)   NOT NULL,
    sender_id       BINARY(16)   NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    content         TEXT         NULL,
    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    update_at       DATETIME(6)  NOT NULL,
    PRIMARY KEY (id)
    );

CREATE TABLE IF NOT EXISTS message_attachments (
                                                   id         BINARY(16)  NOT NULL,
    message_id BINARY(16)  NOT NULL,
    file_id    BINARY(16)  NOT NULL,
    sort_order INT         NOT NULL,
    deleted    TINYINT(1)  NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    update_at  DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_message_file UNIQUE (message_id, file_id)
    );

-- indexes (있으면 스킵 / 없으면 생성) : 기존 V1 스타일 유지

-- conversation_members: idx_conv_id
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'conversation_members'
    AND index_name = 'idx_conv_id'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_conv_id ON conversation_members (conversation_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- conversation_members: idx_conv_member_user_id
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'conversation_members'
    AND index_name = 'idx_conv_member_user_id'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_conv_member_user_id ON conversation_members (user_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- messages: idx_message_conversation_created
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'messages'
    AND index_name = 'idx_message_conversation_created'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_message_conversation_created ON messages (conversation_id, created_at)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- message_attachments: idx_message_attachment_message
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'message_attachments'
    AND index_name = 'idx_message_attachment_message'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_message_attachment_message ON message_attachments (message_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;