-- 개인별 핀 숨김(dismiss) 저장
CREATE TABLE IF NOT EXISTS conversation_pin_dismissals (
                                                           id        BINARY(16)  NOT NULL,
    pin_id    BINARY(16)  NOT NULL,
    user_id   BINARY(16)  NOT NULL,

    created_at DATETIME(6) NOT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_pin_dismiss (pin_id, user_id),
    INDEX idx_pin_dismiss_user (user_id, created_at),
    INDEX idx_pin_dismiss_pin (pin_id, created_at)
    );