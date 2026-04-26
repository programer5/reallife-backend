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
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConversationAiActionService {

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
            String query = safe(String.valueOf(payload.getOrDefault("query", text)));
            String url = query.isBlank()
                    ? null
                    : "https://www.google.com/maps/search/?api=1&query=" + UriUtils.encode(query, StandardCharsets.UTF_8);
            return new AiActionExecuteResponse("ok", type, "지도를 열었어요", url, payload);
        }

        if ("schedule".equals(type) || "reminder".equals(type) || "notify".equals(type)) {
            ConversationPinResponse pin = pinService.createAiPin(
                    userId,
                    request.conversationId(),
                    request.messageId(),
                    text,
                    safeString(payload.get("title")),
                    parseStartAt(payload.get("startAt")),
                    safeString(payload.get("placeText")),
                    parseRemindMinutes(payload.get("remindMinutes"))
            );
            payload.put("pinId", pin.pinId());
            payload.put("startAt", pin.startAt());
            payload.put("remindAt", pin.remindAt());
            String label = "schedule".equals(type) ? "일정을 만들었어요" : "알림을 만들었어요";
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

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String summarize(String value) {
        String v = safe(value);
        if (v.length() <= 80) return v;
        return v.substring(0, 80) + "…";
    }
}
