package com.breathingroom.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BenchmarksAndGuardTest {

    @Test
    void everyBenchmarkHasASourceADateAndALink() {
        Set<String> ids = new HashSet<>();
        for (Benchmarks.Benchmark b : Benchmarks.all()) {
            assertTrue(ids.add(b.id()), "duplicate id " + b.id());
            assertFalse(b.label().isBlank() || b.source().isBlank() || b.asOf().isBlank());
            assertTrue(b.value() > 0);
            if (!b.kind().equals("ASSUMPTION")) assertTrue(b.url().startsWith("https://"), b.id() + " needs an https link");
        }
        assertEquals(24_500, Benchmarks.value("irs_401k_limit"), 0.001);
        assertEquals(22.36, Benchmarks.value("fed_card_apr"), 0.001);
        assertEquals(3.7, Benchmarks.value("cpi_12mo"), 0.001);
        assertEquals(33.9, Benchmarks.value("bls_housing_share"), 0.001);
    }

    @Test
    void numberGuardChecksDollarsAndPercents() {
        List<String> facts = List.of("The IRS limit is $24,500.", "Average card rate 22.36%.", "You contribute 9.0%.");
        assertFalse(NumberGuard.containsUnknownNumber("The IRS limit is $24,500 and the Fed average is 22.4%.", facts));
        assertTrue(NumberGuard.containsUnknownNumber("The average card rate is 29%.", facts));
        assertTrue(NumberGuard.containsUnknownNumber("You could save $900.", facts));
        assertFalse(NumberGuard.containsUnknownNumber("Nothing numeric here.", facts));
    }
}
