package com.example.backend.service.user;

import com.example.backend.domain.user.User;
import com.example.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User register(String email, String password, String name) {
        User user = new User(email, password, name);
        return userRepository.save(user);
    }
}
