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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
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

    /**
     * 게시글 좋아요 알림 (기존 유지)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPostLiked(PostLikeService.PostLikedEvent e) {
        var post = postRepository.findById(e.postId()).orElse(null);
        if (post == null) return;

        UUID targetUserId = post.getAuthorId();
        if (targetUserId == null || targetUserId.equals(e.userId())) return; // 본인 글 좋아요는 알림 X

        String actorName = userRepository.findById(e.userId()).map(u -> u.getName()).orElse("누군가");
        String body = actorName + "님이 회원님의 게시글을 좋아합니다.";

        notificationCommandService.createIfNotExists(
                targetUserId,
                NotificationType.POST_LIKE,
                e.postId(),
                body
        );
    }

    /**
     * 댓글 알림 (✅ 네 프로젝트 실제 이벤트: CommentCreatedEvent)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onCommentCreated(CommentServiceImpl.CommentCreatedEvent e) {
        var post = postRepository.findById(e.postId()).orElse(null);
        if (post == null) return;

        UUID targetUserId = post.getAuthorId();
        if (targetUserId == null || targetUserId.equals(e.userId())) return;

        String actorName = userRepository.findById(e.userId()).map(u -> u.getName()).orElse("누군가");
        String body = actorName + "님이 회원님의 게시글에 댓글을 남겼습니다.";

        notificationCommandService.createIfNotExists(
                targetUserId,
                NotificationType.POST_COMMENT,
                e.commentId(),
                body
        );
    }

    /**
     * ✅ FOLLOW는 "다시 팔로우해도 다시 뜨게" + 안정적으로 실행되게 변경 (추천 방식)
     * - createOrRevive: 기존 알림이 있어도 revive + NotificationCreatedEvent 다시 발행 → SSE 다시 날아감
     * - REQUIRES_NEW: follow 트랜잭션과 분리되어 알림 생성/이벤트 발행이 더 안정적
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @EventListener
    public void onFollowed(FollowService.UserFollowedEvent e) {
        if (e.targetUserId().equals(e.followerId())) return;

        String actorName = userRepository.findById(e.followerId()).map(u -> u.getName()).orElse("누군가");
        String body = actorName + "님이 회원님을 팔로우하기 시작했습니다.";

        // refId = followerId (같은 사람이 다시 팔로우하면 revive로 다시 알림+SSE 발생)
        notificationCommandService.createOrRevive(
                e.targetUserId(),
                NotificationType.FOLLOW,
                e.followerId(),
                body
        );
    }
}