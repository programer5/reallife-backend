package com.example.backend.ops;

import com.example.backend.config.OpsAdminProperties;
import com.example.backend.domain.user.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OpsAccessService {

    private final UserRepository userRepository;
    private final OpsAdminProperties props;

    public void requireOps(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        UUID userId = UUID.fromString(authentication.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Set<String> allowedEmails = props.getAllowedEmails().stream()
                .map(OpsAccessService::normalize)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());
        Set<String> allowedHandles = props.getAllowedHandles().stream()
                .map(OpsAccessService::normalize)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        String email = normalize(user.getEmail());
        String handle = normalize(user.getHandle());

        boolean emailAllowed = !email.isBlank() && allowedEmails.contains(email);
        boolean handleAllowed = !handle.isBlank() && allowedHandles.contains(handle);

        if (!emailAllowed && !handleAllowed) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
