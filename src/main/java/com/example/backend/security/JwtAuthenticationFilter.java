package com.example.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    // ✅ 쿠키 이름 통일
    public static final String ACCESS_TOKEN_COOKIE = "access_token";
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            jakarta.servlet.http.HttpServletRequest request,
            jakarta.servlet.http.HttpServletResponse response,
            jakarta.servlet.FilterChain filterChain
    ) throws java.io.IOException, jakarta.servlet.ServletException {

        String token = resolveToken(request);

        if (token == null || token.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        // ✅ 여기서 userId를 "String"으로 뽑아 principal로 넣어야
        // @AuthenticationPrincipal String userId 가 null이 안 됨
        String userId = jwtTokenProvider.getSubject(token); // sub 값 (UUID 문자열)

        var auth = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                userId,                 // ✅ principal을 String으로
                null,
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER"))
        );

        auth.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource()
                .buildDetails(request));

        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }

    private String resolveToken(jakarta.servlet.http.HttpServletRequest request) {
        // 1) Authorization: Bearer xxx
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }

        // 2) Cookie fallback (웹/SSE)
        var cookies = request.getCookies();
        if (cookies == null) return null;

        for (var c : cookies) {
            if (ACCESS_TOKEN_COOKIE.equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.startsWith("/docs")
                || uri.startsWith("/static")
                || uri.startsWith("/favicon.ico")
                || uri.startsWith("/error");
    }
}