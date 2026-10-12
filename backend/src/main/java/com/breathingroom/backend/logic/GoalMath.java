package com.breathingroom.backend.logic;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Pure goal math. No Spring, no database, no AI - so it is easy to test.
 * Every number the Advisor shows for a savings goal comes from here.
 */
public final class GoalMath {

    private GoalMath() {}

    /** One choice the user can approve. The AI may only pick or explain these - it never invents them. */
    public record Option(String id, String label, double monthlyAmount, String newTargetDate, String projection) {}

    public static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    /** Whole months from today until the target date. 0 means "no future deadline" (today, past, or missing). */
    public static int monthsLeft(LocalDate today, LocalDate target) {
        if (target == null || !target.isAfter(today)) return 0;
        long months = ChronoUnit.MONTHS.between(today, target);
        if (today.plusMonths(months).isBefore(target)) months++; // a partial month still counts
        return (int) Math.max(1, months);
    }

    public static double requiredMonthly(double target, double saved, int monthsLeft) {
        double remaining = Math.max(0, target - saved);
        if (remaining <= 0) return 0;
        return round2(remaining / Math.max(1, monthsLeft));
    }

    public static String money(double v) {
        return String.format(Locale.US, "$%,.2f", v);
    }

    public static List<Option> options(double target, double saved, LocalDate targetDate,
                                       double currentMonthly, double availableForGoal, LocalDate today) {
        List<Option> out = new ArrayList<>();
        double remaining = Math.max(0, target - saved);
        String dateText = targetDate == null ? null : targetDate.toString();

        if (remaining <= 0) {
            out.add(new Option("KEEP", "Goal reached - nothing to change", 0, dateText, "This goal is fully funded."));
            return out;
        }

        int months = monthsLeft(today, targetDate);
        double required = requiredMonthly(target, saved, months);
        double available = round2(Math.max(0, availableForGoal));

        // Option 1: hit the original date (only if there is a future date and the budget can afford it)
        if (months > 0 && required > 0 && required <= available) {
            out.add(new Option("REQUIRED",
                    "Save " + money(required) + " per month to reach it by " + targetDate,
                    required, dateText,
                    "Reaches the goal on your original date."));
        }

        // Option 2: a comfortable pace that keeps a 25% cushion, with a later date
        double comfortable = Math.floor(available * 0.75);
        boolean needed = months == 0 || required > available || comfortable < required;
        if (comfortable > 0 && needed) {
            int needMonths = (int) Math.ceil(remaining / comfortable);
            LocalDate newDate = today.plusMonths(needMonths);
            out.add(new Option("COMFORTABLE",
                    "Save " + money(comfortable) + " per month and finish around " + newDate,
                    comfortable, newDate.toString(),
                    "Keeps about 25% of your free monthly money as a cushion."));
        }

        // Option 3: change nothing
        String projection;
        if (currentMonthly > 0) {
            int m = (int) Math.ceil(remaining / currentMonthly);
            projection = "At " + money(currentMonthly) + " per month you would finish around " + today.plusMonths(m) + ".";
        } else {
            projection = "No monthly amount is set yet, so there is no finish date.";
        }
        out.add(new Option("KEEP", "Keep your current plan", round2(currentMonthly), dateText, projection));
        return out;
    }

    /** The deterministic default: the first of REQUIRED, COMFORTABLE, KEEP that exists. */
    public static String defaultChoice(List<Option> options) {
        for (String id : new String[]{"REQUIRED", "COMFORTABLE", "KEEP"}) {
            for (Option o : options) if (o.id().equals(id)) return id;
        }
        return options.isEmpty() ? null : options.get(0).id();
    }
}
