package com.example.backend.service.search;

import com.example.backend.controller.search.dto.SearchResponse;
import com.example.backend.repository.search.UnifiedSearchRepository;
import com.example.backend.search.index.SearchIndexingService;
import com.example.backend.repository.search.dto.SearchRow;
import com.example.backend.search.query.ElasticSearchQueryGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UnifiedSearchService {

    private final UnifiedSearchRepository unifiedSearchRepository;
    private final SearchIndexingService searchIndexingService;
    private final ElasticSearchQueryGateway elasticSearchQueryGateway;

    public SearchResponse search(UUID meId, String query, String typeRaw, UUID conversationId, Integer limitRaw) {
        String q = query == null ? "" : query.trim();
        SearchType type = SearchType.from(typeRaw);
        int limit = normalizeLimit(limitRaw);

        if (elasticSearchQueryGateway.supports()) {
            try {
                SearchResponse response = elasticSearchQueryGateway.search(q, type.name(), conversationId, limit);
                log.info("search backend=elasticsearch query='{}' type={} conversationId={} limit={} items={}",
                        q, type.name(), conversationId, limit, response.items().size());
                return response;
            } catch (Exception e) {
                log.warn("search backend=elasticsearch failed. fallback=db. query='{}' type={} conversationId={} limit={} reason={}",
                        q, type.name(), conversationId, limit, e.getMessage());
            }
        } else {
            log.info("search backend=elasticsearch unavailable. fallback=db. query='{}' type={} conversationId={} limit={}",
                    q, type.name(), conversationId, limit);
        }

        List<SearchRow> messageRows = List.of();
        List<SearchRow> actionRows = List.of();
        List<SearchRow> capsuleRows = List.of();
        List<SearchRow> postRows = List.of();

        if (type.includesMessages()) {
            messageRows = unifiedSearchRepository.searchMessages(meId, q, conversationId, limit);
        }
        if (type.includesActions()) {
            actionRows = unifiedSearchRepository.searchPins(meId, q, conversationId, limit);
        }
        if (type.includesCapsules()) {
            capsuleRows = unifiedSearchRepository.searchCapsules(meId, q, conversationId, limit);
        }
        if (type.includesPosts() && conversationId == null) {
            postRows = unifiedSearchRepository.searchPosts(meId, q, limit);
        }

        List<SearchResponse.Item> items = new ArrayList<>();
        items.addAll(mapRows(messageRows));
        items.addAll(mapRows(actionRows));
        items.addAll(mapRows(capsuleRows));
        items.addAll(mapRows(postRows));
        items.sort(Comparator
                .comparing(SearchResponse.Item::relevance, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(SearchResponse.Item::createdAt, Comparator.nullsLast(Comparator.reverseOrder())));
        if (items.size() > limit * 4) {
            items = items.subList(0, limit * 4);
        }

        List<SearchResponse.Section> sections = List.of(
                new SearchResponse.Section("MESSAGES", "메시지", messageRows.size()),
                new SearchResponse.Section("ACTIONS", "액션", actionRows.size()),
                new SearchResponse.Section("CAPSULES", "캡슐", capsuleRows.size()),
                new SearchResponse.Section("POSTS", "피드", postRows.size())
        );

        log.info("search backend=db-fallback query='{}' type={} conversationId={} limit={} messageCount={} actionCount={} capsuleCount={} postCount={} items={}",
                q, type.name(), conversationId, limit,
                messageRows.size(), actionRows.size(), capsuleRows.size(), postRows.size(), items.size());

        return new SearchResponse(
                q,
                type.name(),
                conversationId,
                sections,
                items,
                new SearchResponse.Meta(
                        searchIndexingService.elasticReady(),
                        "db-fallback",
                        limit,
                        List.of("ALL", "MESSAGES", "ACTIONS", "CAPSULES", "POSTS")
                )
        );
    }

    private List<SearchResponse.Item> mapRows(List<SearchRow> rows) {
        return rows.stream().map(this::toItem).toList();
    }

    private SearchResponse.Item toItem(SearchRow row) {
        String deepLink = switch (row.type()) {
            case "MESSAGES" -> row.conversationId() == null ? "/inbox" : "/inbox/conversations/" + row.conversationId();
            case "ACTIONS" -> row.conversationId() == null ? "/inbox" : "/inbox/conversations/" + row.conversationId();
            case "CAPSULES" -> row.conversationId() == null ? "/inbox" : "/inbox/conversations/" + row.conversationId();
            case "POSTS" -> "/posts/" + row.id();
            default -> "/home";
        };
        String anchorType = switch (row.type()) {
            case "MESSAGES" -> "MESSAGE";
            case "ACTIONS" -> "PIN";
            case "CAPSULES" -> "CAPSULE";
            case "POSTS" -> "POST";
            default -> "NONE";
        };

        return new SearchResponse.Item(
                row.type(),
                row.id(),
                shorten(row.title(), 84),
                shorten(row.snippet(), 180),
                shorten(row.highlight(), 120),
                row.createdAt(),
                row.conversationId(),
                deepLink,
                row.badge(),
                row.secondary(),
                anchorType,
                row.id(),
                row.relevanceScore()
        );
    }

    private String shorten(String value, int max) {
        if (value == null) return "";
        String v = value.trim().replaceAll("\\s+", " ");
        if (v.length() <= max) return v;
        return v.substring(0, max - 1) + "…";
    }

    private int normalizeLimit(Integer raw) {
        int value = raw == null ? 6 : raw;
        if (value < 1) return 1;
        return Math.min(value, 20);
    }

    enum SearchType {
        ALL, MESSAGES, ACTIONS, CAPSULES, POSTS;

        static SearchType from(String raw) {
            if (raw == null || raw.isBlank()) return ALL;
            try {
                return SearchType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException e) {
                return ALL;
            }
        }

        boolean includesMessages() { return this == ALL || this == MESSAGES; }
        boolean includesActions() { return this == ALL || this == ACTIONS; }
        boolean includesCapsules() { return this == ALL || this == CAPSULES; }
        boolean includesPosts() { return this == ALL || this == POSTS; }
    }
}