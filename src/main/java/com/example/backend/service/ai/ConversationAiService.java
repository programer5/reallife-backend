package com.example.backend.service.ai;

import com.example.backend.controller.ai.dto.AiActionSuggestion;
import com.example.backend.controller.ai.dto.AiReplyResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class ConversationAiService {

    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    @Value("${OPENAI_API_KEY:}")
    private String openAiApiKey;

    @Value("${OPENAI_MODEL:gpt-4o-mini}")
    private String openAiModel;

    public ConversationAiService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl("https://api.openai.com/v1")
                .build();
    }

    public AiReplyResponse suggest(String text) {
        String clean = safe(text);
        if (clean.isBlank()) return fallback(clean, "rule");

        if (openAiApiKey == null || openAiApiKey.isBlank()) {
            return fallback(clean, "rule");
        }

        try {
            AiReplyResponse response = callOpenAi(clean);
            if (response.replies().isEmpty() && response.actions().isEmpty()) {
                return fallback(clean, "rule-empty");
            }
            return response;
        } catch (Exception e) {
            log.warn("AI reply fallback used. reason={}", e.getMessage());
            return fallback(clean, "rule-fallback");
        }
    }

    private AiReplyResponse callOpenAi(String text) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", openAiModel);
        payload.put("temperature", 0.35);
        payload.put("messages", List.of(
                Map.of(
                        "role", "system",
                         "content", "너는 한국어 메신저 앱의 짧은 추천 답장/액션 생성기다. 반드시 JSON만 반환한다. 스키마: {\"replies\":[문자열 최대3개],\"actions\":[{\"type\":\"schedule|reminder|map|notify|focus\",\"label\":\"짧은 버튼명\",\"payload\":{\"title\":\"선택\",\"placeText\":\"선택\",\"startAt\":\"yyyy-MM-ddTHH:mm:ss 선택\",\"remindMinutes\":30}} 최대3개]}. 일정/약속/나중에/이따/다시 보기 맥락은 reminder 또는 schedule 액션을 추천한다. 답장은 자연스럽고 짧게."
                ),
                Map.of("role", "user", "content", text)
        ));

        JsonNode root = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + openAiApiKey)
                .body(payload)
                .retrieve()
                .body(JsonNode.class);

        String content = root == null ? "" : root.at("/choices/0/message/content").asText("");
        JsonNode json = objectMapper.readTree(stripCodeFence(content));

        List<String> replies = new ArrayList<>();
        JsonNode replyNode = json.path("replies");
        if (replyNode.isArray()) {
            for (JsonNode n : replyNode) {
                String v = safe(n.asText(""));
                if (!v.isBlank() && replies.size() < 3) replies.add(v);
            }
        }

        List<AiActionSuggestion> actions = new ArrayList<>();
        JsonNode actionNode = json.path("actions");
        if (actionNode.isArray()) {
            for (JsonNode n : actionNode) {
                if (actions.size() >= 3) break;
                String type = safe(n.path("type").asText(""));
                String label = safe(n.path("label").asText(""));
                if (type.isBlank() || label.isBlank()) continue;
                Map<String, Object> actionPayload = objectMapper.convertValue(n.path("payload"), Map.class);
                actions.add(new AiActionSuggestion(type, label, actionPayload == null ? Map.of() : actionPayload));
            }
        }
        return new AiReplyResponse(replies, actions, "openai");
    }

    private AiReplyResponse fallback(String text, String source) {
        String lower = text == null ? "" : text.toLowerCase();
        List<String> replies = new ArrayList<>();
        List<AiActionSuggestion> actions = new ArrayList<>();

        if (containsAny(lower, "약속", "만나", "보자", "시간", "내일", "오늘", "오후", "오전")) {
            replies.addAll(List.of("좋아, 시간 맞춰 갈게", "확인했어", "조금만 이따 다시 볼게"));
            actions.add(new AiActionSuggestion("schedule", "📅 일정", Map.of("source", text)));
            actions.add(new AiActionSuggestion("reminder", "⏰ 알림", Map.of("source", text)));
        } else if (containsAny(lower, "어디", "위치", "장소", "카페", "식당", "역")) {
            replies.addAll(List.of("어디로 가면 돼?", "위치 보내줘", "확인했어"));
            actions.add(new AiActionSuggestion("map", "📍 지도", Map.of("query", text)));
        } else if (text != null && text.contains("?")) {
            replies.addAll(List.of("확인해볼게", "응, 좋아", "조금만 기다려줘"));
            actions.add(new AiActionSuggestion("focus", "💬 답장", Map.of()));
        } else {
            replies.addAll(List.of("좋아", "확인했어", "이따 얘기하자"));
            actions.add(new AiActionSuggestion("notify", "🔔 알림", Map.of("source", text)));
        }
        return new AiReplyResponse(replies.stream().limit(3).toList(), actions.stream().limit(3).toList(), source);
    }

    private boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String stripCodeFence(String value) {
        String v = safe(value);
        if (v.startsWith("```")) {
            v = v.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        return v;
    }
}
