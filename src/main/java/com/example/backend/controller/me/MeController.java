package com.example.backend.controller.me;

import com.example.backend.controller.me.dto.ProfileUpdateRequest;
import com.example.backend.controller.user.dto.MeResponse;
import com.example.backend.controller.user.dto.ProfileResponse;
import com.example.backend.domain.user.FollowerTier;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.user.UserRepository;
import com.example.backend.service.user.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/me")
public class MeController {

    private final UserRepository userRepository;
    private final UserProfileService userProfileService;

    @GetMapping
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

    @PatchMapping("/profile")
    public ProfileResponse updateProfile(Authentication authentication,
                                         @RequestBody @Valid ProfileUpdateRequest request) {
        UUID meId = UUID.fromString(authentication.getName());
        return userProfileService.updateMyProfile(meId, request);
    }
}