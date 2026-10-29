package com.breathingroom.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class GoalMathTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 11);

    @Test
    void monthsLeftCountsPartialMonths() {
        assertEquals(2, GoalMath.monthsLeft(TODAY, LocalDate.of(2026, 12, 11)));
        assertEquals(3, GoalMath.monthsLeft(TODAY, LocalDate.of(2026, 12, 20)));
        assertEquals(0, GoalMath.monthsLeft(TODAY, TODAY));
        assertEquals(0, GoalMath.monthsLeft(TODAY, null));
    }

    @Test
    void requiredMonthlyIsRemainingDividedByMonths() {
        assertEquals(350.00, GoalMath.requiredMonthly(6000, 3200, 8), 0.001);
        assertEquals(0.0, GoalMath.requiredMonthly(1000, 1000, 5), 0.001);
    }

    @Test
    void affordableGoalOffersRequiredOption() {
        List<GoalMath.Option> opts = GoalMath.options(6000, 3200, LocalDate.of(2027, 6, 11), 100, 500, TODAY);
        assertEquals("REQUIRED", GoalMath.defaultChoice(opts));
        assertEquals(350.00, opts.get(0).monthlyAmount(), 0.001);
    }

    @Test
    void unaffordableGoalOffersComfortablePaceWithLaterDate() {
        List<GoalMath.Option> opts = GoalMath.options(6000, 3200, LocalDate.of(2027, 2, 11), 0, 400, TODAY);
        assertEquals("COMFORTABLE", GoalMath.defaultChoice(opts));
        GoalMath.Option comfortable = opts.get(0);
        assertEquals(300.0, comfortable.monthlyAmount(), 0.001);
        assertTrue(LocalDate.parse(comfortable.newTargetDate()).isAfter(LocalDate.of(2027, 2, 11)));
    }

    @Test
    void completedGoalOnlyHasKeep() {
        List<GoalMath.Option> opts = GoalMath.options(1000, 1000, LocalDate.of(2027, 1, 1), 0, 500, TODAY);
        assertEquals(1, opts.size());
        assertEquals("KEEP", opts.get(0).id());
    }
}
