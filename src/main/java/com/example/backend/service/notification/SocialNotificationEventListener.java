package com.example.backend.service.notification;

import com.example.backend.domain.notification.NotificationType;
import com.example.backend.repository.post.PostRepository;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.service.comment.CommentServiceImpl;
import com.example.backend.service.follow.FollowService;
import com.example.backend.service.like.PostLikeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SocialNotificationEventListener {

    private final NotificationCommandService notificationCommandService;
    private final PostRepository postRepository;
    private final UserRepository userRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostLiked(PostLikeService.PostLikedEvent e) {
        var post = postRepository.findById(e.postId()).orElse(null);
        if (post == null) return;

        UUID targetUserId = post.getAuthorId();
        if (targetUserId == null || targetUserId.equals(e.userId())) return; // 본인 글 좋아요는 알림 X

        String actorName = userRepository.findById(e.userId()).map(u -> u.getName()).orElse("누군가");
        String body = actorName + "님이 회원님의 게시글을 좋아합니다.";

        // refId는 postId로 (MVP). 더 정교하게 하려면 likeId를 refId로 쓰면 됨.
        notificationCommandService.createIfNotExists(
                targetUserId,
                NotificationType.POST_LIKE,
                e.postId(),
                body
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentServiceImpl.CommentCreatedEvent e) {
        var post = postRepository.findById(e.postId()).orElse(null);
        if (post == null) return;

        UUID targetUserId = post.getAuthorId();
        if (targetUserId == null || targetUserId.equals(e.userId())) return;

        String actorName = userRepository.findById(e.userId()).map(u -> u.getName()).orElse("누군가");
        String body = actorName + "님이 회원님의 게시글에 댓글을 남겼습니다.";

        // refId는 commentId로 (같은 글에 여러 댓글도 구분)
        notificationCommandService.createIfNotExists(
                targetUserId,
                NotificationType.POST_COMMENT,
                e.commentId(),
                body
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFollowed(FollowService.UserFollowedEvent e) {
        if (e.targetUserId().equals(e.followerId())) return;

        String actorName = userRepository.findById(e.followerId()).map(u -> u.getName()).orElse("누군가");
        String body = actorName + "님이 회원님을 팔로우하기 시작했습니다.";

        // refId = followerId (한 사람당 한 번)
        notificationCommandService.createIfNotExists(
                e.targetUserId(),
                NotificationType.FOLLOW,
                e.followerId(),
                body
        );
    }
}
