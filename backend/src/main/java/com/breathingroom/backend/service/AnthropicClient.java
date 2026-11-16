package com.breathingroom.backend.service;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Talks to the Anthropic Messages API. The key lives on the server only (never in React, never in Git).
 * If no key is set, or the call fails, chat() returns empty and the app shows its built-in text.
 */
@Service
public class AnthropicClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(AnthropicClient.class);

    private final RestClient http;
    private final String apiKey;
    private final String model;
    private volatile String lastError = "";

    public AnthropicClient(@Value("${anthropic.api-key:}") String apiKey,
                           @Value("${anthropic.model:claude-sonnet-5-5}") String model,
                           @Value("${anthropic.base-url:https://api.anthropic.com}") String baseUrl) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(Duration.ofSeconds(45));
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isEmpty() && !apiKey.startsWith("YOUR_");
    }

    @Override
    public String provider() {
        return "Anthropic";
    }

    @Override
    public String lastError() {
        return lastError;
    }

    @Override
    public String model() {
        return model;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Optional<String> chat(String system, String user) {
        if (!isConfigured()) return Optional.empty();
        lastError = "";
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("max_tokens", 700);
            body.put("system", system);
            body.put("messages", List.of(Map.of("role", "user", "content", user)));

            Map<String, Object> response = http.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
            StringBuilder text = new StringBuilder();
            for (Map<String, Object> block : content) {
                if ("text".equals(block.get("type")) && block.get("text") != null) text.append(block.get("text"));
            }
            if (text.toString().isBlank()) return Optional.empty();
            return Optional.of(text.toString());
        } catch (Exception e) {
            // Never log the key or the user's prompt (privacy). The class name is enough to debug.
            lastError = e instanceof org.springframework.web.client.RestClientResponseException r ? "HTTP " + r.getStatusCode().value() : e.getClass().getSimpleName();
            log.warn("Anthropic call failed: {}", lastError);
            return Optional.empty();
        }
    }
}
