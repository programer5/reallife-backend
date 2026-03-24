package com.example.backend.search.query;

import com.example.backend.config.search.SearchElasticProperties;
import com.example.backend.controller.search.dto.SearchResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticSearchQueryGateway {

    private final SearchElasticProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    public boolean supports() {
        return properties.isEnabled() && properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank();
    }

    public SearchResponse search(String query, String typeRaw, UUID conversationId, int limit) {
        if (!supports()) throw new IllegalStateException("Elastic search disabled");
        try {
            String url = normalizedBaseUrl() + "/" + properties.getIndexName() + "/_search";
            HttpEntity<String> entity = new HttpEntity<>(buildBody(query, typeRaw, conversationId, limit), headers());
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            return mapResponse(query, typeRaw, conversationId, limit, response.getBody());
        } catch (Exception e) {
            throw new IllegalStateException("Elastic search query failed: " + e.getMessage(), e);
        }
    }

    private String buildBody(String query, String typeRaw, UUID conversationId, int limit) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("size", Math.max(1, Math.min(limit * 4, 80)));

        ObjectNode bool = root.putObject("query").putObject("bool");
        String normalizedQuery = query == null ? "" : query.trim();
        ArrayNode must = bool.putArray("must");
        if (normalizedQuery.isBlank()) {
            must.addObject().putObject("match_all");
        } else {
            ObjectNode disMax = must.addObject().putObject("dis_max");
            disMax.put("tie_breaker", 0.18);
            ArrayNode queries = disMax.putArray("queries");

            ObjectNode exactPhrase = queries.addObject().putObject("multi_match");
            exactPhrase.put("query", normalizedQuery);
            ArrayNode exactFields = exactPhrase.putArray("fields");
            exactFields.add("title^9");
            exactFields.add("body^4");
            exactPhrase.put("type", "phrase");
            exactPhrase.put("boost", 8.5);

            ObjectNode phrasePrefix = queries.addObject().putObject("multi_match");
            phrasePrefix.put("query", normalizedQuery);
            ArrayNode prefixFields = phrasePrefix.putArray("fields");
            prefixFields.add("title.autocomplete^8");
            prefixFields.add("body.autocomplete^3");
            phrasePrefix.put("type", "bool_prefix");
            phrasePrefix.put("boost", 5.5);

            ObjectNode bestFields = queries.addObject().putObject("multi_match");
            bestFields.put("query", normalizedQuery);
            ArrayNode fields = bestFields.putArray("fields");
            fields.add("title^7");
            fields.add("body^2.2");
            fields.add("badge^2.0");
            fields.add("secondary^1.6");
            fields.add("tags^1.8");
            bestFields.put("type", "best_fields");
            bestFields.put("operator", "and");
            bestFields.put("minimum_should_match", minimumShouldMatch(normalizedQuery));

            ObjectNode titleTerm = queries.addObject().putObject("term").putObject("title.keyword");
            titleTerm.put("value", normalizedQuery);
            titleTerm.put("boost", 12.0);
        }

        ArrayNode filter = bool.putArray("filter");
        String normalizedType = normalizeType(typeRaw);
        if (!"ALL".equals(normalizedType)) {
            filter.addObject().putObject("term").put("type", normalizedType);
        }
        if (conversationId != null) {
            filter.addObject().putObject("term").put("conversationId", conversationId.toString());
        }

        ObjectNode highlight = root.putObject("highlight");
        highlight.put("pre_tags", "<mark>");
        highlight.put("post_tags", "</mark>");
        ObjectNode fieldsNode = highlight.putObject("fields");
        fieldsNode.putObject("title");
        fieldsNode.putObject("body");

        ArrayNode should = bool.putArray("should");
        should.addObject().putObject("term").putObject("type").put("value", normalizedType).put("boost", "ALL".equals(normalizedType) ? 1.0 : 1.6);
        should.addObject().putObject("term").putObject("type").put("value", "ACTIONS").put("boost", 1.22);
        should.addObject().putObject("term").putObject("type").put("value", "CAPSULES").put("boost", 1.15);
        should.addObject().putObject("term").putObject("type").put("value", "MESSAGES").put("boost", 1.10);
        should.addObject().putObject("term").putObject("type").put("value", "POSTS").put("boost", 1.04);
        ObjectNode recentBoost = should.addObject().putObject("range").putObject("createdAt");
        recentBoost.put("gte", "now-21d/d");
        recentBoost.put("boost", 1.14);

        ArrayNode sort = root.putArray("sort");
        sort.addObject().putObject("_score").put("order", "desc");
        sort.addObject().putObject("createdAt").put("order", "desc");
        return objectMapper.writeValueAsString(root);
    }

    private SearchResponse mapResponse(String query, String typeRaw, UUID conversationId, int limit, String body) throws Exception {
        JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
        List<SearchResponse.Item> items = new ArrayList<>();
        JsonNode hits = root.path("hits").path("hits");
        if (hits.isArray()) {
            for (JsonNode hit : hits) {
                JsonNode src = hit.path("_source");
                String type = text(src, "type");
                UUID id = uuid(src, "id", text(hit, "_id"));
                UUID convId = uuid(src, "conversationId", null);
                String title = text(src, "title");
                String snippet = text(src, "body");
                String highlight = extractHighlight(hit.path("highlight"), title, snippet);
                LocalDateTime createdAt = date(src.path("createdAt").asText(null));
                String deepLink = text(src, "deepLink");
                String badge = text(src, "badge");
                String secondary = text(src, "secondary");
                String anchorType = text(src, "anchorType");
                if (anchorType.isBlank()) anchorType = anchorType(type);
                UUID anchorId = uuid(src, "anchorId", text(src, "id"));
                Integer relevance = hit.hasNonNull("_score") ? Math.max(1, (int)Math.round(hit.path("_score").asDouble() * 100)) : null;
                items.add(new SearchResponse.Item(
                        type,
                        id,
                        shorten(title, 84),
                        shorten(snippet, 180),
                        shorten(highlight, 120),
                        createdAt,
                        convId,
                        deepLink,
                        badge,
                        secondary,
                        anchorType,
                        anchorId,
                        relevance
                ));
            }
        }
        Map<String, Long> countByType = items.stream().collect(Collectors.groupingBy(SearchResponse.Item::type, LinkedHashMap::new, Collectors.counting()));
        List<SearchResponse.Section> sections = List.of(
                new SearchResponse.Section("MESSAGES", "메시지", countByType.getOrDefault("MESSAGES", 0L)),
                new SearchResponse.Section("ACTIONS", "액션", countByType.getOrDefault("ACTIONS", 0L)),
                new SearchResponse.Section("CAPSULES", "캡슐", countByType.getOrDefault("CAPSULES", 0L)),
                new SearchResponse.Section("POSTS", "피드", countByType.getOrDefault("POSTS", 0L))
        );
        return new SearchResponse(query, normalizeType(typeRaw), conversationId, sections, items,
                new SearchResponse.Meta(true, "elasticsearch", limit, List.of("ALL", "MESSAGES", "ACTIONS", "CAPSULES", "POSTS")));
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.set("Authorization", "ApiKey " + properties.getApiKey().trim());
        } else if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            String raw = properties.getUsername() + ":" + safe(properties.getPassword());
            headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
        }
        return headers;
    }

    private String normalizedBaseUrl() {
        String base = properties.getBaseUrl();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }
    private String normalizeType(String raw) {
        if (raw == null || raw.isBlank()) return "ALL";
        return raw.trim().toUpperCase(Locale.ROOT);
    }
    private String text(JsonNode node, String field) {
        JsonNode n = node.path(field);
        return n.isMissingNode() || n.isNull() ? "" : n.asText("");
    }
    private UUID uuid(JsonNode node, String field, String fallback) {
        String value = text(node, field);
        if (value.isBlank()) value = safe(fallback);
        try { return value.isBlank() ? null : UUID.fromString(value); } catch (Exception e) { return null; }
    }
    private LocalDateTime date(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try { return LocalDateTime.parse(raw); } catch (Exception ignore) {}
        try { return OffsetDateTime.parse(raw).toLocalDateTime(); } catch (Exception ignore) {}
        return null;
    }
    private String extractHighlight(JsonNode highlight, String title, String body) {
        if (highlight != null && !highlight.isMissingNode()) {
            JsonNode titleNode = highlight.path("title");
            if (titleNode.isArray() && !titleNode.isEmpty()) return titleNode.get(0).asText("");
            JsonNode bodyNode = highlight.path("body");
            if (bodyNode.isArray() && !bodyNode.isEmpty()) return bodyNode.get(0).asText("");
        }
        return !safe(body).isBlank() ? body : title;
    }
    private String anchorType(String type) {
        return switch (safe(type).toUpperCase(Locale.ROOT)) {
            case "MESSAGES" -> "MESSAGE";
            case "ACTIONS" -> "PIN";
            case "CAPSULES" -> "CAPSULE";
            case "POSTS" -> "POST";
            default -> "NONE";
        };
    }
    private String shorten(String value, int max) {
        String v = safe(value).trim().replaceAll("\\s+", " ");
        if (v.length() <= max) return v;
        return v.substring(0, Math.max(0, max - 1)) + "…";
    }
    private String safe(String value) { return value == null ? "" : value; }

    private String minimumShouldMatch(String normalizedQuery) {
        if (normalizedQuery == null || normalizedQuery.isBlank()) return "100%";
        int length = normalizedQuery.codePointCount(0, normalizedQuery.length());
        if (length <= 2) return "100%";
        if (length <= 4) return "85%";
        return "75%";
    }
}
