package com.example.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "ops.admin")
public class OpsAdminProperties {
    private List<String> allowedEmails = new ArrayList<>();
    private List<String> allowedHandles = new ArrayList<>();
}
