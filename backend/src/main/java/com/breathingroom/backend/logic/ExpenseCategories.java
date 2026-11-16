package com.breathingroom.backend.logic;

import java.util.List;
import java.util.Set;

/**
 * The expense categories the app offers. The browser shows the same list (frontend/src/pages/ExpenseForm.jsx).
 * "Shopping" and "Other" are kept so expenses saved before the list grew still count and still display.
 */
public final class ExpenseCategories {

    public static final List<String> OFFERED = List.of("Housing", "Utilities", "Groceries", "Dining", "Transportation", "Insurance", "Childcare",
            "Education", "Medical", "Entertainment", "Subscriptions", "Savings", "Travel", "Household", "Miscellaneous");

    /** Older names that are still accepted and displayed. */
    public static final List<String> LEGACY = List.of("Shopping", "Other");

    /** Money you set aside rather than spend: counted as saving, not as a "want", in the health check. */
    public static final String SAVINGS = "Savings";

    private static final Set<String> ALLOWED = Set.copyOf(java.util.stream.Stream.concat(OFFERED.stream(), LEGACY.stream()).toList());

    private ExpenseCategories() {}

    public static boolean isKnown(String category) {
        return category != null && ALLOWED.contains(category.trim());
    }
}
