package com.breathingroom.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Pay frequencies, including Annually. Expected numbers were worked out by hand. */
class PaycheckFrequencyTest {

    @Test
    void periodsPerYearForEveryFrequency() {
        assertEquals(52, PaycheckMath.periodsPerYear("weekly"));
        assertEquals(26, PaycheckMath.periodsPerYear("biweekly"));
        assertEquals(24, PaycheckMath.periodsPerYear("semimonthly"));
        assertEquals(12, PaycheckMath.periodsPerYear("monthly"));
        assertEquals(1, PaycheckMath.periodsPerYear("annually"));
        assertTrue(PaycheckMath.FREQUENCIES.contains("annually"));
    }

    @Test
    void annualSalaryGivesYearlyAndMonthlyTakeHome() {
        // $60,000 a year, 10% to the 401(k) = $6,000, yearly deductions $12,000 -> take-home $42,000 a year = $3,500 a month
        PaycheckMath.Result r = PaycheckMath.compute("salary", "annually", 0, 0, 0, 1.5, 60000, 10, 50, 6, 12000);
        assertEquals(60000.0, r.grossPerPeriod(), 0.001);
        assertEquals(1, r.periodsPerYear());
        assertEquals(60000.0, r.grossPerYear(), 0.001);
        assertEquals(6000.0, r.k401PerPeriod(), 0.001);
        assertEquals(1800.0, r.employerMatchPerPeriod(), 0.001); // 50% of the first 6% of $60,000
        assertEquals(42000.0, r.takeHomePerPeriod(), 0.001);
        assertEquals(3500.0, r.takeHomePerMonth(), 0.001);
    }

    @Test
    void sameJobPaidAnnuallyOrSemimonthlyGivesTheSameMonthlyTakeHome() {
        PaycheckMath.Result yearly = PaycheckMath.compute("salary", "annually", 0, 0, 0, 1.5, 60000, 8, 100, 4, 14400);
        PaycheckMath.Result twice = PaycheckMath.compute("salary", "semimonthly", 0, 0, 0, 1.5, 2500, 8, 100, 4, 600);
        assertEquals(twice.takeHomePerMonth(), yearly.takeHomePerMonth(), 0.001);
        assertEquals(twice.grossPerYear(), yearly.grossPerYear(), 0.001);
        assertEquals(twice.employerMatchPerPeriod() * 24, yearly.employerMatchPerPeriod(), 0.001);
    }

    @Test
    void unknownFrequencyStillFallsBackToBiweekly() {
        assertEquals(26, PaycheckMath.periodsPerYear("every-full-moon"));
        assertEquals(26, PaycheckMath.periodsPerYear(null));
    }
}
