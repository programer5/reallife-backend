package com.example.backend.security.oauth;

import com.example.backend.domain.user.AuthProvider;
import com.example.backend.domain.user.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class OAuth2UserServiceImpl extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public OAuth2UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        if (!"google".equalsIgnoreCase(registrationId)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        Map<String, Object> attributes = oauth2User.getAttributes();
        String providerId = (String) attributes.get("sub");
        String email = (String) attributes.get("email");
        String name = (String) attributes.get("name");

        if (!StringUtils.hasText(providerId) || !StringUtils.hasText(email)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        User user = userRepository.findByProviderAndProviderId(AuthProvider.GOOGLE, providerId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .map(existing -> {
                            existing.linkProvider(AuthProvider.GOOGLE, providerId);
                            return existing;
                        })
                        .orElseGet(() -> createNewUser(email, name, providerId)));

        Map<String, Object> enriched = new HashMap<>(attributes);
        enriched.put("userId", user.getId().toString());

        return new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                enriched,
                "sub"
        );
    }

    private User createNewUser(String email, String name, String providerId) {
        String baseHandle = createHandleBase(email);
        String handle = generateUniqueHandle(baseHandle);
        String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());

        User user = User.createOAuthUser(
                email,
                handle,
                randomPassword,
                name != null ? name : "USER",
                AuthProvider.GOOGLE,
                providerId
        );

        return userRepository.save(user);
    }

    private String createHandleBase(String email) {
        String local = email.split("@")[0].toLowerCase();
        String sanitized = local.replaceAll("[^a-z0-9._]", "");
        return sanitized.isBlank() ? "user" : sanitized;
    }

    private String generateUniqueHandle(String base) {
        String handle = base;
        while (userRepository.existsByHandle(handle)) {
            String suffix = UUID.randomUUID().toString().substring(0, 6);
            handle = base + "_" + suffix;
        }
        return handle;
    }
}
