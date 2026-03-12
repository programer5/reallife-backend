
package com.example.backend.service.message;

import com.example.backend.domain.message.MessageCapsule;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MessageCapsuleService {

    private final EntityManager em;

    @Transactional
    public UUID create(UUID messageId, UUID conversationId, UUID creatorId, String title, LocalDateTime unlockAt){
        MessageCapsule c = MessageCapsule.create(messageId,conversationId,creatorId,title,unlockAt);
        em.persist(c);
        return c.getId();
    }

    @Transactional
    public void open(UUID capsuleId){
        MessageCapsule c = em.find(MessageCapsule.class,capsuleId);
        if(c!=null){
            c.open();
        }
    }
}
