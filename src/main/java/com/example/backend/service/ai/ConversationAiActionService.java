package com.example.backend.service.ai;

import com.example.backend.controller.ai.dto.AiActionExecuteRequest;
import com.example.backend.controller.ai.dto.AiActionExecuteResponse;
import com.example.backend.controller.message.dto.ConversationPinResponse;
import com.example.backend.service.pin.ConversationPinService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ConversationAiActionService {

    private static final Pattern KOREAN_HOUR_PATTERN = Pattern.compile("(오전|오후)?\\s*(\\d{1,2})\\s*시");

    private final ConversationPinService pinService;

    @Transactional
    public AiActionExecuteResponse execute(UUID userId, AiActionExecuteRequest request) {
        String type = normalize(request.type());
        String text = safe(request.text());
        Map<String, Object> payload = new LinkedHashMap<>();
        if (request.payload() != null) payload.putAll(request.payload());
        payload.putIfAbsent("conversationId", request.conversationId());
        payload.putIfAbsent("messageId", request.messageId());
        payload.putIfAbsent("sourceText", text);

        if ("map".equals(type)) {
            String query = firstText(
                    safeString(payload.get("query")),
                    safeString(payload.get("placeText")),
                    text
            );
            String url = query.isBlank()
                    ? null
                    : "https://www.google.com/maps/search/?api=1&query=" + UriUtils.encode(query, StandardCharsets.UTF_8);
            payload.put("query", query);
            return new AiActionExecuteResponse("ok", type, "지도를 열었어요", url, payload);
        }

        if ("schedule".equals(type) || "reminder".equals(type) || "notify".equals(type)) {
            LocalDateTime inferredStartAt = firstNonNull(
                    parseStartAt(payload.get("startAt")),
                    inferStartAt(text, type)
            );
            String inferredTitle = firstText(
                    safeString(payload.get("title")),
                    "schedule".equals(type) ? "대화 일정" : "대화 다시 보기"
            );
            Integer remindMinutes = parseRemindMinutes(payload.get("remindMinutes"));
            if (remindMinutes == null) remindMinutes = "schedule".equals(type) ? 60 : 30;

            ConversationPinResponse pin = pinService.createAiPin(
                    userId,
                    request.conversationId(),
                    request.messageId(),
                    text,
                    inferredTitle,
                    inferredStartAt,
                    firstText(safeString(payload.get("placeText")), extractPlaceHint(text)),
                    remindMinutes
            );
            payload.put("pinId", pin.pinId());
            payload.put("startAt", pin.startAt());
            payload.put("remindAt", pin.remindAt());
            payload.put("title", inferredTitle);
            payload.put("remindMinutes", remindMinutes);
            String label = "schedule".equals(type) ? "일정을 만들었어요" : "이따 볼 알림을 만들었어요";
            String targetUrl = "/inbox/conversations/" + pin.conversationId() + "/pins?pinId=" + pin.pinId();
            return new AiActionExecuteResponse("ok", type, label, targetUrl, payload);
        }

        return new AiActionExecuteResponse("ok", type.isBlank() ? "focus" : type, "답장 입력으로 이어가요", null, payload);
    }

    private String safeString(Object value) {
        if (value == null) return null;
        String v = String.valueOf(value).trim();
        return v.isBlank() ? null : v;
    }

    private LocalDateTime parseStartAt(Object value) {
        String v = safeString(value);
        if (v == null) return null;
        try {
            return LocalDateTime.parse(v);
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer parseRemindMinutes(Object value) {
        String v = safeString(value);
        if (v == null) return null;
        try {
            return Integer.parseInt(v);
        } catch (Exception ignored) {
            return null;
        }
    }

    private LocalDateTime inferStartAt(String text, String type) {
        LocalDateTime now = LocalDateTime.now();
        String lower = safe(text).toLowerCase();

        if (lower.contains("이따") || lower.contains("나중") || lower.contains("다시 얘기")) {
            return now.plusHours(2).withSecond(0).withNano(0);
        }

        LocalDate date = lower.contains("내일") ? LocalDate.now().plusDays(1) : LocalDate.now();
        Integer hour = extractHour(lower);
        if (hour != null) {
            return LocalDateTime.of(date, LocalTime.of(hour, 0));
        }

        if ("schedule".equals(type)) {
            return now.plusDays(1).withHour(19).withMinute(0).withSecond(0).withNano(0);
        }
        return now.plusHours(2).withSecond(0).withNano(0);
    }

    private Integer extractHour(String text) {
        Matcher matcher = KOREAN_HOUR_PATTERN.matcher(text);
        if (!matcher.find()) return null;
        String ampm = matcher.group(1);
        int hour = Integer.parseInt(matcher.group(2));
        if ("오후".equals(ampm) && hour < 12) hour += 12;
        if ("오전".equals(ampm) && hour == 12) hour = 0;
        if (hour < 0 || hour > 23) return null;
        return hour;
    }

    private String extractPlaceHint(String text) {
        String value = safe(text);
        for (String keyword : new String[]{"카페", "커피숍", "식당", "역", "강남", "모란"}) {
            int index = value.indexOf(keyword);
            if (index >= 0) {
                int start = Math.max(0, index - 8);
                int end = Math.min(value.length(), index + keyword.length() + 8);
                return value.substring(start, end).trim();
            }
        }
        return null;
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) return value;
        }
        return null;
    }

    private String firstText(String... values) {
        for (String value : values) {
            String v = safe(value);
            if (!v.isBlank()) return v;
        }
        return "";
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
