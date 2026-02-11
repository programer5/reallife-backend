-- V4__add_comments_update_at.sql
-- comments 테이블에 update_at 컬럼 추가 (없으면 추가)

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'comments'
    AND column_name = 'update_at'
);

SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE comments ADD COLUMN update_at DATETIME(6) NULL',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;