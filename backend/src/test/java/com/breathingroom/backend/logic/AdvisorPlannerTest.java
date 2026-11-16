package com.breathingroom.backend.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.breathingroom.backend.logic.AdvisorPlanner.*;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AdvisorPlannerTest {

    private static Snapshot snap(double surplus, double contribution, double match, List<DebtLine> debts) {
        return new Snapshot(true, 2000, 26, 4000, 2500, 300, surplus, contribution, match,
                Map.of("Dining", 300.0, "Groceries", 500.0, "Subscriptions", 50.0), debts, 500, 2000);
    }

    private static final List<DebtLine> CARD = List.of(new DebtLine("Visa", 5000, 24.0, 150), new DebtLine("Car", 9000, 6.0, 250));

    @Test
    void detectsIntent() {
        assertEquals(Intent.RETIREMENT, AdvisorPlanner.detect("Is my 401(k) too high?"));
        assertEquals(Intent.DEBT, AdvisorPlanner.detect("Should I pay off my credit card first?"));
        assertEquals(Intent.SAVINGS, AdvisorPlanner.detect("Can I afford to save more?"));
        assertEquals(Intent.SPENDING, AdvisorPlanner.detect("Where can I cut my budget?"));
        assertEquals(Intent.GENERAL, AdvisorPlanner.detect("hello"));
        assertEquals(Intent.GENERAL, AdvisorPlanner.detect(null));
    }

    @Test
    void debtPlanOffersOptionsWithTradeoffs() {
        Plan p = AdvisorPlanner.plan(Intent.DEBT, snap(600, 10, 5, CARD));
        assertEquals(3, p.choices().size());
        assertTrue(p.headline().contains("Visa"));
        for (Choice c : p.choices()) { assertFalse(c.tradeoff().isBlank()); assertFalse(c.detail().isBlank()); }
        assertTrue(p.choices().get(1).label().contains("$300.00"), "half of the $600 surplus");
        assertTrue(p.choices().get(1).detail().contains("saving about"));
    }

    @Test
    void cardAboveTheAverageGetsALowerRateOption() {
        Plan p = AdvisorPlanner.plan(Intent.DEBT, snap(600, 10, 5, CARD));
        Choice c = p.choices().get(0);
        assertTrue(c.label().contains("lower rate on Visa"));
        assertTrue(c.detail().contains("22.4%"), "the Federal Reserve average");
        assertTrue(p.facts().get("Debt 1").contains("percentage points above"));
        assertTrue(p.headline().contains("above the 22.4% average"));
    }

    @Test
    void noSurplusStillGivesUsefulOptions() {
        Plan p = AdvisorPlanner.plan(Intent.DEBT, snap(-100, 10, 5, CARD));
        assertEquals(3, p.choices().size());
        assertTrue(p.headline().contains("no free money"));
        assertTrue(p.choices().get(0).label().contains("lower rate"));
        assertTrue(p.choices().get(1).label().contains("Trim flexible spending by 10%"));
        assertTrue(p.choices().get(2).label().contains("minimums"));
    }

    @Test
    void lowRateDebtGetsNoRateNegotiationOption() {
        Plan p = AdvisorPlanner.plan(Intent.DEBT, snap(600, 10, 5, List.of(new DebtLine("Car", 9000, 6.0, 250))));
        assertFalse(p.choices().get(0).label().contains("lower rate"));
        assertTrue(p.headline().contains("not above"));
    }

    @Test
    void noDebtsSaysSo() {
        Plan p = AdvisorPlanner.plan(Intent.DEBT, snap(600, 10, 5, List.of()));
        assertTrue(p.choices().isEmpty());
        assertTrue(p.headline().contains("no debts"));
    }

    @Test
    void retirementAboveMatchComparesThreePaths() {
        Plan p = AdvisorPlanner.plan(Intent.RETIREMENT, snap(600, 10, 5, CARD));
        assertEquals(3, p.choices().size());
        assertTrue(p.facts().containsKey("Employer match applies up to"));
        assertTrue(p.choices().get(2).label().contains("5.0%"));
    }

    @Test
    void retirementBelowMatchSuggestsCapturingIt() {
        Plan p = AdvisorPlanner.plan(Intent.RETIREMENT, snap(600, 2, 5, CARD));
        assertTrue(p.choices().get(0).label().contains("full match"));
    }

    @Test
    void retirementWithoutMatchExplains() {
        Plan p = AdvisorPlanner.plan(Intent.RETIREMENT, snap(600, 10, 0, CARD));
        assertTrue(p.choices().isEmpty());
        assertTrue(p.headline().contains("No employer match"));
    }

    @Test
    void savingsPlanComparesPaces() {
        Plan p = AdvisorPlanner.plan(Intent.SAVINGS, snap(600, 10, 5, CARD));
        assertEquals(3, p.choices().size());
        assertTrue(p.facts().containsKey("A 3-month cushion would be"));
    }

    @Test
    void spendingPlanTrimsFlexibleCategoriesOnly() {
        Plan p = AdvisorPlanner.plan(Intent.SPENDING, snap(600, 10, 5, CARD));
        assertEquals(3, p.choices().size());
        assertTrue(p.choices().get(0).detail().contains("$35.00"), "10% of dining 300 + subscriptions 50");
        assertTrue(p.choices().get(2).label().contains("Dining"));
    }

    @Test
    void missingPaycheckAsksForIt() {
        Snapshot none = new Snapshot(false, 0, 26, 0, 0, 0, 0, 0, 0, Map.of(), List.of(), 0, 0);
        for (Intent i : Intent.values()) {
            Plan p = AdvisorPlanner.plan(i, none);
            assertTrue(p.choices().isEmpty());
            assertFalse(p.headline().isBlank());
        }
    }

    @Test
    void everyNumberInChoicesIsAllowedByTheGuards() {
        Plan p = AdvisorPlanner.plan(Intent.DEBT, snap(600, 10, 5, CARD));
        String echoed = p.choices().get(1).label() + " " + p.choices().get(1).detail();
        assertFalse(NumberGuard.containsUnknownNumber(echoed, p.allowedSources()));
        assertTrue(NumberGuard.containsUnknownNumber("You could save $9,999.", p.allowedSources()));
    }
}
