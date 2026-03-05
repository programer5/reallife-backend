package com.example.backend.service.comment;

import com.example.backend.controller.comment.dto.CommentCreateRequest;
import com.example.backend.controller.comment.dto.CommentListItem;
import com.example.backend.controller.comment.dto.CommentListResponse;
import com.example.backend.controller.comment.dto.CommentResponse;
import com.example.backend.domain.comment.Comment;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.comment.CommentRepository;
import com.example.backend.repository.post.PostRepository;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    @Override
    public CommentResponse create(UUID postId, UUID userId, CommentCreateRequest request) {
        // ✅ post 존재
        postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        // ✅ user 존재 (선택이지만, 안정성 위해 유지)
        userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        UUID parentId = request.parentCommentId();
        if (parentId != null) {
            // ✅ parent가 같은 post에 속하고 삭제되지 않았는지(1-depth만 허용해도 됨)
            boolean ok = commentRepository.existsByIdAndPostIdAndDeletedFalse(parentId, postId);
            if (!ok) {
                throw new BusinessException(ErrorCode.COMMENT_NOT_FOUND, "Parent comment not found");
            }
        }

        Comment saved = commentRepository.save(
                Comment.create(postId, userId, parentId, request.content())
        );

        eventPublisher.publishEvent(new CommentCreatedEvent(postId, userId, saved.getId(), parentId));

        return CommentResponse.from(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public CommentListResponse list(UUID postId, String cursor, int size, String sort) {
        String s = (sort == null || sort.isBlank()) ? "LATEST" : sort.trim().toUpperCase();
        Pageable pageable = PageRequest.of(0, size);

        List<CommentListItem> items;
        if ("POPULAR".equals(s)) {
            items = listPopular(postId, cursor, pageable);
        } else {
            items = listLatest(postId, cursor, pageable);
            s = "LATEST";
        }

        boolean hasNext = items.size() == size;
        String nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            CommentListItem last = items.get(items.size() - 1);
            nextCursor = encodeCursor(last, s);
        }

        return new CommentListResponse(items, nextCursor, hasNext);
    }

    private List<CommentListItem> listLatest(UUID postId, String cursor, Pageable pageable) {
        if (cursor == null || cursor.isBlank()) {
            return commentRepository.findFirstPageLatest(postId, pageable);
        }
        CursorLatest c = parseLatest(cursor);
        return commentRepository.findNextPageLatest(postId, c.createdAt(), c.id(), pageable);
    }

    private List<CommentListItem> listPopular(UUID postId, String cursor, Pageable pageable) {
        if (cursor == null || cursor.isBlank()) {
            return commentRepository.findFirstPagePopular(postId, pageable);
        }
        CursorPopular c = parsePopular(cursor);
        return commentRepository.findNextPagePopular(postId, c.likeCount(), c.createdAt(), c.id(), pageable);
    }

    @Transactional
    @Override
    public void delete(UUID commentId, UUID userId) {
        Comment c = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!c.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_OWNED);
        }

        c.delete();
    }

    // ===== cursor helpers =====
    private static String encodeCursor(CommentListItem item, String sort) {
        if ("POPULAR".equals(sort)) {
            return "lc=" + item.likeCount() + "|t=" + item.createdAt() + "|id=" + item.commentId();
        }
        return "t=" + item.createdAt() + "|id=" + item.commentId();
    }

    private static CursorLatest parseLatest(String cursor) {
        try {
            String[] parts = cursor.split("\\|");
            LocalDateTime t = null;
            UUID id = null;
            for (String p : parts) {
                if (p.startsWith("t=")) t = LocalDateTime.parse(p.substring(2));
                if (p.startsWith("id=")) id = UUID.fromString(p.substring(3));
            }
            if (t == null || id == null) throw new IllegalArgumentException("bad cursor");
            return new CursorLatest(t, id);
        } catch (Exception e) {
            // fallback: legacy format "createdAt|uuid"
            try {
                String[] p = cursor.split("\\|");
                return new CursorLatest(LocalDateTime.parse(p[0]), UUID.fromString(p[1]));
            } catch (Exception ignored) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "부모 댓글이 올바르지 않습니다.");
            }
        }
    }

    private static CursorPopular parsePopular(String cursor) {
        try {
            String[] parts = cursor.split("\\|");
            Long lc = null;
            LocalDateTime t = null;
            UUID id = null;
            for (String p : parts) {
                if (p.startsWith("lc=")) lc = Long.parseLong(p.substring(3));
                if (p.startsWith("t=")) t = LocalDateTime.parse(p.substring(2));
                if (p.startsWith("id=")) id = UUID.fromString(p.substring(3));
            }
            if (lc == null || t == null || id == null) throw new IllegalArgumentException("bad cursor");
            return new CursorPopular(lc, t, id);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "부모 댓글이 올바르지 않습니다.");
        }
    }

    private record CursorLatest(LocalDateTime createdAt, UUID id) {}
    private record CursorPopular(long likeCount, LocalDateTime createdAt, UUID id) {}

    public record CommentCreatedEvent(UUID postId, UUID userId, UUID commentId, UUID parentCommentId) {}
}
