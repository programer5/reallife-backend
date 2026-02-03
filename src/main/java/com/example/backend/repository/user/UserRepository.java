package com.example.backend.repository.user;

import com.example.backend.domain.user.AuthProvider;
import com.example.backend.domain.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByHandle(String handle);
    Optional<User> findByHandle(String handle);
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);
}
