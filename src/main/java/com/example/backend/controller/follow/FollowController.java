package com.example.backend.controller.follow;

import com.example.backend.service.follow.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/follows")
public class FollowController {

    private final FollowService followService;

    @PostMapping("/{targetUserId}")
    public ResponseEntity<Void> follow(@PathVariable UUID targetUserId, Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        followService.follow(meId, targetUserId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> unfollow(@PathVariable UUID targetUserId, Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        followService.unfollow(meId, targetUserId);
        return ResponseEntity.noContent().build();
    }
}