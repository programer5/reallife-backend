package com.example.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * 예: ["http://localhost:5173", "https://app.example.com"]
     * credentials=true를 쓰므로 "*"는 사용하면 안 됩니다.
     */
    private List<String> allowedOrigins = new ArrayList<>();

    private List<String> allowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    /**
     * 프론트에서 보낼 수 있는 헤더 목록
     * (Authorization은 Bearer 모드도 지원하므로 포함)
     */
    private List<String> allowedHeaders = List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "X-Requested-With",
            "Last-Event-ID"
    );

    /**
     * 브라우저가 접근 가능한 응답 헤더
     * Set-Cookie는 JS가 접근 못 하지만, 프론트 디버깅용으로 필요한 경우가 있어 추가 가능
     */
    private List<String> exposedHeaders = List.of(
            "Location"
    );

    private boolean allowCredentials = true;

    /**
     * preflight 캐시(초)
     */
    private long maxAgeSeconds = 3600;
}