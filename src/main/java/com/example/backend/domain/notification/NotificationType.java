package com.example.backend.domain.notification;

public enum NotificationType {
    MESSAGE_RECEIVED, // ✅ 메시지 수신
    POST_LIKE,
    POST_COMMENT,
    FOLLOW,

    // ✅ NEW: Conversation Pins
    PIN_CREATED,  // 핀 생성 즉시 알림(인박스)
    PIN_REMIND    // 1시간 전 알림(인박스)
}