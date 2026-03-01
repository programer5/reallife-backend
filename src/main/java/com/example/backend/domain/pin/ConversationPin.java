package com.example.backend.domain.pin;

import com.example.backend.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

import static lombok.AccessLevel.PROTECTED;

@Entity
@Getter
@NoArgsConstructor(access = PROTECTED)
@Table(
        name = "conversation_pins",
        indexes = {
                @Index(name = "idx_pins_conversation_active_created", columnList = "conversation_id, status, created_at"),
                @Index(name = "idx_pins_remind", columnList = "status, remind_at"),
                @Index(name = "idx_pins_conversation_source_message", columnList = "conversation_id, source_message_id")
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

    // ✅ 같은 메시지에서 생성된 핀 중복 방지용
    @Column(name = "source_message_id")
    private UUID sourceMessageId;

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

    @Column(name = "reminded_at")
    private LocalDateTime remindedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PinStatus status;

    public static ConversationPin createSchedule(
            UUID conversationId,
            UUID createdBy,
            UUID sourceMessageId,
            String title,
            String placeText,
            LocalDateTime startAt,
            int remindMinutes // ✅ NEW
    ) {
        ConversationPin pin = new ConversationPin();
        pin.conversationId = conversationId;
        pin.createdBy = createdBy;
        pin.sourceMessageId = sourceMessageId;
        pin.type = PinType.SCHEDULE;
        pin.title = (title == null || title.isBlank()) ? "약속" : title.trim();
        pin.placeText = (placeText == null || placeText.isBlank()) ? null : placeText.trim();
        pin.startAt = startAt;

        int minutes = (remindMinutes == 5 || remindMinutes == 10 || remindMinutes == 30 || remindMinutes == 60)
                ? remindMinutes
                : 60;

        pin.remindAt = (startAt == null) ? null : startAt.minusMinutes(minutes); // ✅ 변경
        pin.status = PinStatus.ACTIVE;
        return pin;
    }

    public void markDone() {
        this.status = PinStatus.DONE;
    }

    public void cancel() {
        this.status = PinStatus.CANCELED;
    }

    public void updatePlaceText(String placeText) {
        this.placeText = (placeText == null || placeText.isBlank()) ? null : placeText.trim();
    }

    public boolean updateSchedule(String title, String placeText, LocalDateTime startAt, Integer remindMinutes) {
        String newTitle = (title == null || title.isBlank()) ? this.title : title.trim();
        String newPlace = (placeText == null || placeText.isBlank()) ? null : placeText.trim();

        // ✅ 1) startAt이 null이면 "시간 변경 없음" → 기존 startAt 유지
        LocalDateTime effectiveStartAt = (startAt == null) ? this.startAt : startAt;

        // ✅ 2) remindMinutes가 없으면 "기존 diff" 유지 (startAt-remindAt)
        int effectiveMinutes = 60; // fallback
        if (remindMinutes != null) {
            int v = remindMinutes;
            if (v == 5 || v == 10 || v == 30 || v == 60) effectiveMinutes = v;
        } else {
            if (this.startAt != null && this.remindAt != null) {
                long diff = java.time.Duration.between(this.remindAt, this.startAt).toMinutes();
                int d = (int) diff;
                if (d == 5 || d == 10 || d == 30 || d == 60) effectiveMinutes = d;
            }
        }

        LocalDateTime newRemindAt =
                (effectiveStartAt == null) ? null : effectiveStartAt.minusMinutes(effectiveMinutes);

        boolean scheduleChanged =
                !Objects.equals(this.startAt, effectiveStartAt) ||
                        !Objects.equals(this.remindAt, newRemindAt) ||
                        !Objects.equals(this.title, newTitle) ||
                        !Objects.equals(this.placeText, newPlace);

        this.title = newTitle;
        this.placeText = newPlace;
        this.startAt = effectiveStartAt;
        this.remindAt = newRemindAt;

        // ✅ 일정/리마인드가 바뀌면 재알림 허용
        if (scheduleChanged) {
            this.remindedAt = null;
        }

        return scheduleChanged;
    }
}