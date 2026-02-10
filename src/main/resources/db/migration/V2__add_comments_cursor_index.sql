-- V2__add_comments_cursor_index.sql
-- Create cursor-friendly index if it doesn't exist
-- idx_comments_post_created_id (post_id, created_at, id)

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'comments'
    AND index_name = 'idx_comments_post_created_id'
);

SET @sql := IF(
  @idx_exists = 0,
  'CREATE INDEX idx_comments_post_created_id ON comments (post_id, created_at, id)',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;