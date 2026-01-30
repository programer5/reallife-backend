package com.example.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper om = new ObjectMapper();
        // LocalDateTime 같은 Java Time 직렬화 지원(우리 ErrorResponse.timestamp 때문에 필요)
        om.registerModule(new JavaTimeModule());
        return om;
    }
}
