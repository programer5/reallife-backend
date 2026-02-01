package com.example.backend.controller.user;

import com.example.backend.controller.user.dto.UserCreateRequest;
import com.example.backend.controller.user.dto.UserCreateResponse;
import com.example.backend.domain.user.User;
import com.example.backend.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/api/users")
    public UserCreateResponse create(@RequestBody @Valid UserCreateRequest req) {

        User user = userService.register(
                req.email(),
                req.handle(),
                req.password(),
                req.name()
        );

        return new UserCreateResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getHandle(),
                user.getName()
        );
    }

    @GetMapping("/api/users/exists")
    public Map<String, Boolean> existsHandle(@RequestParam String handle) {
        return Map.of("exists", userService.existsHandle(handle));
    }
}
