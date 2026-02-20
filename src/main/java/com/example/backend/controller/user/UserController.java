package com.example.backend.controller.user;

import com.example.backend.controller.user.dto.ProfileResponse;
import com.example.backend.controller.user.dto.UserCreateRequest;
import com.example.backend.controller.user.dto.UserCreateResponse;
import com.example.backend.domain.user.User;
import com.example.backend.service.user.UserProfileService;
import com.example.backend.service.user.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserProfileService userProfileService;

    @PostMapping
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

    @GetMapping("/exists")
    public Map<String, Boolean> existsHandle(@RequestParam String handle) {
        return Map.of("exists", userService.existsHandle(handle));
    }

    // ✅ 이제 원하는 경로로 열림
    @GetMapping("/{handle}")
    public ProfileResponse getProfile(@PathVariable String handle) {
        return userProfileService.getProfileByHandle(handle);
    }

    @GetMapping("/id/{userId}")
    public ProfileResponse getProfileById(@PathVariable UUID userId) {
        return userProfileService.getProfileById(userId);
    }
}