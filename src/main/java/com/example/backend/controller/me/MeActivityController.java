
package com.example.backend.controller.me;

import com.example.backend.controller.me.dto.MyActionListResponse;
import com.example.backend.controller.me.dto.MyCapsuleListResponse;
import com.example.backend.controller.me.dto.MyShareListResponse;
import com.example.backend.domain.message.MessageCapsule;
import com.example.backend.domain.pin.ConversationPin;
import com.example.backend.domain.post.Post;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me/activity")
public class MeActivityController {

    private final EntityManager em;

    @GetMapping("/actions")
    public MyActionListResponse actions(Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        List<ConversationPin> pins = em.createQuery(
                        "select p from ConversationPin p where p.deleted = false and p.createdBy = :meId order by p.createdAt desc",
                        ConversationPin.class
                )
                .setParameter("meId", meId)
                .setMaxResults(30)
                .getResultList();

        return new MyActionListResponse(
                pins.stream()
                        .map(p -> new MyActionListResponse.Item(
                                p.getId(),
                                p.getType().name(),
                                p.getTitle(),
                                p.getPlaceText(),
                                p.getStartAt(),
                                p.getStatus().name()
                        ))
                        .toList()
        );
    }

    @GetMapping("/capsules")
    public MyCapsuleListResponse capsules(Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        List<MessageCapsule> items = em.createQuery(
                        "select c from MessageCapsule c where c.creatorId = :meId order by c.unlockAt desc",
                        MessageCapsule.class
                )
                .setParameter("meId", meId)
                .setMaxResults(30)
                .getResultList();

        return new MyCapsuleListResponse(
                items.stream()
                        .map(c -> new MyCapsuleListResponse.Item(
                                c.getId(),
                                c.getTitle(),
                                c.getUnlockAt(),
                                c.getOpenedAt() != null
                        ))
                        .toList()
        );
    }

    @GetMapping("/shares")
    public MyShareListResponse shares(Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());
        List<Post> posts = em.createQuery(
                        "select distinct p from Post p left join fetch p.images img where p.deleted = false and p.authorId = :meId order by p.createdAt desc",
                        Post.class
                )
                .setParameter("meId", meId)
                .setMaxResults(30)
                .getResultList();

        return new MyShareListResponse(
                posts.stream()
                        .map(p -> new MyShareListResponse.Item(
                                p.getId(),
                                p.getContent(),
                                p.getVisibility().name(),
                                p.getCreatedAt()
                        ))
                        .toList()
        );
    }
}
