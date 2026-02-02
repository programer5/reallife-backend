package com.example.backend.controller.like;

import com.example.backend.service.like.PostLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/posts/{postId}/likes")
public class PostLikeController {

    private final PostLikeService postLikeService;

    @PostMapping
    public ResponseEntity<Void> like(@PathVariable UUID postId, Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        postLikeService.like(meId, postId);
        return ResponseEntity.noContent().build(); // 204
    }

    @DeleteMapping
    public ResponseEntity<Void> unlike(@PathVariable UUID postId, Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        postLikeService.unlike(meId, postId);
        return ResponseEntity.noContent().build(); // 204
    }
}