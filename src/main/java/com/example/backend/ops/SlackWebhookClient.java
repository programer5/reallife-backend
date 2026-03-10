package com.example.backend.ops;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackWebhookClient {

    private final ObjectMapper objectMapper;

    @Value("${ops.alert.enabled:false}")
    private boolean enabled;

    @Value("${ops.alert.slack.webhook-url:}")
    private String webhookUrl;

    @Value("${ops.alert.slack.username:RealLife Ops}")
    private String username;

    @Value("${ops.alert.slack.icon-emoji::rotating_light:}")
    private String iconEmoji;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean hasWebhookConfigured() {
        return webhookUrl != null && !webhookUrl.isBlank();
    }

    public boolean isAvailable() {
        return enabled && hasWebhookConfigured();
    }

    public boolean send(String title, String body) {
        if (!isAvailable()) {
            log.debug("Slack webhook disabled or missing. title={}", title);
            return false;
        }

        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("username", username);
            payload.put("icon_emoji", iconEmoji);
            payload.put("text", "*" + safe(title) + "*\n" + safe(body));

            byte[] json = objectMapper.writeValueAsBytes(payload);

            URL url = URI.create(webhookUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            conn.setRequestProperty("Accept", "application/json");

            try (OutputStream os = conn.getOutputStream()) {
                os.write(json);
            }

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("Slack webhook responded with status=" + code);
            }

            return true;
        } catch (Exception e) {
            log.error("Failed to send Slack webhook. title={}", title, e);
            return false;
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}