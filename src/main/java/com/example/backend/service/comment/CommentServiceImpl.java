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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ApplicationEventPublisher eventPublisher;

    @Override
    public CommentResponse create(UUID postId, UUID userId, CommentCreateRequest request) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        Comment saved = commentRepository.save(Comment.create(postId, userId, request.content()));

        // ✅ 댓글 수 카운트 증가 (피드 숫자 일치)
        post.increaseCommentCount();

        // ✅ 커밋 이후 알림/SSE 등에 쓰기 위한 이벤트
        eventPublisher.publishEvent(new CommentCreatedEvent(postId, userId, saved.getId()));

        return CommentResponse.from(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CommentListResponse list(UUID postId, String cursor, int size) {
        postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        int pageSize = size <= 0 ? 20 : Math.min(size, 50);
        Pageable pageable = PageRequest.of(0, pageSize + 1);

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

    @Override
    public void delete(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .filter(c -> !c.isDeleted())
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_OWNED);
        }

        comment.delete();

        // ✅ 댓글 수 카운트 감소 (0 밑으로 내려가지 않도록 Post에서 가드)
        postRepository.findById(comment.getPostId())
                .ifPresent(Post::decreaseCommentCount);
    }

    // ====== cursor util ======
    record CursorKey(LocalDateTime createdAt, UUID commentId) {

        static CursorKey parseOpaque(String cursor) {
            if (cursor == null || cursor.isBlank()) return null;

            // cursor가 "있는데" 파싱이 깨지면 400이 정석 (DocsTest도 이것을 기대)
            try {
                String raw = new String(Base64.getUrlDecoder().decode(cursor));
                String[] parts = raw.split("\\|", 2);
                if (parts.length != 2) throw new BusinessException(ErrorCode.INVALID_REQUEST);

                return new CursorKey(LocalDateTime.parse(parts[0]), UUID.fromString(parts[1]));
            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }

        static String encodeOpaque(String raw) {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(raw.getBytes());
        }
    }

    public record CommentCreatedEvent(UUID postId, UUID userId, UUID commentId) {}
}
