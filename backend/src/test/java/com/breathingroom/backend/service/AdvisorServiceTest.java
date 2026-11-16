package com.breathingroom.backend.service;

import static org.junit.jupiter.api.Assertions.*;

import com.breathingroom.backend.logic.AdvisorPlanner;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AdvisorServiceTest {

    private record Fake(String provider, boolean configured) implements AiClient {
        public boolean isConfigured() { return configured; }
        public String model() { return "m"; }
        public Optional<String> chat(String s, String u) { return Optional.empty(); }
    }

    @Test
    void autoPrefersAnthropicWhenConfigured() {
        AiClient a = new Fake("Anthropic", true), o = new Fake("OpenAI", true);
        assertSame(a, AdvisorService.pick("auto", a, o));
    }

    @Test
    void autoFallsBackToOpenAiWhenOnlyItIsConfigured() {
        AiClient a = new Fake("Anthropic", false), o = new Fake("OpenAI", true);
        assertSame(o, AdvisorService.pick("auto", a, o));
    }

    @Test
    void explicitProviderWins() {
        AiClient a = new Fake("Anthropic", true), o = new Fake("OpenAI", true);
        assertSame(o, AdvisorService.pick("openai", a, o));
        assertSame(a, AdvisorService.pick("ANTHROPIC", a, o));
        assertSame(a, AdvisorService.pick(null, a, o));
    }

    @Test
    void fallbackAnswerIsSpecificAndMentionsOptions() {
        AdvisorPlanner.Plan p = new AdvisorPlanner.Plan(AdvisorPlanner.Intent.DEBT, "Your highest rate is Visa.", Map.of(),
                List.of(new AdvisorPlanner.Choice("A", "d", "t"), new AdvisorPlanner.Choice("B", "d", "t")));
        String text = AdvisorService.fallbackAnswer(p);
        assertTrue(text.startsWith("Your highest rate is Visa."));
        assertTrue(text.contains("2 ways"));
    }
}
