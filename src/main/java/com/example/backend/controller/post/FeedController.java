package com.example.backend.controller.post;

import com.example.backend.controller.post.dto.FeedResponse;
import com.example.backend.service.post.FeedService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/feed")
public class FeedController {

    private final FeedService feedService;

    public FeedController(FeedService feedService) {
        this.feedService = feedService;
    }

    @GetMapping
    public FeedResponse feed(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        // ✅ 현재 Security principal이 UUID 문자열이어야 함
        UUID meId = UUID.fromString(authentication.getName());
        return feedService.getFollowingFeed(meId, cursor, size);
    }
}