package com.example.backend.config;

import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.security.SecurityErrorHandler;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile; // ✅ 추가
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@org.springframework.context.annotation.Import(com.example.backend.config.SecurityConfig.class)
@TestConfiguration
@Profile("test") // ✅ 테스트 프로필에서만 적용
public class TestSecurityConfig {

    private final SecurityErrorHandler securityErrorHandler;
    private final JwtTokenProvider jwtTokenProvider;

    public TestSecurityConfig(SecurityErrorHandler securityErrorHandler, JwtTokenProvider jwtTokenProvider) {
        this.securityErrorHandler = securityErrorHandler;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Bean
    SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/docs/**", "/static/**").permitAll()
                        .requestMatchers("/api/users").permitAll()
                        .requestMatchers("/api/auth/login", "/api/auth/login-cookie").permitAll()
                        .requestMatchers("/api/auth/refresh", "/api/auth/refresh-cookie").permitAll()
                        .requestMatchers("/api/auth/logout-cookie", "/api/auth/logout-all", "/api/auth/logout-all-cookie").authenticated()
                        .requestMatchers("/api/sse/**").authenticated()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}