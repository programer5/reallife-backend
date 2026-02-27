-- V21__conversation_pins_source_message_id.sql (MySQL 8, UUID=CHAR(36))

-- 1) 컬럼 추가 (MySQL은 IF NOT EXISTS 지원 X → INFORMATION_SCHEMA로 우회)
SET @col_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'conversation_pins'
    AND COLUMN_NAME = 'source_message_id'
);

SET @sql := IF(
  @col_exists = 0,
  'ALTER TABLE conversation_pins ADD COLUMN source_message_id CHAR(36) NULL',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2) 유니크 인덱스 (Postgres partial unique index 대체)
-- MySQL은 partial index가 없으니 "생성 컬럼"으로 조건을 인덱스에 포함시키는 방식이 가장 깔끔함.
-- deleted=false AND source_message_id IS NOT NULL 조건을 만족할 때만 키가 생기게 만들어 유니크 제약을 걸어준다.

SET @idx_exists := (
  SELECT COUNT(*)
  FROM INFORMATION_SCHEMA.STATISTICS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'conversation_pins'
    AND INDEX_NAME = 'uk_pins_conversation_source_message'
);

SET @sql := IF(
  @idx_exists = 0,
  '
  ALTER TABLE conversation_pins
    ADD COLUMN source_message_key CHAR(36)
      GENERATED ALWAYS AS (
        CASE
          WHEN deleted = 0 AND source_message_id IS NOT NULL THEN source_message_id
          ELSE NULL
        END
      ) STORED,
    ADD UNIQUE INDEX uk_pins_conversation_source_message (conversation_id, source_message_key)
  ',
  'SELECT 1'
);

PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;