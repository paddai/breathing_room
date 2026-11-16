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
 * The ONLY place the app talks to OpenAI. The API key lives on the server (never in React, never in Git).
 * If no key is set, or the call fails, chat() returns empty and the app falls back to built-in text.
 */
@Service
public class OpenAiClient implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(OpenAiClient.class);

    private final RestClient http;
    private final String apiKey;
    private final String model;
    private volatile String lastError = "";

    public OpenAiClient(@Value("${openai.api-key:}") String apiKey,
                        @Value("${openai.model:gpt-4.1-mini}") String model,
                        @Value("${openai.base-url:https://api.openai.com/v1}") String baseUrl) {
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        HttpClient jdk = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(jdk);
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.http = RestClient.builder().baseUrl(baseUrl).requestFactory(factory).build();
    }

    @Override
    public boolean isConfigured() {
        return !apiKey.isEmpty() && !apiKey.startsWith("YOUR_");
    }

    @Override
    public String provider() {
        return "OpenAI";
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
            body.put("messages", List.of(
                    Map.of("role", "system", "content", system),
                    Map.of("role", "user", "content", user)));

            Map<String, Object> response = http.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            Object content = message.get("content");
            if (content == null || content.toString().isBlank()) return Optional.empty();
            return Optional.of(content.toString());
        } catch (Exception e) {
            // Never log the key or the user's prompt (privacy). The class name is enough to debug.
            lastError = e instanceof org.springframework.web.client.RestClientResponseException r ? "HTTP " + r.getStatusCode().value() : e.getClass().getSimpleName();
            log.warn("OpenAI call failed: {}", lastError);
            return Optional.empty();
        }
    }
}
