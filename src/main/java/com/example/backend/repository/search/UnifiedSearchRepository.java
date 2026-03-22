package com.example.backend.repository.search;

import com.example.backend.repository.search.dto.SearchRow;

import java.util.List;
import java.util.UUID;

public interface UnifiedSearchRepository {
    List<SearchRow> searchMessages(UUID meId, String query, UUID conversationId, int limit);
    List<SearchRow> searchPins(UUID meId, String query, UUID conversationId, int limit);
    List<SearchRow> searchCapsules(UUID meId, String query, UUID conversationId, int limit);
    List<SearchRow> searchPosts(UUID meId, String query, int limit);
}
