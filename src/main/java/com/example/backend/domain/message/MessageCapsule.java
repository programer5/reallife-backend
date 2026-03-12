package com.example.backend.domain.message;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name="message_capsules")
public class MessageCapsule {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name="message_id")
    private UUID messageId;

    @Column(name="conversation_id")
    private UUID conversationId;

    @Column(name="creator_id")
    private UUID creatorId;

    @Column(name="title")
    private String title;

    @Column(name="unlock_at")
    private LocalDateTime unlockAt;

    @Column(name="opened_at")
    private LocalDateTime openedAt;

    public static MessageCapsule create(UUID messageId, UUID conversationId, UUID creatorId, String title, LocalDateTime unlockAt){
        MessageCapsule c=new MessageCapsule();
        c.messageId=messageId;
        c.conversationId=conversationId;
        c.creatorId=creatorId;
        c.title=title;
        c.unlockAt=unlockAt;
        return c;
    }

    public void open(){
        this.openedAt=LocalDateTime.now();
    }
}
