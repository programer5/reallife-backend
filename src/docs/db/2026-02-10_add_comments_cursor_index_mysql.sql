/*
Purpose
- Optimize cursor pagination for comments list on MySQL.

Target query pattern (conceptually)
- WHERE post_id = ? AND deleted = false
- ORDER BY created_at DESC, id DESC
- Cursor fetch uses:
    (created_at < :createdAt) OR (created_at = :createdAt AND id < :id)

Recommended index
- (post_id, created_at, id)

Notes
- MySQL does NOT support "CREATE INDEX IF NOT EXISTS".
- This script is written to be idempotent by checking information_schema first,
  then creating the index only when it doesn't exist.
*/

-- Change these only if your schema name differs.
SET @schema_name := DATABASE();
SET @table_name := 'comments';
SET @index_name := 'idx_comments_post_created_id';

-- Check existence
SELECT COUNT(*) INTO @idx_exists
FROM information_schema.statistics
WHERE table_schema = @schema_name
  AND table_name = @table_name
  AND index_name = @index_name;

-- Create index only if missing
SET @ddl := IF(
  @idx_exists = 0,
  CONCAT('CREATE INDEX ', @index_name, ' ON ', @table_name, ' (post_id, created_at, id)'),
  CONCAT('SELECT ''SKIP: index ', @index_name, ' already exists''')
);

PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Optional: verify created
-- SHOW INDEX FROM comments;