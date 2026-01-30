package com.example.backend.security;

import com.example.backend.exception.ErrorCode;
import com.example.backend.exception.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        write(response, request, ErrorCode.UNAUTHORIZED);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        write(response, request, ErrorCode.FORBIDDEN);
    }

    private void write(HttpServletResponse response, HttpServletRequest request, ErrorCode ec) throws IOException {

        response.setStatus(ec.status().value());
        response.setContentType("application/json;charset=UTF-8");

        ErrorResponse body = new ErrorResponse(
                ec.code(),
                ec.message(),
                LocalDateTime.now(),
                request.getRequestURI(),
                null
        );

        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
