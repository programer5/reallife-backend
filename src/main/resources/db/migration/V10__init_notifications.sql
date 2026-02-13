-- V10__init_notifications.sql
-- ddl-auto=validate를 만족시키기 위해 notifications 테이블을 Flyway로 생성

CREATE TABLE IF NOT EXISTS notifications (
                                             id BINARY(16) NOT NULL,
    user_id BINARY(16) NOT NULL,
    type VARCHAR(30) NOT NULL,
    ref_id BINARY(16) NOT NULL,
    body VARCHAR(300) NOT NULL,
    read_at DATETIME(6) NULL,

    created_at DATETIME(6) NOT NULL,
    update_at DATETIME(6) NOT NULL,
    deleted TINYINT(1) NOT NULL DEFAULT 0,

    PRIMARY KEY (id)
    );

-- 인덱스 (엔티티 @Index와 맞춤)
CREATE INDEX idx_notification_user_created ON notifications (user_id, created_at);
CREATE INDEX idx_notification_user_read ON notifications (user_id, read_at);
CREATE INDEX idx_notification_user_deleted ON notifications (user_id, deleted);