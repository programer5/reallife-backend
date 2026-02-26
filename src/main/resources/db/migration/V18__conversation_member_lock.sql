-- DM 잠금(개인별) 기능용 컬럼 추가
-- ddl-auto=validate 환경에서 부팅 가능하도록 Flyway로 스키마 보강

ALTER TABLE conversation_members
    ADD COLUMN lock_enabled TINYINT(1) NOT NULL DEFAULT 0,
  ADD COLUMN lock_password_hash VARCHAR(255) NULL,
  ADD COLUMN lock_version VARCHAR(36) NULL;

-- (선택) 잠금 조회가 잦아지면 도움이 됨
-- CREATE INDEX idx_conv_member_lock_enabled ON conversation_members (conversation_id, user_id, lock_enabled);