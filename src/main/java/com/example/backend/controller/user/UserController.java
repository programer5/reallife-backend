package com.example.backend.controller.user;

import com.example.backend.domain.user.User;
import com.example.backend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public User register() {
        return userService.register(
                "test@test.com",
                "password",
                "테스트");
    }
}
