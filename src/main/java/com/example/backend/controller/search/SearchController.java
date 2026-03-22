package com.example.backend.controller.search;

import com.example.backend.controller.search.dto.SearchResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.service.search.UnifiedSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/search")
public class SearchController {

    private final UnifiedSearchService unifiedSearchService;

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
}
