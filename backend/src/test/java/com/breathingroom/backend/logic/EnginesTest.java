package com.breathingroom.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Expected numbers were calculated independently in Python and match the Paycheck-to-Plan spreadsheet. */
class EnginesTest {

    private static final List<DebtPayoff.Debt> EXAMPLE = List.of(
            new DebtPayoff.Debt("Medical bill", 800, 0, 40),
            new DebtPayoff.Debt("Credit card", 4200, 24, 90),
            new DebtPayoff.Debt("Car loan", 9800, 7, 220));

    @Test
    void avalancheMatchesIndependentCalculation() {
        DebtPayoff.Result r = DebtPayoff.simulate(EXAMPLE, 100, DebtPayoff.Strategy.AVALANCHE);
        assertEquals(40, r.months());
        assertEquals(2761.79, r.totalInterest(), 0.01);
        assertTrue(r.paidOff());
        assertEquals("Medical bill", r.order().get(0).name());
        assertEquals(20, r.order().get(0).month());
    }

    @Test
    void snowballPaysSmallestBalanceFirst() {
        DebtPayoff.Result r = DebtPayoff.simulate(EXAMPLE, 100, DebtPayoff.Strategy.SNOWBALL);
        assertEquals(40, r.months());
        assertEquals(2939.86, r.totalInterest(), 0.01);
        assertEquals(6, r.order().get(0).month());
    }

    @Test
    void minimumsOnlyCostsMoreThanAnyExtraPayment() {
        DebtPayoff.Result min = DebtPayoff.minimumsOnly(EXAMPLE);
        assertEquals(137, min.months());
        assertEquals(9689.29, min.totalInterest(), 0.01);
    }

    @Test
    void debtThatNeverPaysOffIsReported() {
        List<DebtPayoff.Debt> never = List.of(new DebtPayoff.Debt("Card", 5000, 30, 50));
        assertFalse(DebtPayoff.simulate(never, 0, DebtPayoff.Strategy.AVALANCHE).paidOff());
        assertFalse(DebtPayoff.minimumsOnly(never).paidOff());
    }

    @Test
    void ties_keepEnteredOrder() {
        List<DebtPayoff.Debt> tie = List.of(new DebtPayoff.Debt("A", 1000, 20, 50), new DebtPayoff.Debt("B", 1000, 20, 50), new DebtPayoff.Debt("C", 1000, 20, 50));
        DebtPayoff.Result r = DebtPayoff.simulate(tie, 75, DebtPayoff.Strategy.AVALANCHE);
        assertEquals(16, r.months());
        assertEquals(421.24, r.totalInterest(), 0.01);
        assertEquals("A", r.order().get(0).name());
    }

    @Test
    void scenarioCompareCurrentVersusLoweredContribution() {
        List<DebtPayoff.Debt> app = List.of(new DebtPayoff.Debt("Store card", 600, 26, 25), new DebtPayoff.Debt("Credit card", 4200, 24, 90),
                new DebtPayoff.Debt("Car loan", 9800, 7, 220));
        ScenarioMath.Scenario s = ScenarioMath.compute(2150, 26, 9, 6, 100, app);
        assertTrue(s.applicable());
        assertEquals(139.75, s.freedGrossPerMonth(), 0.01);
        assertEquals(63, s.current().months());
        assertEquals(37, s.alternative().months());
        assertEquals(26, s.monthsSaved());
        assertEquals(3646.10, s.interestSaved(), 0.01);
        assertEquals(5170.75, s.retirementGivenUp(), 0.01);
        assertEquals(40, ScenarioMath.compute(2150, 26, 9, 6, 80, app).alternative().months()); // keeping only 80% after tax
    }

    @Test
    void scenarioNotApplicableWhenNothingAboveTheMatch() {
        assertFalse(ScenarioMath.compute(2150, 26, 6, 6, 100, EXAMPLE).applicable());
        assertFalse(ScenarioMath.compute(2150, 26, 9, 0, 100, EXAMPLE).applicable());
        assertFalse(ScenarioMath.compute(2150, 26, 9, 6, 100, List.of()).applicable());
    }

    @Test
    void takeHomePay() {
        PaycheckMath.Result p = PaycheckMath.compute("salary", "biweekly", 0, 0, 0, 1.5, 2150, 9, 50, 6, 524.48);
        assertEquals(1432.02, p.takeHomePerPeriod(), 0.01);
        assertEquals(64.50, p.employerMatchPerPeriod(), 0.01);
        assertEquals(3102.71, p.takeHomePerMonth(), 0.01);
        PaycheckMath.Result h = PaycheckMath.compute("hourly", "weekly", 20, 30, 5, 1.5, 0, 5, 100, 3, 100);
        assertEquals(750.0, h.grossPerPeriod(), 0.001);
        assertEquals(612.50, h.takeHomePerPeriod(), 0.01);
    }
}
