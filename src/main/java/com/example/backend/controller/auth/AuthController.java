package com.example.backend.controller.auth;

import com.example.backend.controller.auth.dto.LoginCookieResponse;
import com.example.backend.controller.auth.dto.LoginRequest;
import com.example.backend.controller.auth.dto.LoginResponse;
import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.security.LoginRateLimiter;
import com.example.backend.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;
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
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {

        if (!loginRateLimiter.allow(request.email())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }

        String token = authService.login(request.email(), request.password());
        return new LoginResponse(token, "Bearer");
    }

    /**
     * ✅ 추천 방식: 브라우저(SSE 포함)용 - HttpOnly 쿠키로 토큰 심기
     */
    @PostMapping("/login-cookie")
    public LoginCookieResponse loginCookie(@Valid @RequestBody LoginRequest request,
                                           jakarta.servlet.http.HttpServletResponse response) {

        if (!loginRateLimiter.allow(request.email())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }

        String token = authService.login(request.email(), request.password());

        // dev 기준: Secure(false). 운영 HTTPS면 true 권장
        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, token)
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(60 * 60 * 6) // 6시간 예시
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return new LoginCookieResponse("OK");
    }

    /**
     * ✅ 쿠키 로그아웃(쿠키 삭제)
     */
    @PostMapping("/logout-cookie")
    public LoginCookieResponse logoutCookie(jakarta.servlet.http.HttpServletResponse response) {

        ResponseCookie cookie = ResponseCookie.from(JwtAuthenticationFilter.ACCESS_TOKEN_COOKIE, "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .sameSite("Lax")
                .maxAge(0)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return new LoginCookieResponse("OK");
    }
}