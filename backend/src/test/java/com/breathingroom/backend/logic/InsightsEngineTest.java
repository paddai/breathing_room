package com.breathingroom.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/** The sample household from the app's demo data. Expected numbers were calculated independently in Python. */
class InsightsEngineTest {

    private static InsightsEngine.Facts sample(double contribution) {
        Map<String, Double> spending = new LinkedHashMap<>();
        spending.put("Housing", 1150.0);
        spending.put("Utilities", 167.30);
        spending.put("Groceries", 280.65);
        spending.put("Transportation", 58.0);
        spending.put("Dining", 62.5);
        spending.put("Shopping", 74.0);
        List<DebtPayoff.Debt> debts = List.of(new DebtPayoff.Debt("Store card", 600, 26, 25), new DebtPayoff.Debt("Credit card", 4200, 24, 90),
                new DebtPayoff.Debt("Car loan", 9800, 7, 220));
        List<InsightsEngine.GoalFact> goals = List.of(new InsightsEngine.GoalFact("Emergency fund", 6000, 3200, 8, 100), new InsightsEngine.GoalFact("Family trip", 1800, 350, 6, 0));
        return new InsightsEngine.Facts(2150, 26, 3102.71, contribution, 6, 50, spending, 1792.45, 335, debts, goals);
    }

    private static InsightsEngine.Insight find(List<InsightsEngine.Insight> all, String id) {
        Optional<InsightsEngine.Insight> i = all.stream().filter(x -> x.id().equals(id)).findFirst();
        assertTrue(i.isPresent(), "missing insight " + id);
        return i.get();
    }

    @Test
    void retirementAndMatchNumbers() {
        List<InsightsEngine.Insight> all = InsightsEngine.analyze(sample(9));
        assertTrue(find(all, "retirement_pace").title().contains("$5,031"));
        assertTrue(find(all, "retirement_pace").detail().contains("20.5%"));
        assertEquals("GOOD", find(all, "match_captured").severity());
        assertTrue(find(all, "match_captured").detail().contains("$1,677"));
        assertEquals("GOOD", find(all, "retirement_rate").severity());
        assertTrue(find(all, "retirement_rate").detail().contains("12.0%"));
    }

    @Test
    void contributionAboveTheIrsLimitIsAnAlertAndMissingMatchIsFlagged() {
        assertFalse(InsightsEngine.analyze(sample(30)).stream().anyMatch(i -> i.id().equals("retirement_limit"))); // 2150 x 26 x 30% = $16,770 is under the limit
        InsightsEngine.Facts rich = new InsightsEngine.Facts(5000, 26, 6000, 20, 6, 50, Map.of(), 0, 0, List.of(), List.of());
        InsightsEngine.Insight limit = find(InsightsEngine.analyze(rich), "retirement_limit"); // 5000 x 26 x 20% = $26,000 > $24,500
        assertEquals("ALERT", limit.severity());
        assertTrue(limit.detail().contains("$26,000") && limit.detail().contains("$24,500"));
        InsightsEngine.Insight gap = find(InsightsEngine.analyze(sample(2)), "match_gap");
        assertEquals("WATCH", gap.severity());
        assertTrue(gap.detail().contains("$1,118")); // 2150 x 26 x 4% x 50%
    }

    @Test
    void debtInsights() {
        List<InsightsEngine.Insight> all = InsightsEngine.analyze(sample(9));
        InsightsEngine.Insight above = find(all, "debt_above_average");
        assertEquals("WATCH", above.severity());
        assertTrue(above.title().startsWith("2 debts"));
        assertTrue(above.detail().contains("$91") && above.detail().contains("22.36%")); // 600 x 3.64% + 4200 x 1.64%
        assertTrue(find(all, "interest_cost").title().contains("$154"));
        assertEquals("GOOD", find(all, "debt_to_income").severity()); // 335 / 4,658.33 = 7.2%
        assertTrue(find(all, "debt_to_income").title().contains("7.2%"));
    }

    @Test
    void budgetHousingAndEmergencyFund() {
        List<InsightsEngine.Insight> all = InsightsEngine.analyze(sample(9));
        assertEquals("GOOD", find(all, "housing_burden").severity()); // 1,317.30 / 4,658.33 = 28.3%
        assertTrue(find(all, "housing_burden").title().contains("28.3%"));
        assertEquals("WATCH", find(all, "spending_mix").severity());
        assertTrue(find(all, "spending_mix").detail().contains("73.5%"));
        InsightsEngine.Insight budget = find(all, "budget_503020");
        assertEquals("WATCH", budget.severity());
        assertTrue(budget.title().contains("64.2%"));
        assertTrue(budget.detail().contains("31.4%"));
        InsightsEngine.Insight fund = find(all, "emergency_cover");
        assertEquals("INFO", fund.severity());
        assertTrue(fund.title().contains("1.6 months"));
        assertTrue(fund.detail().contains("$5,973") && fund.detail().contains("$11,946") && fund.detail().contains("3 to 6 months"));
    }

    @Test
    void goalsAreAdjustedForRecentInflation() {
        List<InsightsEngine.Insight> all = InsightsEngine.analyze(sample(9));
        InsightsEngine.Insight g1 = find(all, "goal_inflation_1");
        assertTrue(g1.detail().contains("$6,147") && g1.detail().contains("$147 more") && g1.detail().contains("$368 a month instead of $350"));
        assertTrue(find(all, "goal_inflation_2").detail().contains("$1,833"));
        assertEquals(6147.10, InsightsEngine.inflate(6000, 3.7, 8), 0.01);
    }

    @Test
    void noEmergencyGoalSuggestsOneAndEmptyDataGivesNothingToCompare() {
        InsightsEngine.Facts f = sample(9);
        InsightsEngine.Facts noGoals = new InsightsEngine.Facts(f.grossPerPeriod(), 26, f.monthlyTakeHome(), 9, 6, 50, f.spendingByCategory(), f.monthlyExpenses(), 335, f.debts(), List.of());
        assertEquals("INFO", find(InsightsEngine.analyze(noGoals), "emergency_none").severity());
        assertTrue(InsightsEngine.analyze(new InsightsEngine.Facts(0, 26, 0, 0, 0, 0, Map.of(), 0, 0, List.of(), List.of())).isEmpty());
    }

    @Test
    void mostUrgentInsightsComeFirst() {
        List<InsightsEngine.Insight> all = InsightsEngine.analyze(sample(9));
        int lastRank = -1;
        for (InsightsEngine.Insight i : all) {
            int rank = List.of("ALERT", "WATCH", "INFO", "GOOD").indexOf(i.severity());
            assertTrue(rank >= lastRank);
            lastRank = rank;
        }
        assertFalse(all.isEmpty());
    }
}
