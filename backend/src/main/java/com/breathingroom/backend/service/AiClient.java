package com.breathingroom.backend.service;

import java.util.Optional;

/** One AI provider. The advisor only needs "is it set up", "which model" and "send this text, get text back". */
public interface AiClient {

    boolean isConfigured();

    String provider();

    String model();

    Optional<String> chat(String system, String user);

    /** Short, safe reason the last call failed (for example "HTTP 401"), or empty if the last call worked or none was made. Never contains the key. */
    default String lastError() {
        return "";
    }
}
