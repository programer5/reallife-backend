package com.example.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebCorsConfig implements WebMvcConfigurer {

    private final CorsProperties props;

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        // credentials=true일 때 allowedOrigins에 "*" 금지
        // (실수 방지용: 리스트에 "*"가 있으면 동작이 깨질 수 있으니 운영에서 제거 권장)
        registry.addMapping("/api/**")
                .allowedOrigins(props.getAllowedOrigins().toArray(new String[0]))
                .allowedMethods(props.getAllowedMethods().toArray(new String[0]))
                .allowedHeaders(props.getAllowedHeaders().toArray(new String[0]))
                .exposedHeaders(props.getExposedHeaders().toArray(new String[0]))
                .allowCredentials(props.isAllowCredentials())
                .maxAge(props.getMaxAgeSeconds());

        // SSE도 명시적으로 포함(실제로는 /api/**에 포함되지만 의도를 명확히)
        registry.addMapping("/api/sse/**")
                .allowedOrigins(props.getAllowedOrigins().toArray(new String[0]))
                .allowedMethods("GET", "OPTIONS")
                .allowedHeaders(props.getAllowedHeaders().toArray(new String[0]))
                .allowCredentials(true)
                .maxAge(props.getMaxAgeSeconds());
    }
}