package com.example.backend.controller.auth;

import com.example.backend.config.AuthCookieFactory;
import com.example.backend.controller.auth.dto.LoginCookieResponse;
import com.example.backend.controller.auth.dto.LoginRequest;
import com.example.backend.controller.auth.dto.LoginResponse;
import com.example.backend.controller.auth.dto.RefreshRequest;
import com.example.backend.security.LoginRateLimiter;
import com.example.backend.service.auth.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;
    private final AuthCookieFactory cookieFactory;

    /**
     * ✅ 기존 방식 유지: 토큰을 바디로 반환 (API 클라이언트용)
     */
    @PostMapping("/login")
    public LoginResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
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
    public LoginCookieResponse loginCookie(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse response
    ) {
        var tokens = authService.loginWithRefresh(request.email(), request.password());

        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.accessCookie(tokens.accessToken(), httpRequest).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.refreshCookie(tokens.refreshToken(), httpRequest).toString());

        return new LoginCookieResponse("OK");
    }

    /**
     * ✅ 쿠키 로그아웃(쿠키 삭제)
     */
    @PostMapping("/logout-cookie")
    public LoginCookieResponse logoutCookie(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshRaw = extractCookie(request, "refresh_token");
        authService.revokeRefresh(refreshRaw);

        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.deleteAccessCookie(request).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.deleteRefreshCookie(request).toString());

        return new LoginCookieResponse("OK");
    }

    @PostMapping("/refresh-cookie")
    public LoginCookieResponse refreshCookie(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshRaw = extractCookie(request, "refresh_token");

        var tokens = authService.refreshRotate(refreshRaw);

        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.accessCookie(tokens.accessToken(), request).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.refreshCookie(tokens.refreshToken(), request).toString());

        return new LoginCookieResponse("OK");
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshRequest request) {
        var tokens = authService.refreshRotate(request.refreshToken());
        return new LoginResponse(tokens.accessToken(), tokens.refreshToken(), "Bearer");
    }

    @PostMapping("/logout-all-cookie")
    public LoginCookieResponse logoutAllCookie(
            @AuthenticationPrincipal String userId,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        authService.logoutAll(UUID.fromString(userId));

        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.deleteAccessCookie(request).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.deleteRefreshCookie(request).toString());

        return new LoginCookieResponse("OK");
    }

    @PostMapping("/logout-all")
    public LoginResponse logoutAllBearer(
            @AuthenticationPrincipal String userId
    ) {
        // userId는 JWT 인증 필터에서 Principal에 문자열(UUID)로 넣어주는 전제
        authService.logoutAll(UUID.fromString(userId));

        // 기존 테스트/문서가 response 필드를 기대하므로 형태는 유지
        return new LoginResponse(null, null, "Bearer");
    }

    private static String extractCookie(HttpServletRequest request, String name) {
        var cookies = request.getCookies();
        if (cookies == null) return null;
        for (var c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private static String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return request.getRemoteAddr();
    }
}