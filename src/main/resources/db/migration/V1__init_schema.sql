-- V1__init_schema.sql
-- 빈 DB(도커 등)에서도 앱이 부팅되도록 최소 스키마를 생성한다.
-- (현재는 comments 테이블 + author 인덱스만 최소로 생성)

CREATE TABLE IF NOT EXISTS comments (
                                        id BINARY(16) NOT NULL,
    post_id BINARY(16) NOT NULL,
    author_id BINARY(16) NOT NULL,
    content VARCHAR(1000) NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
    );

-- idx_comment_author (author_id) : 있으면 스킵, 없으면 생성
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'comments'
    AND index_name = 'idx_comment_author'
);

SET @sql := IF(
  @idx_exists = 0,
  'CREATE INDEX idx_comment_author ON comments (author_id)',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;