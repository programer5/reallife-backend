package com.example.backend.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        // ✅ 이미 인증이 세팅되어 있으면(다른 필터/테스트) 중복 처리 방지
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        // ✅ 토큰 없으면 그냥 통과 (permitAll은 정상, protected는 EntryPoint가 401 처리)
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7).trim();
        if (!StringUtils.hasText(token)) {
            chain.doFilter(request, response);
            return;
        }

        try {
            Claims claims = jwtTokenProvider.parse(token);

            String email = (String) claims.get("email");
            if (!StringUtils.hasText(email)) {
                // email이 없으면 인증 세팅하지 않고 통과
                chain.doFilter(request, response);
                return;
            }

            var auth = new UsernamePasswordAuthenticationToken(
                    email, null, List.of()
            );
            auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception e) {
            // ✅ 토큰이 이상하면 인증 비우고 통과
            //    protected 요청이면 SecurityErrorHandler가 401 JSON 내려줌
            SecurityContextHolder.clearContext();
        }

        chain.doFilter(request, response);
    }

    // (선택) 문서/정적리소스는 필터 제외하면 더 깔끔
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/docs")
                || uri.startsWith("/static")
                || uri.startsWith("/favicon.ico")
                || uri.startsWith("/error");
    }
}
