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
        followService.follow(authentication.getName(), targetUserId);
        return ResponseEntity.noContent().build(); // 204
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> unfollow(@PathVariable UUID targetUserId, Authentication authentication) {
        followService.unfollow(authentication.getName(), targetUserId);
        return ResponseEntity.noContent().build(); // 204
    }
}
