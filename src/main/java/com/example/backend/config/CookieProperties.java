package com.example.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {

    /**
     * secure 모드:
     * - "auto": 요청이 https(또는 x-forwarded-proto=https)이면 secure=true
     * - "true": 항상 secure=true
     * - "false": 항상 secure=false (로컬 개발용)
     */
    private String secureMode = "auto";

    /**
     * SameSite 정책:
     * - Lax: 동일 사이트 SPA 기본 권장
     * - None: 크로스 도메인(front/back 분리)일 때 필요 (반드시 Secure=true 필요)
     */
    private String sameSite = "Lax";

    /**
     * 쿠키 도메인 (옵션)
     * - 비우면 host-only cookie(기본)
     * - 예: ".example.com"
     */
    private String domain;

    /**
     * access token 쿠키 maxAge (초)
     */
    private long accessMaxAgeSeconds = 60L * 15; // 15분

    /**
     * refresh token 쿠키 maxAge (초)
     */
    private long refreshMaxAgeSeconds = 60L * 60 * 24 * 14; // 14일

    /**
     * refresh token 쿠키 path
     */
    private String refreshPath = "/api/auth";
}