package com.example.backend.service.like;

import com.example.backend.domain.comment.Comment;
import com.example.backend.domain.like.CommentLike;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.comment.CommentRepository;
import com.example.backend.repository.like.CommentLikeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CommentLikeServiceImpl implements CommentLikeService {

    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    @Transactional
    @Override
    public void like(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            return; // idempotent
        }

        commentLikeRepository.save(CommentLike.create(commentId, userId));
        comment.increaseLikeCount();
    }

    @Transactional
    @Override
    public void unlike(UUID commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        var opt = commentLikeRepository.findByCommentIdAndUserId(commentId, userId);
        if (opt.isEmpty()) return;

        commentLikeRepository.delete(opt.get());
        comment.decreaseLikeCount();
    }
}
