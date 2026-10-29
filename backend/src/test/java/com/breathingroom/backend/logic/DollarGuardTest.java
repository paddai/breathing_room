package com.breathingroom.backend.logic;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class DollarGuardTest {

    private static final List<String> ALLOWED = List.of("$350.00", "$64.50", "Plan: $1,200.00 total");

    @Test
    void allowsFiguresThatJavaCalculated() {
        assertFalse(DollarGuard.containsUnknownDollarAmount("You would save $350 a month.", ALLOWED));
        assertFalse(DollarGuard.containsUnknownDollarAmount("About $65 per paycheck, or $1,200.00 overall.", ALLOWED));
    }

    @Test
    void rejectsInventedFigures() {
        assertTrue(DollarGuard.containsUnknownDollarAmount("You would save $400 a month.", ALLOWED));
        assertTrue(DollarGuard.containsUnknownDollarAmount("Pay $350 now and $90 later.", ALLOWED));
    }

    @Test
    void textWithoutDollarsIsFine() {
        assertFalse(DollarGuard.containsUnknownDollarAmount("This is a good time to review your plan.", ALLOWED));
    }
}
