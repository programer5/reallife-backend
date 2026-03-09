package com.example.backend.domain.error;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "error_log")
public class ErrorLog {

    @Id
    @GeneratedValue
    private UUID id;

    private String type;

    @Column(length = 2000)
    private String message;

    private String path;

    private LocalDateTime createdAt;

    public static ErrorLog of(String type, String message, String path) {
        ErrorLog log = new ErrorLog();
        log.type = type;
        log.message = message;
        log.path = path;
        log.createdAt = LocalDateTime.now();
        return log;
    }
}