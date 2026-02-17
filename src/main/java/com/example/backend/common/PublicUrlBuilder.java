package com.example.backend.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class PublicUrlBuilder {

    private final String baseUrl;

    public PublicUrlBuilder(@Value("${app.public-base-url:}") String baseUrl) {
        this.baseUrl = normalizeBaseUrl(baseUrl);
        log.info("PUBLIC BASE URL = {}", baseUrl);
    }

    public String absolute(String path) {
        if (!StringUtils.hasText(path)) return path;

        // baseUrl 설정이 비어있으면(예: 테스트) 기존 상대경로 그대로 반환
        if (!StringUtils.hasText(baseUrl)) {
            return ensureLeadingSlash(path);
        }

        String p = ensureLeadingSlash(path);
        return baseUrl + p;
    }

    private String normalizeBaseUrl(String v) {
        if (!StringUtils.hasText(v)) return "";
        String s = v.trim();
        // 끝 슬래시 제거
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    private String ensureLeadingSlash(String v) {
        String s = v.trim();
        return s.startsWith("/") ? s : "/" + s;
    }
}