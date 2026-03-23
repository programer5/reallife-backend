package com.example.backend.controller.search;

import com.example.backend.config.search.SearchElasticProperties;
import com.example.backend.controller.search.dto.SearchReindexResponse;
import com.example.backend.controller.search.dto.SearchResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.service.search.SearchReindexService;
import com.example.backend.service.search.UnifiedSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final UnifiedSearchService unifiedSearchService;
    private final SearchReindexService searchReindexService;
    private final SearchElasticProperties searchElasticProperties;

    @GetMapping
    public SearchResponse search(
            @RequestParam String q,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID conversationId,
            @RequestParam(required = false) Integer limit,
            Authentication authentication
    ) {
        if (q == null || q.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
        UUID meId = UUID.fromString(authentication.getName());
        return unifiedSearchService.search(meId, q, type, conversationId, limit);
    }

    @PostMapping("/admin/reindex")
    public SearchReindexResponse reindex(
            @RequestHeader("X-Search-Reindex-Token") String reindexToken,
            @RequestParam(required = false) Integer batchSize,
            Authentication authentication
    ) {
        validateReindexToken(reindexToken);
        UUID meId = UUID.fromString(authentication.getName());
        return searchReindexService.reindexAll(meId, batchSize);
    }

    private void validateReindexToken(String input) {
        String expected = searchElasticProperties.getReindexAdminToken();
        if (expected == null || expected.isBlank() || !expected.equals(input)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}