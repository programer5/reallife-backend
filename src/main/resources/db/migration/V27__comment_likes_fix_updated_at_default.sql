-- V27__comment_likes_fix_updated_at_default.sql
-- comment_likes.updated_at 컬럼이 NOT NULL인데 DEFAULT가 없어 insert 시 실패하는 문제 해결
-- (현재 엔티티는 update_at 컬럼을 사용 중이라, updated_at은 DB 기본값으로 채움)

SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'comment_likes'
    AND column_name = 'updated_at'
);

SET @sql := IF(
  @col_exists = 1,
  'ALTER TABLE comment_likes MODIFY COLUMN updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)',
  'SELECT 1'
);

PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
