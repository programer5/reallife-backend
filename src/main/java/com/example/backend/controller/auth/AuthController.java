package com.example.backend.controller.auth;

import com.example.backend.controller.auth.dto.LoginRequest;
import com.example.backend.controller.auth.dto.LoginResponse;
import com.example.backend.service.auth.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        String token = authService.login(request.email(), request.password());
        return new LoginResponse(token, "Bearer");
    }
}
