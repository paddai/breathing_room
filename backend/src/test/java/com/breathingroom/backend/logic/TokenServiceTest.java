package com.breathingroom.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    private static final byte[] SECRET = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final Clock NOW = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);

    @Test
    void issuedTokenVerifiesToTheSameUser() {
        TokenService t = new TokenService(SECRET, Duration.ofHours(8), NOW);
        assertEquals(Optional.of(42L), t.verify(t.issue(42L)));
    }

    @Test
    void tamperedTokenIsRejected() {
        TokenService t = new TokenService(SECRET, Duration.ofHours(8), NOW);
        String[] parts = t.issue(42L).split("\\.");
        String forgedPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(("{\"sub\":\"1\",\"iat\":1,\"exp\":99999999999}").getBytes(StandardCharsets.UTF_8));
        assertTrue(t.verify(parts[0] + "." + forgedPayload + "." + parts[2]).isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        String token = new TokenService(SECRET, Duration.ofHours(1), NOW).issue(7L);
        Clock later = Clock.fixed(Instant.parse("2026-10-20T14:00:00Z"), ZoneOffset.UTC);
        assertTrue(new TokenService(SECRET, Duration.ofHours(1), later).verify(token).isEmpty());
    }

    @Test
    void tokenFromAnotherSecretIsRejected() {
        String token = new TokenService(SECRET, Duration.ofHours(8), NOW).issue(7L);
        byte[] other = "ffffffffffffffffffffffffffffffff".getBytes(StandardCharsets.UTF_8);
        assertTrue(new TokenService(other, Duration.ofHours(8), NOW).verify(token).isEmpty());
    }

    @Test
    void algNoneAndGarbageAreRejected() {
        TokenService t = new TokenService(SECRET, Duration.ofHours(8), NOW);
        Base64.Encoder e = Base64.getUrlEncoder().withoutPadding();
        String none = e.encodeToString("{\"alg\":\"none\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8)) + "."
                + e.encodeToString("{\"sub\":\"1\",\"exp\":99999999999}".getBytes(StandardCharsets.UTF_8)) + ".";
        assertTrue(t.verify(none).isEmpty());
        assertTrue(t.verify("not a token").isEmpty());
        assertTrue(t.verify(null).isEmpty());
        assertTrue(t.verify("a.b.c").isEmpty());
    }

    @Test
    void shortSecretIsRefused() {
        assertThrows(IllegalArgumentException.class, () -> new TokenService(new byte[8], Duration.ofHours(1), NOW));
    }
}
