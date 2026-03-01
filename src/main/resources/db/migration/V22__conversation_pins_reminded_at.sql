-- V22__conversation_pins_reminded_at.sql
-- PIN_REMIND 중복 처리 방지용 reminded_at 추가

-- 컬럼 존재 여부 체크 후 없으면 추가
SET @col_exists := (
  SELECT COUNT(*)
  FROM information_schema.columns
  WHERE table_schema = DATABASE()
    AND table_name = 'conversation_pins'
    AND column_name = 'reminded_at'
);

SET @sql := IF(@col_exists = 0,
  'ALTER TABLE conversation_pins ADD COLUMN reminded_at DATETIME(6) NULL AFTER remind_at',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 인덱스(있으면 스킵 / 없으면 생성)
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'conversation_pins'
    AND index_name = 'idx_pins_remind_pending'
);

SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_pins_remind_pending ON conversation_pins (status, remind_at, reminded_at)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;