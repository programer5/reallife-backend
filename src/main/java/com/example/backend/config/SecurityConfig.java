package com.example.backend.config;

import com.example.backend.security.JwtAuthenticationFilter;
import com.example.backend.security.JwtTokenProvider;
import com.example.backend.security.SecurityErrorHandler;
import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityErrorHandler securityErrorHandler;
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(securityErrorHandler)
                        .accessDeniedHandler(securityErrorHandler)
                )
                .authorizeHttpRequests(auth -> auth
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        .requestMatchers("/docs/**", "/static/**", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/api/users").permitAll()

                        // ✅ auth: 공개
                        .requestMatchers("/api/auth/login", "/api/auth/login-cookie").permitAll()
                        .requestMatchers("/api/auth/refresh", "/api/auth/refresh-cookie").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/files/*/download").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/files/*/thumbnail").permitAll()

                        // ✅ auth: 보호(인증 필요)
                        .requestMatchers("/api/auth/logout-cookie").authenticated()
                        .requestMatchers("/api/auth/logout-all").authenticated()
                        .requestMatchers("/api/auth/logout-all-cookie").authenticated()

                        // ✅ SSE 보호
                        .requestMatchers("/api/sse/**").authenticated()

                        .anyRequest().authenticated()
                )
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider),
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}