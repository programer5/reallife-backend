package com.example.backend.sse;

/**
 * SSE 이벤트 id 규칙을 한 곳에서 관리하기 위한 유틸
 *
 * - message 관련: msg:{uuid}
 * - notification 관련: noti:{uuid}
 *
 * 장점:
 * - id만 봐도 어떤 이벤트인지 즉시 알 수 있음
 * - 서로 다른 이벤트 타입 간 id 충돌/혼동 방지
 * - Last-Event-ID가 "진짜 스트림 커서"처럼 명확해짐
 */
public final class SseEventIds {

    private SseEventIds() {}

    public static String format(String eventName, String rawId) {
        if (rawId == null || rawId.isBlank()) return null;
        if (eventName == null || eventName.isBlank()) return rawId;

        return switch (eventName) {
            case "message-created", "message-deleted" -> "msg:" + rawId;
            case "notification-created" -> "noti:" + rawId;
            default -> rawId; // 다른 이벤트는 그대로
        };
    }
}