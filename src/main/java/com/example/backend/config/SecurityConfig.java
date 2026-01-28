package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth
                        // ✅ 문서 공개
                        .requestMatchers("/docs", "/docs/**").permitAll()

                        // ✅ 정적 리소스(혹시 docs 외에도 필요하면)
                        .requestMatchers("/favicon.ico", "/css/**", "/js/**", "/images/**").permitAll()

                        // ✅ 회원가입(지금 구현한 API)
                        .requestMatchers("/api/users").permitAll()

                        // ✅ 로그인/인증 관련(추후)
                        .requestMatchers("/api/auth/**").permitAll()

                        // 그 외는 인증 필요
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}
