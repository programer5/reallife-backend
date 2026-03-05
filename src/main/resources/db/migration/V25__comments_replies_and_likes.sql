-- V25__comments_replies_and_likes.sql
-- Replies (parent_comment_id) + like_count + comment_likes table

-- 1) comments.parent_comment_id
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'comments'
    AND column_name = 'parent_comment_id'
);
SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE comments ADD COLUMN parent_comment_id BINARY(16) NULL AFTER author_id',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 2) comments.like_count
SET @col_exists2 := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'comments'
    AND column_name = 'like_count'
);
SET @sql2 := IF(
  @col_exists2 = 0,
  'ALTER TABLE comments ADD COLUMN like_count BIGINT NOT NULL DEFAULT 0 AFTER content',
  'SELECT 1'
);
PREPARE stmt2 FROM @sql2; EXECUTE stmt2; DEALLOCATE PREPARE stmt2;

-- 3) indexes
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'comments'
    AND index_name = 'idx_comment_post_parent_created'
);
SET @sql3 := IF(
  @idx_exists = 0,
  'CREATE INDEX idx_comment_post_parent_created ON comments (post_id, parent_comment_id, created_at)',
  'SELECT 1'
);
PREPARE stmt3 FROM @sql3; EXECUTE stmt3; DEALLOCATE PREPARE stmt3;

SET @idx_exists4 := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'comments'
    AND index_name = 'idx_comment_post_like_created'
);
SET @sql4 := IF(
  @idx_exists4 = 0,
  'CREATE INDEX idx_comment_post_like_created ON comments (post_id, like_count, created_at)',
  'SELECT 1'
);
PREPARE stmt4 FROM @sql4; EXECUTE stmt4; DEALLOCATE PREPARE stmt4;

-- 4) comment_likes table
CREATE TABLE IF NOT EXISTS comment_likes (
  id BINARY(16) NOT NULL,
  comment_id BINARY(16) NOT NULL,
  user_id BINARY(16) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  deleted BIT(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (id),
  CONSTRAINT uk_comment_user_like UNIQUE (comment_id, user_id),
  INDEX idx_comment_like_comment_id (comment_id),
  INDEX idx_comment_like_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
