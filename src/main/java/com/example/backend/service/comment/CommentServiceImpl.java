package com.example.backend.service.comment;

import com.example.backend.controller.comment.dto.CommentCreateRequest;
import com.example.backend.controller.comment.dto.CommentResponse;
import com.example.backend.domain.comment.Comment;
import com.example.backend.domain.post.Post;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.comment.CommentRepository;
import com.example.backend.repository.post.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        Comment saved = commentRepository.save(Comment.create(post, userId, request.content()));
        return CommentResponse.from(saved);
    }

    @Override
    public void delete(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (comment.isDeleted()) {
            return; // 멱등 삭제(204 유지)
        }

        if (!comment.getAuthorId().equals(userId)) {
            throw new BusinessException(ErrorCode.COMMENT_NOT_OWNED);
        }

        comment.delete();
    }
}