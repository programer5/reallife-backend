package com.example.backend.domain.pin;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(
        name = "conversation_pin_dismissals",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_pin_dismiss", columnNames = {"pin_id", "user_id"})
        }
)
public class ConversationPinDismissal {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "pin_id", nullable = false)
    private UUID pinId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static ConversationPinDismissal of(UUID pinId, UUID userId) {
        ConversationPinDismissal d = new ConversationPinDismissal();
        d.pinId = pinId;
        d.userId = userId;
        d.createdAt = LocalDateTime.now();
        return d;
    }
}