package com.example.backend.controller.user;

import com.example.backend.controller.user.dto.UserCreateRequest;
import com.example.backend.controller.user.dto.UserCreateResponse;
import com.example.backend.domain.user.User;
import com.example.backend.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public UserCreateResponse register(@Valid @RequestBody UserCreateRequest request) {

        User user = userService.register(request.email(), request.password(), request.name());
        return new UserCreateResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getCreatedAt()
        );
    }
}
