package com.example.backend.controller.auth;

import com.example.backend.controller.auth.dto.LoginRequest;
import com.example.backend.controller.auth.dto.LoginResponse;
import com.example.backend.security.LoginRateLimiter;
import com.example.backend.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginRateLimiter loginRateLimiter;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {

        if (!loginRateLimiter.allow(request.email())) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }

        String token = authService.login(request.email(), request.password());
        return new LoginResponse(token, "Bearer");
    }
}
