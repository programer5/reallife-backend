-- DIRECT 중복 방지용: (user1_id, user2_id) UNIQUE
-- user1_id/user2_id는 항상 min/max 정렬해서 저장

CREATE TABLE IF NOT EXISTS direct_conversation_keys (
                                                        conversation_id BINARY(16)  NOT NULL,
    user1_id        BINARY(16)  NOT NULL,
    user2_id        BINARY(16)  NOT NULL,
    deleted         TINYINT(1)  NOT NULL DEFAULT 0,
    created_at      DATETIME(6) NOT NULL,
    update_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (conversation_id),
    CONSTRAINT uk_direct_pair UNIQUE (user1_id, user2_id)
    );

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'direct_conversation_keys'
    AND index_name = 'idx_direct_user1'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_direct_user1 ON direct_conversation_keys (user1_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'direct_conversation_keys'
    AND index_name = 'idx_direct_user2'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_direct_user2 ON direct_conversation_keys (user2_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;