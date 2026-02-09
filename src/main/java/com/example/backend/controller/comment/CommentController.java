package com.example.backend.controller.comment;

import com.example.backend.controller.comment.dto.CommentCreateRequest;
import com.example.backend.controller.comment.dto.CommentResponse;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.comment.CommentRepository;
import com.example.backend.service.comment.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
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
    private final CommentRepository commentRepository; // 목록만 빠르게 위해(원하면 service로 이동)

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<?> list(
            @PathVariable UUID postId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        // ✅ 기본 방어 (음수/과도한 size 방지)
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        var result = commentRepository.findActiveByPostId(postId, PageRequest.of(safePage, safeSize))
                .map(CommentResponse::from);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<CommentResponse> create(
            @PathVariable UUID postId,
            @Valid @RequestBody CommentCreateRequest request,
            Authentication authentication
    ) {
        UUID userId = requireUserId(authentication);

        // ✅ record는 getter 없음 → request.content()
        CommentResponse created = commentService.create(postId, userId, request);

        return ResponseEntity
                .created(URI.create("/api/comments/" + created.id()))
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
            // ✅ JWT sub 가 UUID 문자열이라는 전제
            return UUID.fromString(authentication.getName());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "Invalid subject");
        }
    }
}