package com.example.backend.service.user;

import com.example.backend.domain.user.User;
import com.example.backend.exception.BusinessException;
import com.example.backend.exception.ErrorCode;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(String email, String handle, String rawPassword, String name) {

        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        if (userRepository.existsByHandle(handle)) {
            throw new BusinessException(ErrorCode.DUPLICATE_HANDLE);
        }

        String encoded = passwordEncoder.encode(rawPassword);

        User user = new User(email, handle, encoded, name);
        return userRepository.save(user);
    }

    public boolean existsHandle(String handle) {
        return userRepository.existsByHandle(handle);
    }
}