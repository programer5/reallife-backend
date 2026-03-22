package com.example.backend.search.index;

import java.util.UUID;

public interface SearchIndexGateway {
    boolean isReady();
    void upsert(SearchIndexDocument document);
    void delete(String type, UUID id);
}
