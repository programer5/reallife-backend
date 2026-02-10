-- V3__drop_comment_duplicate_index.sql
-- Drop duplicate/overlapping index on comments if it exists
-- Keep: idx_comments_post_created_id (post_id, created_at, id)
-- Drop: idx_comment_post_created (post_id, created_at)

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'comments'
    AND index_name = 'idx_comment_post_created'
);

SET @sql := IF(
  @idx_exists > 0,
  'DROP INDEX idx_comment_post_created ON comments',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;