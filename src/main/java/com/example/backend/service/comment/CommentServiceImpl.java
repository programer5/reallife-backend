package com.example.backend.service.comment;

import com.example.backend.controller.comment.dto.CommentCreateRequest;
import com.example.backend.controller.comment.dto.CommentListItem;
import com.example.backend.controller.comment.dto.CommentListResponse;
import com.example.backend.controller.comment.dto.CommentResponse;
import com.example.backend.domain.comment.Comment;
import com.example.backend.domain.post.Post;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.comment.CommentRepository;
import com.example.backend.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;

    @Override
    public CommentResponse create(UUID postId, UUID userId, CommentCreateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        Comment saved = commentRepository.save(Comment.create(postId, userId, request.content()));
        return CommentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentListResponse list(UUID postId, String cursor, int size) {
        postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // size 검증이 이미 다른 곳(Controller @Validated 등)에 있으면 생략 가능
        int pageSize = size <= 0 ? 20 : Math.min(size, 50);

        Pageable pageable = PageRequest.of(0, pageSize + 1); // hasNext 판별용 +1

        CursorKey cursorKey = CursorKey.parseOpaque(cursor);

        List<CommentListItem> fetched = (cursorKey == null)
                ? commentRepository.findFirstPage(postId, pageable)
                : commentRepository.findNextPage(postId, cursorKey.createdAt(), cursorKey.commentId(), pageable);

        boolean hasNext = fetched.size() > pageSize;
        List<CommentListItem> items = hasNext ? fetched.subList(0, pageSize) : fetched;

        String nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            CommentListItem last = items.get(items.size() - 1);
            String raw = last.createdAt() + "|" + last.commentId();
            nextCursor = CursorKey.encodeOpaque(raw);
        }

        return new CommentListResponse(items, nextCursor, hasNext);
    }

    /**
     * 내부 커서 포맷: "createdAt|commentId"
     * 외부 노출: Base64URL(무패딩)으로 인코딩된 opaque 문자열
     */
    private record CursorKey(LocalDateTime createdAt, UUID commentId) {

        static CursorKey parseOpaque(String cursor) {
            if (cursor == null || cursor.isBlank()) return null;

            String raw = decodeOpaque(cursor);

            try {
                String[] parts = raw.split("\\|", 2);
                LocalDateTime createdAt = LocalDateTime.parse(parts[0]);
                UUID id = UUID.fromString(parts[1]);
                return new CursorKey(createdAt, id);
            } catch (Exception e) {
                // 포맷 불일치도 요청 오류(400)
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }

        static String encodeOpaque(String raw) {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
        }

        static String decodeOpaque(String cursor) {
            try {
                byte[] decoded = Base64.getUrlDecoder().decode(cursor);
                return new String(decoded, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                // Base64 decode 실패도 요청 오류(400)
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }
    }

    @Override
    public void delete(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (comment.isDeleted()) return;

        if (!comment.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_OWNED);
        }

        comment.delete();
    }
}