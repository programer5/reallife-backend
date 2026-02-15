package com.example.backend.controller.auth;

import com.example.backend.controller.auth.dto.LoginCookieResponse;
import com.example.backend.controller.auth.dto.LoginRequest;
import com.example.backend.controller.auth.dto.LoginResponse;
import com.example.backend.controller.auth.dto.RefreshRequest;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.security.LoginRateLimiter;
import com.example.backend.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    /**
     * ✅ 기존 방식 유지: 토큰을 바디로 반환 (API 클라이언트용)
     */
    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest
    ) {
        String ip = clientIp(httpRequest);

        if (!loginRateLimiter.allow(request.email(), ip)) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }

        var tokens = authService.loginWithRefresh(request.email(), request.password());
        return new LoginResponse(tokens.accessToken(), tokens.refreshToken(), "Bearer");
    }

    /**
     * ✅ 추천 방식: 브라우저(SSE 포함)용 - HttpOnly 쿠키로 토큰 심기
     */
    @PostMapping("/login-cookie")
    public LoginCookieResponse loginCookie(@Valid @RequestBody LoginRequest request,
                                           jakarta.servlet.http.HttpServletResponse response) {

        var tokens = authService.loginWithRefresh(request.email(), request.password());

        ResponseCookie accessCookie = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, tokens.accessToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(60 * 15) // Access 15분(권장)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from(JwtAuthenticationFilter.REFRESH_TOKEN_COOKIE, tokens.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/api/auth") // refresh/logout 범위만 쿠키 전송되게 좁히기(권장)
                .sameSite("Lax")
                .maxAge(60L * 60 * 24 * 14) // Refresh 14일(권장)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return new LoginCookieResponse("OK");
    }

    /**
     * ✅ 쿠키 로그아웃(쿠키 삭제)
     */
    @PostMapping("/logout-cookie")
    public LoginCookieResponse logoutCookie(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response
    ) {
        String refreshRaw = null;
        var cookies = request.getCookies();
        if (cookies != null) {
            for (var c : cookies) {
                if (JwtAuthenticationFilter.REFRESH_TOKEN_COOKIE.equals(c.getName())) {
                    refreshRaw = c.getValue();
                    break;
                }
            }
        }

        authService.revokeRefresh(refreshRaw);

        ResponseCookie accessCookie = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true).secure(false).path("/").sameSite("Lax").maxAge(0).build();

        ResponseCookie refreshCookie = ResponseCookie.from(JwtAuthenticationFilter.REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true).secure(false).path("/api/auth").sameSite("Lax").maxAge(0).build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return new LoginCookieResponse("OK");
    }

    @PostMapping("/refresh-cookie")
    public LoginCookieResponse refreshCookie(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response
    ) {
        String refreshRaw = null;
        var cookies = request.getCookies();
        if (cookies != null) {
            for (var c : cookies) {
                if (JwtAuthenticationFilter.REFRESH_TOKEN_COOKIE.equals(c.getName())) {
                    refreshRaw = c.getValue();
                    break;
                }
            }
        }

        var tokens = authService.refreshRotate(refreshRaw);

        ResponseCookie accessCookie = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, tokens.accessToken())
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(60 * 15)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from(JwtAuthenticationFilter.REFRESH_TOKEN_COOKIE, tokens.refreshToken())
                .httpOnly(true)
                .secure(false)
                .path("/api/auth")
                .sameSite("Lax")
                .maxAge(60L * 60 * 24 * 14)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
        return new LoginCookieResponse("OK");
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {

        var tokens = authService.refreshRotate(request.refreshToken());

        return new LoginResponse(
                tokens.accessToken(),
                tokens.refreshToken(),
                "Bearer"
        );
    }

    @PostMapping("/logout-all-cookie")
    public LoginCookieResponse logoutAllCookie(
            @AuthenticationPrincipal String userId,
            jakarta.servlet.http.HttpServletResponse response
    ) {
        authService.logoutAll(java.util.UUID.fromString(userId));

        ResponseCookie accessCookie = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true).secure(false).path("/").sameSite("Lax").maxAge(0).build();

        ResponseCookie refreshCookie = ResponseCookie.from(JwtAuthenticationFilter.REFRESH_TOKEN_COOKIE, "")
                .httpOnly(true).secure(false).path("/api/auth").sameSite("Lax").maxAge(0).build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return new LoginCookieResponse("OK");
    }

    @PostMapping("/logout-all")
    public LoginResponse logoutAll(@AuthenticationPrincipal String userId) {

        if (userId == null || userId.isBlank()) {
            throw new com.example.backend.exception.BusinessException(
                    com.example.backend.exception.ErrorCode.UNAUTHORIZED
            );
        }

        authService.logoutAll(java.util.UUID.fromString(userId));
        return new LoginResponse(null, null, "Bearer");
    }

    private String clientIp(jakarta.servlet.http.HttpServletRequest request) {
        // reverse proxy 환경 대비
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // "client, proxy1, proxy2" 형태라 첫 번째가 원 IP인 경우가 많음
            return xff.split(",")[0].trim();
        }
        String xri = request.getHeader("X-Real-IP");
        if (xri != null && !xri.isBlank()) return xri.trim();

        return request.getRemoteAddr();
    }
}