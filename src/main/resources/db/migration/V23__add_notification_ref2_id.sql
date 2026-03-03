-- V23__add_notification_ref2_id.sql (MySQL 8.x safe, idempotent)

-- 1) add column if missing
SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'ALTER TABLE notifications ADD COLUMN ref2_id BINARY(16) NULL',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'notifications'
    AND COLUMN_NAME = 'ref2_id'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) create index if missing
SET @sql := (
  SELECT IF(
    COUNT(*) = 0,
    'CREATE INDEX idx_notification_user_type_ref2 ON notifications (user_id, type, ref2_id)',
    'SELECT 1'
  )
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'notifications'
    AND INDEX_NAME = 'idx_notification_user_type_ref2'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;