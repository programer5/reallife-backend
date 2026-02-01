package com.example.backend.controller.user;

import com.example.backend.controller.user.dto.MeResponse;
import com.example.backend.domain.user.FollowerTier;
import com.example.backend.domain.user.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class MeController {

    private final UserRepository userRepository;

    @GetMapping("/api/me")
    public MeResponse me(Authentication authentication) {
        if (authentication == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        // JwtAuthenticationFilter에서 principal을 email로 넣었음
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        FollowerTier tier = FollowerTier.of(user.getFollowerCount());

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
