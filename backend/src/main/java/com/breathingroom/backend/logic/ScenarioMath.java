package com.breathingroom.backend.logic;

import java.util.Comparator;
import java.util.List;

/**
 * "What if I lowered my 401(k) to my employer's match and put the difference toward debt?" (tasks 7.3, 7.4, 7.5).
 * Current plan = keep contributing as you do now.  Alternative = contribute only up to the match limit and send the freed
 * money to your debts (highest APR first). The employer match is not affected, because you still contribute up to the limit.
 * Everything is an estimate: it ignores investment growth you would give up and any tax on the freed money (use the after-tax input).
 */
public final class ScenarioMath {

    private ScenarioMath() {}

    public record Scenario(boolean applicable, String note, double freedGrossPerMonth, double extraToDebtPerMonth, double afterTaxPercent,
                           String targetDebt, double targetApr, DebtPayoff.Result current, DebtPayoff.Result alternative,
                           int monthsSaved, double interestSaved, double retirementGivenUp, String caution) {}

    private static Scenario no(String note) {
        return new Scenario(false, note, 0, 0, 100, "", 0, null, null, 0, 0, 0, "");
    }

    public static Scenario compute(double grossPerPeriod, int periodsPerYear, double contributionPercent, double matchLimitPercent,
                                   double afterTaxPercent, List<DebtPayoff.Debt> debts) {
        if (grossPerPeriod <= 0) return no("Enter your paycheck first.");
        if (matchLimitPercent <= 0) return no("Add your employer match on the Paycheck page. This comparison needs a match to compare against.");
        double above = Math.max(0, contributionPercent - matchLimitPercent);
        if (above <= 0) return no("Your 401(k) contribution is not above your employer's match, so there is nothing to redirect.");
        List<DebtPayoff.Debt> active = debts.stream().filter(d -> d.balance() > 0).toList();
        if (active.isEmpty()) return no("Add your debts first. The comparison shows how redirecting money changes your payoff.");

        double keep = Math.max(0, Math.min(100, afterTaxPercent));
        double freed = grossPerPeriod * above / 100.0 * periodsPerYear / 12.0;
        double extra = freed * keep / 100.0;
        DebtPayoff.Debt target = active.stream().max(Comparator.comparingDouble(DebtPayoff.Debt::apr)).orElseThrow();

        DebtPayoff.Result current = DebtPayoff.simulate(active, 0, DebtPayoff.Strategy.AVALANCHE);
        DebtPayoff.Result alt = DebtPayoff.simulate(active, extra, DebtPayoff.Strategy.AVALANCHE);
        boolean comparable = current.paidOff() && alt.paidOff();
        int monthsSaved = comparable ? current.months() - alt.months() : 0;
        double interestSaved = comparable ? current.totalInterest() - alt.totalInterest() : 0;
        double giveUp = freed * alt.months();

        String note = comparable ? "" : (alt.paidOff()
                ? "With your current payments alone your debts are not paid off within 50 years. The alternative pays them off."
                : "Neither plan pays off all your debts within 50 years at these amounts.");
        String caution = "Educational estimate. It ignores the investment growth you would give up, taxes on the freed money, and changes to your debts. Not financial advice.";
        if (target.apr() <= 7.0) caution = "Your highest interest rate is only " + String.format("%.1f", target.apr())
                + "%, which a long-term retirement account may beat over time. " + caution;
        return new Scenario(true, note, freed, extra, keep, target.name(), target.apr(), current, alt, monthsSaved, interestSaved, giveUp, caution);
    }
}
