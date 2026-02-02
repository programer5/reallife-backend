package com.example.backend.controller.user;

import com.example.backend.controller.user.dto.MeResponse;
import com.example.backend.domain.user.FollowerTier;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;

    @GetMapping("/api/me")
    public MeResponse me(Authentication authentication) {
        UUID meId = UUID.fromString(authentication.getName());

        var user = userRepository.findById(meId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        var tier = FollowerTier.of(user.getFollowerCount());

        return new MeResponse(
                user.getId().toString(),
                user.getEmail(),
                user.getHandle(),
                user.getName(),
                user.getFollowerCount(),
                tier.name()
        );
    }
}