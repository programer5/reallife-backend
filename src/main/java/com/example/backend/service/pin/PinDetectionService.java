package com.example.backend.service.pin;

import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PinDetectionService {

    // 시간 패턴
    private static final Pattern P_TIME_HOUR =
            Pattern.compile("(오전|오후)?\\s*(\\d{1,2})\\s*시(\\s*(\\d{1,2})\\s*분)?");
    private static final Pattern P_TIME_COLON =
            Pattern.compile("(\\d{1,2})\\s*:\\s*(\\d{2})");

    // 날짜 패턴
    private static final Pattern P_DATE_SLASH =
            Pattern.compile("(\\d{1,2})\\s*/\\s*(\\d{1,2})"); // 2/28
    private static final Pattern P_DATE_DASH =
            Pattern.compile("(\\d{4})\\s*-\\s*(\\d{1,2})\\s*-\\s*(\\d{1,2})"); // 2026-02-28
    private static final Pattern P_RELATIVE_DAY =
            Pattern.compile("(오늘|내일|모레)");

    // 약속 맥락(오탐 방지)
    private static final Pattern P_INTENT =
            Pattern.compile("(보자|만나|약속|갈까|볼까|하자|모이|회식|밥|점심|저녁|술)");

    // ✅ 기존 코드 호환용 결과 타입
    public record DetectionResult(
            boolean detected,
            String type,      // SCHEDULE
            String title,     // "약속"
            String placeText, // nullable
            LocalDateTime startAt,  // nullable
            LocalDateTime remindAt  // nullable
    ) {}

    // ✅ 기존 호출부(ConversationPinService)가 쓰는 시그니처 유지
    public Optional<DetectionResult> detect(String message) {
        // 서버는 KST 기준으로 동작한다고 가정(프로젝트가 KR 서비스)
        ZoneId zoneId = ZoneId.of("Asia/Seoul");
        LocalDate today = LocalDate.now(zoneId);
        return detect(message, zoneId, today);
    }

    // ✅ 새 로직(테스트 주입 가능)
    public Optional<DetectionResult> detect(String message, ZoneId zoneId, LocalDate today) {
        if (message == null) return Optional.empty();

        String text = normalize(message);
        if (text.isBlank()) return Optional.empty();

        // 1) 시간 필수
        TimeParse tp = parseTime(text);
        if (tp == null) return Optional.empty();

        // 2) 날짜(없으면 today)
        LocalDate date = parseDate(text, today);
        LocalDateTime startAt = LocalDateTime.of(date, tp.time);

        // 3) 장소 추정
        String place = extractPlaceAfter(text, tp.matchEndIndex);

        // 4) 오탐 방지: 의도도 없고 장소도 없으면 생성 X
        boolean hasIntent = P_INTENT.matcher(text).find();
        boolean hasPlace = place != null && !place.isBlank();
        if (!hasIntent && !hasPlace) return Optional.empty();

        return Optional.of(new DetectionResult(
                true,
                "SCHEDULE",
                "약속",
                hasPlace ? place : null,
                startAt,
                startAt.minusHours(1)
        ));
    }

    // ---------- helpers ----------

    private static String normalize(String s) {
        return s
                .replace("\u00A0", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase(Locale.KOREAN);
    }

    private static LocalDate parseDate(String text, LocalDate today) {
        Matcher dash = P_DATE_DASH.matcher(text);
        if (dash.find()) {
            int y = Integer.parseInt(dash.group(1));
            int m = Integer.parseInt(dash.group(2));
            int d = Integer.parseInt(dash.group(3));
            return safeDate(y, m, d).orElse(today);
        }

        Matcher slash = P_DATE_SLASH.matcher(text);
        if (slash.find()) {
            int m = Integer.parseInt(slash.group(1));
            int d = Integer.parseInt(slash.group(2));
            return safeDate(today.getYear(), m, d).orElse(today);
        }

        Matcher rel = P_RELATIVE_DAY.matcher(text);
        if (rel.find()) {
            return switch (rel.group(1)) {
                case "오늘" -> today;
                case "내일" -> today.plusDays(1);
                case "모레" -> today.plusDays(2);
                default -> today;
            };
        }

        return today;
    }

    private static Optional<LocalDate> safeDate(int y, int m, int d) {
        try {
            return Optional.of(LocalDate.of(y, m, d));
        } catch (DateTimeException e) {
            return Optional.empty();
        }
    }

    private static TimeParse parseTime(String text) {
        Matcher colon = P_TIME_COLON.matcher(text);
        if (colon.find()) {
            int h = Integer.parseInt(colon.group(1));
            int mm = Integer.parseInt(colon.group(2));
            if (h < 0 || h > 23) return null;
            if (mm < 0 || mm > 59) return null;
            return new TimeParse(LocalTime.of(h, mm), colon.end());
        }

        Matcher hour = P_TIME_HOUR.matcher(text);
        if (hour.find()) {
            String ampm = hour.group(1);
            int h = Integer.parseInt(hour.group(2));
            String minStr = hour.group(4);
            int mm = (minStr == null) ? 0 : Integer.parseInt(minStr);

            if (h < 0 || h > 12) return null;
            if (mm < 0 || mm > 59) return null;

            int hh = h;
            if ("오후".equals(ampm) && h < 12) hh = h + 12;
            if ("오전".equals(ampm) && h == 12) hh = 0;

            if (hh < 0 || hh > 23) return null;

            return new TimeParse(LocalTime.of(hh, mm), hour.end());
        }

        return null;
    }

    private static String extractPlaceAfter(String text, int timeEndIdx) {
        if (timeEndIdx < 0 || timeEndIdx >= text.length()) return "";

        String tail = text.substring(timeEndIdx).trim();
        if (tail.isBlank()) return "";

        // 조사/접속어 정리
        tail = tail.replaceAll("^(에|에서|로|으로|랑|하고|과|와)\\s*", "").trim();

        // 의도/기타 단어가 나오기 전까지만 장소로 추정
        int cut = indexOfAny(tail,
                "보자", "만나", "약속", "갈까", "볼까", "하자", "모이",
                "연락", "전화", "가능", "까지"
        );
        if (cut >= 0) tail = tail.substring(0, cut).trim();

        // 종결 조사 제거
        tail = tail.replaceAll("\\s*(에서|에|로|으로)\\s*$", "").trim();

        if (tail.length() > 25) tail = tail.substring(0, 25).trim();
        if (tail.length() < 2) return "";
        if (tail.matches("\\d+[\\d:\\s]*")) return "";

        return tail;
    }

    private static int indexOfAny(String s, String... needles) {
        int best = -1;
        for (String n : needles) {
            int i = s.indexOf(n);
            if (i >= 0 && (best == -1 || i < best)) best = i;
        }
        return best;
    }

    private record TimeParse(LocalTime time, int matchEndIndex) {}
}