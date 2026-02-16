-- V14__notifications_unique_index.sql
-- notifications 중복 방지: (user_id, type, ref_id, deleted) UNIQUE
-- MySQL: index 존재 여부 확인 후 생성

SET @db := DATABASE();

SET @table_exists := (
  SELECT COUNT(1)
  FROM information_schema.tables
  WHERE table_schema = @db AND table_name = 'notifications'
);

SET @uq_exists := (
  SELECT COUNT(1)
  FROM information_schema.statistics
  WHERE table_schema = @db AND table_name = 'notifications'
    AND index_name = 'uq_notifications_user_type_ref_deleted'
);

SET @sql := IF(@table_exists = 1 AND @uq_exists = 0,
  'ALTER TABLE notifications ADD UNIQUE INDEX uq_notifications_user_type_ref_deleted (user_id, type, ref_id, deleted)',
  'SELECT 1'
);

PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;