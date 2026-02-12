package com.example.backend.domain.message;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(name = "direct_conversation_keys")
public class DirectConversationKey extends BaseEntity {

    @Id
    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Column(name = "user1_id", nullable = false, updatable = false)
    private UUID user1Id;

    @Column(name = "user2_id", nullable = false, updatable = false)
    private UUID user2Id;

    private DirectConversationKey(UUID conversationId, UUID user1Id, UUID user2Id) {
        this.conversationId = conversationId;
        this.user1Id = user1Id;
        this.user2Id = user2Id;
    }

    public static DirectConversationKey of(UUID conversationId, UUID userA, UUID userB) {
        UUID u1 = (userA.compareTo(userB) <= 0) ? userA : userB;
        UUID u2 = (userA.compareTo(userB) <= 0) ? userB : userA;
        return new DirectConversationKey(conversationId, u1, u2);
    }
}