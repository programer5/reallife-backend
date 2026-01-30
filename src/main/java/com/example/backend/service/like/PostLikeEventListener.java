package com.example.backend.service.like;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PostLikeEventListener {

    @EventListener
    public void on(PostLikeService.PostLikedEvent e) {
        log.info("PostLikedEvent postId={}, userId={}", e.postId(), e.userId());
    }

    @EventListener
    public void on(PostLikeService.PostUnlikedEvent e) {
        log.info("PostUnlikedEvent postId={}, userId={}", e.postId(), e.userId());
    }
}
