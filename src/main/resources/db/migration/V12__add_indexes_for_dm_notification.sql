-- V12__add_indexes_for_dm_notification.sql
-- Safe for MySQL: create index only if table exists AND index not exists

SET @db := DATABASE();

-- Helper pattern:
-- 1) check table exists
-- 2) check index exists
-- 3) prepare CREATE INDEX or SELECT 1

-- 1) messages(conversation_id, deleted, created_at, id)
SET @table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = @db AND table_name = 'messages'
);
SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'messages'
    AND index_name = 'idx_messages_conv_deleted_created_id'
);
SET @sql := IF(@table_exists = 1 AND @idx_exists = 0,
  'CREATE INDEX idx_messages_conv_deleted_created_id ON messages (conversation_id, deleted, created_at, id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) notifications(user_id, deleted, created_at, id)
SET @table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = @db AND table_name = 'notifications'
);
SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'notifications'
    AND index_name = 'idx_notifications_user_deleted_created_id'
);
SET @sql := IF(@table_exists = 1 AND @idx_exists = 0,
  'CREATE INDEX idx_notifications_user_deleted_created_id ON notifications (user_id, deleted, created_at, id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 3) conversation_member(conversation_id, user_id)  ✅ table 없으면 스킵
SET @table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = @db AND table_name = 'conversation_member'
);
SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'conversation_member'
    AND index_name = 'idx_conv_member_conv_user'
);
SET @sql := IF(@table_exists = 1 AND @idx_exists = 0,
  'CREATE INDEX idx_conv_member_conv_user ON conversation_member (conversation_id, user_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 4) conversation_member(user_id, conversation_id) ✅ table 없으면 스킵
SET @table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = @db AND table_name = 'conversation_member'
);
SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'conversation_member'
    AND index_name = 'idx_conv_member_user_conv'
);
SET @sql := IF(@table_exists = 1 AND @idx_exists = 0,
  'CREATE INDEX idx_conv_member_user_conv ON conversation_member (user_id, conversation_id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 5) conversations(last_message_at, id)
SET @table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = @db AND table_name = 'conversations'
);
SET @idx_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'conversations'
    AND index_name = 'idx_conversations_last_message_at'
);
SET @sql := IF(@table_exists = 1 AND @idx_exists = 0,
  'CREATE INDEX idx_conversations_last_message_at ON conversations (last_message_at, id)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;