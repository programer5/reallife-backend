package com.example.backend.search.index;

import com.example.backend.config.search.SearchElasticProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.search.elastic", name = "enabled", havingValue = "true")
public class ElasticSearchIndexGateway implements SearchIndexGateway {

    private final SearchElasticProperties properties;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public boolean isReady() {
        return properties.isEnabled() && properties.getBaseUrl() != null && !properties.getBaseUrl().isBlank();
    }

    @Override
    public void upsert(SearchIndexDocument document) {
        if (!isReady() || document == null || document.id() == null) return;
        try {
            String url = normalizedBaseUrl() + "/" + properties.getIndexName() + "/_doc/" + document.id();
            HttpEntity<String> entity = new HttpEntity<>(buildPayload(document), headers());
            restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);
        } catch (Exception e) {
            log.warn("search index upsert failed. type={}, id={}, message={}", document.type(), document.id(), e.getMessage());
        }
    }

    @Override
    public void delete(String type, UUID id) {
        if (!isReady() || id == null) return;
        try {
            String url = normalizedBaseUrl() + "/" + properties.getIndexName() + "/_doc/" + id;
            restTemplate.exchange(url, HttpMethod.DELETE, new HttpEntity<>(headers()), String.class);
        } catch (Exception e) {
            log.warn("search index delete failed. type={}, id={}, message={}", type, id, e.getMessage());
        }
    }

    private String buildPayload(SearchIndexDocument document) throws Exception {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("type", document.type());
        if (document.id() != null) root.put("id", document.id().toString());
        if (document.conversationId() != null) root.put("conversationId", document.conversationId().toString());
        root.put("title", n(document.title()));
        root.put("body", n(document.body()));
        root.put("badge", n(document.badge()));
        root.put("secondary", n(document.secondary()));
        root.put("anchorType", n(document.anchorType()));
        if (document.anchorId() != null) root.put("anchorId", document.anchorId().toString());
        root.put("deepLink", n(document.deepLink()));
        if (document.createdAt() != null) root.put("createdAt", document.createdAt().toString());
        if (document.updatedAt() != null) root.put("updatedAt", document.updatedAt().toString());
        var tags = root.putArray("tags");
        if (document.tags() != null) {
            for (String tag : document.tags()) tags.add(n(tag));
        }
        return objectMapper.writeValueAsString(root);
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (properties.getApiKey() != null && !properties.getApiKey().isBlank()) {
            headers.set("Authorization", "ApiKey " + properties.getApiKey().trim());
        } else if (properties.getUsername() != null && !properties.getUsername().isBlank()) {
            String raw = properties.getUsername() + ":" + n(properties.getPassword());
            headers.set("Authorization", "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
        }
        return headers;
    }

    private String normalizedBaseUrl() {
        String base = properties.getBaseUrl();
        return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
    }

    private String n(String value) { return value == null ? "" : value; }
}
