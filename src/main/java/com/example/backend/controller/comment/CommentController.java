package com.example.backend.controller.comment;

import com.example.backend.controller.comment.dto.CommentCreateRequest;
import com.example.backend.controller.comment.dto.CommentListResponse;
import com.example.backend.controller.comment.dto.CommentResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.service.comment.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentListResponse> list(
            @PathVariable UUID postId,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        // size 상한(DoS 방지)
        int safeSize = Math.min(Math.max(size, 1), 50);
        return ResponseEntity.ok(commentService.list(postId, cursor, safeSize));
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> create(
            @PathVariable UUID postId,
            @Valid @RequestBody CommentCreateRequest request,
            Authentication authentication
    ) {
        UUID userId = requireUserId(authentication);
        CommentResponse created = commentService.create(postId, userId, request);

        return ResponseEntity
                .created(URI.create("/api/comments/" + created.commentId()))
                .body(created);
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID commentId,
            Authentication authentication
    ) {
        UUID userId = requireUserId(authentication);
        commentService.delete(commentId, userId);
        return ResponseEntity.noContent().build();
    }

    private UUID requireUserId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        try {
            return UUID.fromString(authentication.getName()); // JWT sub 가 UUID라는 전제
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid subject");
        }
    }
}