package com.example.backend.config;

import com.example.backend.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class AuthCookieFactory {

    private final CookieProperties props;

    public ResponseCookie accessCookie(String value, HttpServletRequest request) {
        return base(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, value, "/", props.getAccessMaxAgeSeconds(), request);
    }

    public ResponseCookie refreshCookie(String value, HttpServletRequest request) {
        return base(JwtAuthenticationFilter.REFRESH_TOKEN_COOKIE, value, props.getRefreshPath(), props.getRefreshMaxAgeSeconds(), request);
    }

    public ResponseCookie deleteAccessCookie(HttpServletRequest request) {
        return base(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, "", "/", 0, request);
    }

    public ResponseCookie deleteRefreshCookie(HttpServletRequest request) {
        return base(JwtAuthenticationFilter.REFRESH_TOKEN_COOKIE, "", props.getRefreshPath(), 0, request);
    }

    private ResponseCookie base(String name, String value, String path, long maxAgeSeconds, HttpServletRequest request) {
        boolean secure = resolveSecure(request);

        // SameSite=None이면 Secure=true가 사실상 필수(브라우저 정책)
        String sameSite = props.getSameSite();
        if ("None".equalsIgnoreCase(sameSite)) {
            secure = true;
        }

        ResponseCookie.ResponseCookieBuilder b = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .sameSite(sameSite)
                .maxAge(maxAgeSeconds);

        if (StringUtils.hasText(props.getDomain())) {
            b.domain(props.getDomain());
        }

        return b.build();
    }

    private boolean resolveSecure(HttpServletRequest request) {
        String mode = props.getSecureMode();
        if ("true".equalsIgnoreCase(mode)) return true;
        if ("false".equalsIgnoreCase(mode)) return false;

        // auto: https or X-Forwarded-Proto=https 이면 secure=true
        if (request.isSecure()) return true;

        String xfp = request.getHeader("X-Forwarded-Proto");
        return xfp != null && xfp.toLowerCase().contains("https");
    }
}