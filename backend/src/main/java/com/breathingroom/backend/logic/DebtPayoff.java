package com.breathingroom.backend.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Month-by-month debt payoff simulator (task 6.5, 6.6, 6.7). No Spring, no database, so it is easy to test.
 * Rules: every debt gets its minimum each month; all the extra money (your extra payment plus the minimums of debts already paid off)
 * goes to the first unpaid debt in the strategy's order. Interest is charged monthly at APR / 12.
 *  - AVALANCHE: highest APR first.   - SNOWBALL: smallest balance first.
 */
public final class DebtPayoff {

    public enum Strategy { AVALANCHE, SNOWBALL }

    public record Debt(String name, double balance, double apr, double minPayment) {}

    public record Step(String name, int month) {}

    public record Result(String strategy, int months, boolean paidOff, double totalInterest, List<Step> order) {}

    public static final int MAX_MONTHS = 600; // 50 years
    private static final double EPS = 0.005;

    private DebtPayoff() {}

    private static final class Work {
        final String name; final double apr; final double min; double bal; boolean recorded;
        Work(Debt d) { name = d.name(); apr = d.apr(); min = d.minPayment(); bal = d.balance(); }
    }

    public static Result simulate(List<Debt> input, double extraPerMonth, Strategy strategy) {
        List<Work> debts = new ArrayList<>();
        for (Debt d : input) if (d.balance() > 0) debts.add(new Work(d));
        Comparator<Work> order = strategy == Strategy.AVALANCHE
                ? Comparator.comparingDouble((Work w) -> -w.apr)
                : Comparator.comparingDouble((Work w) -> w.bal);
        debts.sort(order); // List.sort is stable, so ties keep the original order

        double budget = Math.max(0, extraPerMonth);
        for (Work w : debts) budget += w.min;

        int months = 0;
        double totalInterest = 0;
        List<Step> steps = new ArrayList<>();
        while (months < MAX_MONTHS && debts.stream().anyMatch(w -> w.bal > EPS)) {
            months++;
            int n = debts.size();
            double[] due = new double[n];
            double[] minPaid = new double[n];
            double minTotal = 0;
            for (int i = 0; i < n; i++) {
                Work w = debts.get(i);
                double interest = w.bal * w.apr / 1200.0;
                totalInterest += interest;
                due[i] = w.bal + interest;
                minPaid[i] = Math.min(w.min, due[i]);
                minTotal += minPaid[i];
            }
            double pool = budget - minTotal;
            for (int i = 0; i < n; i++) {
                Work w = debts.get(i);
                double extra = Math.min(due[i] - minPaid[i], Math.max(0, pool));
                pool -= extra;
                w.bal = due[i] - minPaid[i] - extra;
                if (w.bal <= EPS && !w.recorded) { w.recorded = true; steps.add(new Step(w.name, months)); }
            }
        }
        boolean paidOff = debts.stream().noneMatch(w -> w.bal > EPS);
        return new Result(strategy.name(), months, paidOff, totalInterest, steps);
    }

    /** Each debt paid on its own with only its minimum: no extra money and no rolling freed minimums forward. */
    public static Result minimumsOnly(List<Debt> input) {
        int months = 0;
        double interest = 0;
        boolean all = true;
        List<Step> steps = new ArrayList<>();
        for (Debt d : input) {
            if (d.balance() <= 0) continue;
            Result r = simulate(List.of(d), 0, Strategy.AVALANCHE);
            months = Math.max(months, r.months());
            interest += r.totalInterest();
            all &= r.paidOff();
            if (r.paidOff()) steps.add(new Step(d.name(), r.months()));
        }
        steps.sort(Comparator.comparingInt(Step::month));
        return new Result("MINIMUMS_ONLY", months, all, interest, steps);
    }
}
