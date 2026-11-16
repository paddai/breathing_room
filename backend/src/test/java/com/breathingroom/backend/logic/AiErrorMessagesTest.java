package com.breathingroom.backend.logic;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AiErrorMessagesTest {

    @Test
    void commonCodesGetPlainEnglish() {
        assertTrue(AiErrorMessages.friendly("HTTP 401", "Anthropic").contains("key was rejected"));
        assertTrue(AiErrorMessages.friendly("HTTP 429", "OpenAI").contains("out of credit"));
        assertTrue(AiErrorMessages.friendly("HTTP 404", "OpenAI").contains("model"));
        assertTrue(AiErrorMessages.friendly("HTTP 529", "Anthropic").contains("overloaded"));
    }

    @Test
    void networkAndUnknownFailuresAreFriendlyToo() {
        assertTrue(AiErrorMessages.friendly("ResourceAccessException", "OpenAI").contains("internet connection"));
        assertTrue(AiErrorMessages.friendly("HTTP 418", "OpenAI").contains("HTTP 418"));
        assertFalse(AiErrorMessages.friendly("", "OpenAI").isBlank());
        assertFalse(AiErrorMessages.friendly(null, "OpenAI").isBlank());
    }
}
