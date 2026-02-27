-- 같은 메시지에서 핀을 여러 번 생성하는 것을 방지하기 위한 컬럼/인덱스 추가
ALTER TABLE conversation_pins
    ADD COLUMN IF NOT EXISTS source_message_id UUID;

-- 이미 있는 데이터에는 NULL 유지 (과거 데이터)
-- 한 대화방 내에서 동일 메시지 기반 핀은 1개만 허용
CREATE UNIQUE INDEX IF NOT EXISTS uk_pins_conversation_source_message
    ON conversation_pins (conversation_id, source_message_id)
    WHERE deleted = false AND source_message_id IS NOT NULL;