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

import java.time.LocalDateTime;
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

        Pageable pageable = PageRequest.of(0, size + 1); // hasNext 판단용

        CursorKey cursorKey = CursorKey.parse(cursor);
        List<CommentListItem> fetched = (cursorKey == null)
                ? commentRepository.findFirstPage(postId, pageable)
                : commentRepository.findNextPage(postId, cursorKey.createdAt(), cursorKey.commentId(), pageable);

        boolean hasNext = fetched.size() > size;
        List<CommentListItem> items = hasNext ? fetched.subList(0, size) : fetched;

        String nextCursor = null;
        if (hasNext && !items.isEmpty()) {
            CommentListItem last = items.get(items.size() - 1);
            nextCursor = last.createdAt() + "|" + last.commentId();
        }

        return new CommentListResponse(items, nextCursor, hasNext);
    }

    private record CursorKey(LocalDateTime createdAt, UUID commentId) {
        static CursorKey parse(String cursor) {
            if (cursor == null || cursor.isBlank()) return null;
            try {
                String[] parts = cursor.split("\\|", 2);
                return new CursorKey(LocalDateTime.parse(parts[0]), UUID.fromString(parts[1]));
            } catch (Exception e) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST, "Invalid cursor");
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