package com.example.backend.search.index;

import com.example.backend.domain.message.Message;
import com.example.backend.domain.message.MessageCapsule;
import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.domain.post.Post;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchIndexingService {

    private final SearchIndexGateway gateway;

    public boolean elasticReady() {
        return gateway.isReady();
    }

    public void indexMessage(Message message) {
        if (message == null) return;
        gateway.upsert(new SearchIndexDocument(
                "MESSAGES",
                message.getId(),
                message.getConversationId(),
                safe(message.getContent(), "메시지"),
                safe(message.getContent(), ""),
                "메시지",
                "대화방 메시지",
                "MESSAGE",
                message.getId(),
                message.getConversationId() == null ? "/inbox" : "/inbox/conversations/" + message.getConversationId(),
                message.getCreatedAt(),
                message.getEditedAt() != null ? message.getEditedAt() : message.getCreatedAt(),
                List.of("message", "conversation")
        ));
    }

    public void indexPin(ConversationPin pin) {
        if (pin == null) return;
        gateway.upsert(new SearchIndexDocument(
                "ACTIONS",
                pin.getId(),
                pin.getConversationId(),
                safe(pin.getTitle(), "액션"),
                safe(pin.getTitle(), "") + (pin.getPlaceText() == null || pin.getPlaceText().isBlank() ? "" : " " + pin.getPlaceText()),
                pin.getType() != null ? pin.getType().name() : "ACTION",
                pin.getStatus() != null ? pin.getStatus().name() : "",
                "PIN",
                pin.getId(),
                pin.getConversationId() == null ? "/inbox" : "/inbox/conversations/" + pin.getConversationId(),
                pin.getCreatedAt(),
                pin.getUpdateAt() != null ? pin.getUpdateAt() : pin.getCreatedAt(),
                List.of("pin", "action")
        ));
    }

    public void indexCapsule(MessageCapsule capsule) {
        if (capsule == null) return;
        gateway.upsert(new SearchIndexDocument(
                "CAPSULES",
                capsule.getId(),
                capsule.getConversationId(),
                safe(capsule.getTitle(), "캡슐"),
                safe(capsule.getTitle(), ""),
                "캡슐",
                capsule.getUnlockAt() == null ? "" : capsule.getUnlockAt().toString(),
                "CAPSULE",
                capsule.getId(),
                capsule.getConversationId() == null ? "/inbox" : "/inbox/conversations/" + capsule.getConversationId(),
                capsule.getUnlockAt(),
                capsule.getOpenedAt() != null ? capsule.getOpenedAt() : capsule.getUnlockAt(),
                List.of("capsule", "time-capsule")
        ));
    }

    public void indexPost(Post post) {
        if (post == null) return;
        gateway.upsert(new SearchIndexDocument(
                "POSTS",
                post.getId(),
                null,
                safe(post.getContent(), "게시글"),
                safe(post.getContent(), ""),
                "피드",
                post.getVisibility() != null ? post.getVisibility().name() : "",
                "POST",
                post.getId(),
                "/posts/" + post.getId(),
                post.getCreatedAt(),
                post.getCreatedAt(),
                List.of("post", "feed")
        ));
    }

    public void remove(String type, UUID id) {
        if (id == null || type == null) return;
        gateway.delete(type, id);
    }

    private String safe(String value, String fallback) {
        if (value == null || value.isBlank()) return fallback;
        return value.trim();
    }
}
