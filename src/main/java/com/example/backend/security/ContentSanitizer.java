package com.example.backend.security;

public final class ContentSanitizer {
    private ContentSanitizer() {}

    // 최소 방어: 스크립트/이벤트핸들러 흔적 제거 (프론트에서도 반드시 escape 권장)
    public static String minimal(String input) {
        if (input == null) return null;
        String s = input;

        // <script ...> 제거 (아주 단순 방어)
        s = s.replaceAll("(?i)<\\s*script[^>]*>(.*?)<\\s*/\\s*script\\s*>", "");

        // onClick 같은 inline handler 흔적 제거
        s = s.replaceAll("(?i)on\\w+\\s*=", "");

        return s;
    }
}
