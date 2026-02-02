package com.example.backend.config;

import com.querydsl.jpa.JPQLTemplates;
import com.querydsl.jpa.impl.JPAQueryFactory;
import jakarta.persistence.EntityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QuerydslConfig {

    @Bean
    public JPAQueryFactory jpaQueryFactory(EntityManager em) {
        // ✅ HibernateQueryHandler 대신 DefaultQueryHandler로 동작하게 해서
        //    ScrollableResults.get(int) NoSuchMethodError를 회피
        return new JPAQueryFactory(JPQLTemplates.DEFAULT, em);
    }
}
