package com.example.backend.domain.pin;

import com.example.backend.domain.BaseEntity;
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
        name = "conversation_pins",
        indexes = {
                @Index(name = "idx_pins_conversation_active_created", columnList = "conversation_id, status, created_at"),
                @Index(name = "idx_pins_remind", columnList = "status, remind_at")
        }
)
public class ConversationPin extends BaseEntity {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private PinType type;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "place_text", length = 255)
    private String placeText;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "remind_at")
    private LocalDateTime remindAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PinStatus status;

    public static ConversationPin createSchedule(
            UUID conversationId,
            UUID createdBy,
            String title,
            String placeText,
            LocalDateTime startAt
    ) {
        ConversationPin pin = new ConversationPin();
        pin.conversationId = conversationId;
        pin.createdBy = createdBy;
        pin.type = PinType.SCHEDULE;
        pin.title = (title == null || title.isBlank()) ? "약속" : title.trim();
        pin.placeText = (placeText == null || placeText.isBlank()) ? null : placeText.trim();
        pin.startAt = startAt;
        pin.remindAt = (startAt == null) ? null : startAt.minusHours(1);
        pin.status = PinStatus.ACTIVE;
        return pin;
    }

    public void markDone() {
        this.status = PinStatus.DONE;
    }

    public void cancel() {
        this.status = PinStatus.CANCELED;
    }
}