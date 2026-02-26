-- conversation_pins: 약속/장소 자동 넛지(핀) MVP

CREATE TABLE IF NOT EXISTS conversation_pins (
                                                 id              BINARY(16)   NOT NULL,
    conversation_id BINARY(16)   NOT NULL,
    created_by      BINARY(16)   NOT NULL,

    type            VARCHAR(20)  NOT NULL,  -- SCHEDULE
    title           VARCHAR(100) NOT NULL,  -- "약속"
    place_text      VARCHAR(255) NULL,
    start_at        DATETIME(6)  NULL,
    remind_at       DATETIME(6)  NULL,

    status          VARCHAR(20)  NOT NULL,  -- ACTIVE|DONE|CANCELED

    deleted         TINYINT(1)   NOT NULL DEFAULT 0,
    created_at      DATETIME(6)  NOT NULL,
    update_at       DATETIME(6)  NOT NULL,

    PRIMARY KEY (id)
    );

-- indexes (있으면 스킵 / 없으면 생성)
SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'conversation_pins'
    AND index_name = 'idx_pins_conversation_active_created'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_pins_conversation_active_created ON conversation_pins (conversation_id, status, created_at)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @idx_exists := (
  SELECT COUNT(*)
  FROM information_schema.statistics
  WHERE table_schema = DATABASE()
    AND table_name = 'conversation_pins'
    AND index_name = 'idx_pins_remind'
);
SET @sql := IF(@idx_exists = 0,
  'CREATE INDEX idx_pins_remind ON conversation_pins (status, remind_at)',
  'SELECT 1'
);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;