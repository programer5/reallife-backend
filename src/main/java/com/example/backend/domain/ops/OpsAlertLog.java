package com.example.backend.domain.ops;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "ops_alert_log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpsAlertLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "channel", nullable = false, length = 30)
    private String channel;

    @Column(name = "alert_key", length = 190)
    private String alertKey;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Lob
    @Column(name = "body")
    private String body;

    @Column(name = "level", nullable = false, length = 30)
    private String level;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "requested_by", length = 120)
    private String requestedBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    private OpsAlertLog(
            String channel,
            String alertKey,
            String title,
            String body,
            String level,
            String status,
            String requestedBy,
            LocalDateTime createdAt
    ) {
        this.channel = channel;
        this.alertKey = alertKey;
        this.title = title;
        this.body = body;
        this.level = level;
        this.status = status;
        this.requestedBy = requestedBy;
        this.createdAt = createdAt;
    }

    public static OpsAlertLog of(
            String channel,
            String alertKey,
            String title,
            String body,
            String level,
            String status,
            String requestedBy
    ) {
        return new OpsAlertLog(
                channel,
                alertKey,
                title,
                body,
                level,
                status,
                requestedBy,
                LocalDateTime.now()
        );
    }
}