package com.breathingroom.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class SecurityRulesTest {

    @Test
    void passwordPolicyRules() {
        assertTrue(PasswordPolicy.validate("short", "a@b.com").isPresent());
        assertTrue(PasswordPolicy.validate("password123", "a@b.com").isPresent());
        assertTrue(PasswordPolicy.validate("aaaaaaaaaaaa", "a@b.com").isPresent());
        assertTrue(PasswordPolicy.validate("maya.rivera-2026!", "maya.rivera@example.com").isPresent());
        assertTrue(PasswordPolicy.validate("blue kettle sings softly", "maya@example.com").isEmpty());
    }

    @Test
    void emailRules() {
        assertTrue(PasswordPolicy.validEmail("maya@example.com"));
        assertFalse(PasswordPolicy.validEmail("not-an-email"));
        assertEquals("maya@example.com", PasswordPolicy.normalizeEmail("  Maya@Example.com "));
    }

    @Test
    void throttleLocksAfterFiveFailuresThenReleases() {
        AtomicLong clock = new AtomicLong(1_000);
        LoginThrottle t = new LoginThrottle(5, 600_000, 100, clock::get);
        for (int i = 0; i < 4; i++) { t.recordFailure("a@b.com"); assertFalse(t.isLocked("a@b.com")); }
        t.recordFailure("a@b.com");
        assertTrue(t.isLocked("a@b.com"));
        assertFalse(t.isLocked("other@b.com"));
        clock.addAndGet(600_001);
        assertFalse(t.isLocked("a@b.com"));
    }

    @Test
    void successfulLoginResetsTheCounter() {
        AtomicLong clock = new AtomicLong(1_000);
        LoginThrottle t = new LoginThrottle(3, 600_000, 100, clock::get);
        t.recordFailure("a@b.com"); t.recordFailure("a@b.com"); t.reset("a@b.com");
        t.recordFailure("a@b.com"); t.recordFailure("a@b.com");
        assertFalse(t.isLocked("a@b.com"));
    }

    @Test
    void dailyQuotaCapsPerUserAndResetsNextDay() {
        Clock day1 = Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC);
        DailyQuota q = new DailyQuota(2, day1);
        assertTrue(q.tryConsume(1)); assertTrue(q.tryConsume(1)); assertFalse(q.tryConsume(1));
        assertTrue(q.tryConsume(2));
        Clock day2 = Clock.fixed(Instant.parse("2026-10-21T12:00:00Z"), ZoneOffset.UTC);
        assertTrue(new DailyQuota(2, day2).tryConsume(1));
    }
}
