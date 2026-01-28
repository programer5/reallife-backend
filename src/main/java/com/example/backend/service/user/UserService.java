package com.example.backend.service.user;

import com.example.backend.domain.user.User;
import com.example.backend.exception.DuplicateEmailException;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User register(String email, String rawPassword, String name) {

        if (userRepository.findByEmail(email).isPresent()) {
            throw new DuplicateEmailException();
        }

        String encodedPassword = passwordEncoder.encode(rawPassword);
        User user = new User(email, encodedPassword, name);
        return userRepository.saveAndFlush(user);
    }
}
