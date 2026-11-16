package com.breathingroom.backend.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExpenseCategoriesTest {

    @Test
    void everyRequestedCategoryIsOffered() {
        for (String c : List.of("Housing", "Utilities", "Groceries", "Dining", "Transportation", "Insurance", "Childcare", "Education",
                "Medical", "Entertainment", "Subscriptions", "Savings", "Travel", "Household", "Miscellaneous")) {
            assertTrue(ExpenseCategories.OFFERED.contains(c), "missing " + c);
            assertTrue(ExpenseCategories.isKnown(c), "not accepted " + c);
        }
        assertEquals(15, ExpenseCategories.OFFERED.size());
    }

    @Test
    void olderCategoriesAreStillAccepted() {
        assertTrue(ExpenseCategories.isKnown("Shopping"));
        assertTrue(ExpenseCategories.isKnown("Other"));
    }

    @Test
    void unknownOrBlankCategoriesAreRejected() {
        assertFalse(ExpenseCategories.isKnown("Gambling"));
        assertFalse(ExpenseCategories.isKnown(""));
        assertFalse(ExpenseCategories.isKnown(null));
    }

    @Test
    void savingsAreNotCountedAsWantsInTheBudgetGuideline() {
        Map<String, Double> spending = new LinkedHashMap<>();
        spending.put("Housing", 1000.0);
        spending.put("Dining", 200.0);
        InsightsEngine.Facts base = new InsightsEngine.Facts(2000, 26, 4000, 0, 0, 0, spending, 1200, 0, List.of(), List.of());
        Map<String, Double> withSavings = new LinkedHashMap<>(spending);
        withSavings.put("Savings", 300.0);
        InsightsEngine.Facts saving = new InsightsEngine.Facts(2000, 26, 4000, 0, 0, 0, withSavings, 1500, 0, List.of(), List.of());
        String a = find(InsightsEngine.analyze(base));
        String b = find(InsightsEngine.analyze(saving));
        // wants stay at $200 (5.0% of take-home) because the $300 is saving
        assertTrue(a.contains("Wants are 5.0%"), a);
        assertTrue(b.contains("Wants are 5.0%"), b);
        assertTrue(a.contains("is 70.0%") && b.contains("is 70.0%"), b); // what is left for savings does not shrink because you saved
    }

    private static String find(List<InsightsEngine.Insight> all) {
        return all.stream().filter(i -> i.id().equals("budget_503020")).findFirst().orElseThrow().detail();
    }
}
