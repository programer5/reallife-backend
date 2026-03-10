package com.example.backend.domain.error;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "ops_alert_log")
public class OpsAlertLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false, length = 30)
    private String channel;

    @Column(length = 190)
    private String alertKey;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String body;

    @Column(nullable = false, length = 30)
    private String level;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(length = 120)
    private String requestedBy;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    public static OpsAlertLog of(
            String channel,
            String alertKey,
            String title,
            String body,
            String level,
            String status,
            String requestedBy
    ) {
        OpsAlertLog log = new OpsAlertLog();
        log.channel = channel;
        log.alertKey = alertKey;
        log.title = title;
        log.body = body;
        log.level = level;
        log.status = status;
        log.requestedBy = requestedBy;
        log.createdAt = LocalDateTime.now();
        return log;
    }
}