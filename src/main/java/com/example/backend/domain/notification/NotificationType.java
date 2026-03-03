package com.example.backend.domain.notification;

public enum NotificationType {

    // 메시지
    MESSAGE_RECEIVED,   // 새 메시지 수신

    // 소셜
    POST_LIKE,
    POST_COMMENT,
    FOLLOW,

    // Conversation Pins
    PIN_CREATED,        // 핀 생성
    PIN_REMIND,         // 일정 리마인드

    // 앞으로 확장용 (지금 바로 안 써도 안전)
    PIN_DONE,           // 핀 완료
    PIN_CANCELED,       // 핀 취소
    PIN_DISMISSED,       // 핀 숨김
    PIN_UPDATED         // 핀 수정
}