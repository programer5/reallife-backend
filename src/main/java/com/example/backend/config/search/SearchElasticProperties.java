package com.example.backend.config.search;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.search.elastic")
public class SearchElasticProperties {
    private boolean enabled = false;
    private String baseUrl = "http://localhost:9200";
    private String indexName = "reallife_search";
    private String apiKey = "";
    private String username = "";
    private String password = "";
    private int connectTimeoutMillis = 1500;
    private int readTimeoutMillis = 2500;

    private String reindexAdminToken = "";
}