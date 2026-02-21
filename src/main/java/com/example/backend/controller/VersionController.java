package com.example.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class VersionController {

    @Value("${app.version:dev}")
    private String appVersion;

    @Value("${app.buildTime:unknown}")
    private String buildTime;

    @GetMapping("/api/version")
    public Map<String, Object> version() {
        return Map.of(
                "version", appVersion,
                "buildTime", buildTime,
                "serverTime", Instant.now().toString()
        );
    }
}