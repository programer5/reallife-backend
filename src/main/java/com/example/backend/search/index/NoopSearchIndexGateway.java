package com.example.backend.search.index;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.search.elastic", name = "enabled", havingValue = "false", matchIfMissing = true)
public class NoopSearchIndexGateway implements SearchIndexGateway {
    @Override
    public boolean isReady() { return false; }

    @Override
    public void upsert(SearchIndexDocument document) { }

    @Override
    public void delete(String type, UUID id) { }
}
