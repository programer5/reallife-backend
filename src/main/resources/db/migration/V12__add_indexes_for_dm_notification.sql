-- V12__add_indexes_for_dm_notification.sql

-- 1) messages: 커서 페이징 핵심 인덱스
-- WHERE conversation_id = ? AND deleted = 0
-- ORDER BY created_at DESC, id DESC
CREATE INDEX idx_messages_conv_deleted_created_id
    ON messages (conversation_id, deleted, created_at, id);

-- 2) message_hidden: 나만 삭제(숨김) NOT EXISTS 최적화
-- 이미 UK(user_id, message_id)가 있으면 사실상 충분하지만,
-- 실행계획이 흔들릴 때를 대비해 조회용 인덱스 명시(UK가 있으면 생략 가능)
CREATE INDEX idx_message_hidden_user_message
    ON message_hidden (user_id, message_id);

-- 3) notifications: 유저별 목록 커서 페이징
CREATE INDEX idx_notifications_user_deleted_created_id
    ON notifications (user_id, deleted, created_at, id);

-- 4) conversation_member: 멤버십 조회/권한 체크/대화방 목록 조인 최적화
-- existsByConversationIdAndUserId, findUserIdsByConversationId, updateLastReadAtIfLater 등에 도움
CREATE INDEX idx_conv_member_conv_user
    ON conversation_member (conversation_id, user_id);

CREATE INDEX idx_conv_member_user_conv
    ON conversation_member (user_id, conversation_id);

-- 5) conversations: 최근 대화방 정렬(있다면)
-- 대화방 목록을 last_message_at 기준으로 정렬한다면 도움
CREATE INDEX idx_conversations_last_message_at
    ON conversations (last_message_at, id);