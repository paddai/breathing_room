package com.breathingroom.backend.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The "financial health check": rules that compare what you entered with published benchmarks (see Benchmarks).
 * Every insight is calculated here in Java, carries the ids of the benchmarks it used, and has a plain-language explanation.
 * No AI is involved; the AI may later explain these results but cannot change them.
 * Severity: ALERT (needs attention now), WATCH (worth a look), INFO (good to know), GOOD (on track).
 */
public final class InsightsEngine {

    public record GoalFact(String name, double target, double saved, int monthsLeft, double monthlyContribution) {}

    public record Facts(double grossPerPeriod, int periodsPerYear, double monthlyTakeHome, double contributionPercent, double matchLimitPercent,
                        double matchRatePercent, Map<String, Double> spendingByCategory, double monthlyExpenses, double monthlyDebtMinimums,
                        List<DebtPayoff.Debt> debts, List<GoalFact> goals) {}

    public record Insight(String id, String severity, String title, String detail, List<String> sources) {}

    private static final Set<String> NEEDS = Set.of("Housing", "Utilities", "Groceries", "Transportation", "Insurance", "Childcare", "Medical");
    private static final Map<String, Integer> RANK = Map.of("ALERT", 0, "WATCH", 1, "INFO", 2, "GOOD", 3);

    private InsightsEngine() {}

    private static double b(String id) { return Benchmarks.value(id); }
    private static String m0(double v) { return String.format(Locale.US, "$%,.0f", v); }
    private static String pc(double v) { return String.format(Locale.US, "%.1f%%", v); }
    private static String pc2(double v) { return String.format(Locale.US, "%.2f%%", v); }
    private static String mo(double v) { return String.format(Locale.US, "%.1f", v); }
    private static double spent(Map<String, Double> s, String category) { return s.getOrDefault(category, 0.0); }

    public static double inflate(double amount, double annualPercent, int months) {
        return amount * Math.pow(1 + annualPercent / 100.0, months / 12.0);
    }

    public static List<Insight> analyze(Facts f) {
        List<Insight> out = new ArrayList<>();
        retirementLimit(f, out);
        matchGap(f, out);
        retirementRate(f, out);
        debtRates(f, out);
        interestCost(f, out);
        debtToIncome(f, out);
        housing(f, out);
        spendingMix(f, out);
        budget503020(f, out);
        emergencyFund(f, out);
        goalInflation(f, out);
        out.sort(Comparator.comparingInt(i -> RANK.get(i.severity()))); // stable: same severity keeps the order above
        return out;
    }

    private static double grossMonthly(Facts f) { return f.grossPerPeriod() * f.periodsPerYear() / 12.0; }

    private static double needsSpending(Facts f) {
        double needs = 0;
        for (Map.Entry<String, Double> e : f.spendingByCategory().entrySet()) if (NEEDS.contains(e.getKey())) needs += e.getValue();
        return needs;
    }

    // ---------------------------------------------------------------- retirement
    private static void retirementLimit(Facts f, List<Insight> out) {
        if (f.grossPerPeriod() <= 0 || f.contributionPercent() <= 0) return;
        double annual = f.grossPerPeriod() * f.periodsPerYear() * f.contributionPercent() / 100.0;
        double limit = b("irs_401k_limit");
        if (annual > limit) {
            out.add(new Insight("retirement_limit", "ALERT", "Your 401(k) is set above the IRS yearly limit",
                    "At " + pc(f.contributionPercent()) + " of pay you are on pace to put about " + m0(annual) + " into your 401(k) this year. The 2026 IRS limit is "
                            + m0(limit) + " (people 50 or older can add a catch-up of " + m0(b("irs_401k_catchup")) + "). Ask payroll to cap it so nothing has to be returned.",
                    List.of("irs_401k_limit", "irs_401k_catchup")));
        } else {
            out.add(new Insight("retirement_pace", "INFO", "Your 401(k) is on pace for " + m0(annual) + " this year",
                    "That is " + pc(annual / limit * 100) + " of the 2026 IRS limit of " + m0(limit) + ".", List.of("irs_401k_limit")));
        }
    }

    private static void matchGap(Facts f, List<Insight> out) {
        if (f.grossPerPeriod() <= 0 || f.matchLimitPercent() <= 0 || f.matchRatePercent() <= 0) return;
        double perYear = f.grossPerPeriod() * f.periodsPerYear();
        if (f.contributionPercent() < f.matchLimitPercent()) {
            double missing = f.matchLimitPercent() - f.contributionPercent();
            double lost = perYear * missing / 100.0 * f.matchRatePercent() / 100.0;
            double cost = f.grossPerPeriod() * missing / 100.0;
            out.add(new Insight("match_gap", f.contributionPercent() <= 0 ? "ALERT" : "WATCH", "You are leaving employer match unclaimed",
                    "Your employer matches up to " + pc(f.matchLimitPercent()) + " of pay. You contribute " + pc(f.contributionPercent()) + ", so about " + m0(lost)
                            + " a year of match goes unclaimed. Raising your contribution by " + pc(missing) + " would cost about " + m0(cost) + " per paycheck before taxes and unlock it.",
                    List.of()));
        } else {
            double match = perYear * f.matchLimitPercent() / 100.0 * f.matchRatePercent() / 100.0;
            out.add(new Insight("match_captured", "GOOD", "You capture your full employer match",
                    "At " + pc(f.contributionPercent()) + " you meet your employer's " + pc(f.matchLimitPercent()) + " match limit, worth about " + m0(match) + " a year.", List.of()));
        }
    }

    private static void retirementRate(Facts f, List<Insight> out) {
        if (f.grossPerPeriod() <= 0) return;
        double matched = f.matchLimitPercent() > 0 ? Math.min(f.contributionPercent(), f.matchLimitPercent()) * f.matchRatePercent() / 100.0 : 0;
        double total = f.contributionPercent() + matched;
        double low = b("vanguard_target_low");
        List<String> src = List.of("vanguard_total_rate", "vanguard_target_low", "vanguard_target_high");
        if (total >= low) {
            out.add(new Insight("retirement_rate", "GOOD", "Your total retirement saving rate looks solid",
                    "Your contribution of " + pc(f.contributionPercent()) + " plus employer match of " + pc(matched) + " is about " + pc(total) + " of pay. Vanguard's 2026 report suggests "
                            + pc(low) + " to " + pc(b("vanguard_target_high")) + " and found the average total was " + pc(b("vanguard_total_rate")) + ".", src));
        } else {
            out.add(new Insight("retirement_rate", "INFO", "Your total retirement saving rate is below the usual target",
                    "Your contribution of " + pc(f.contributionPercent()) + " plus employer match of " + pc(matched) + " is about " + pc(total) + " of pay. Vanguard's 2026 report suggests "
                            + pc(low) + " to " + pc(b("vanguard_target_high")) + " and found the average total was " + pc(b("vanguard_total_rate"))
                            + ". Debt payments may come first right now, and that is a reasonable choice.", src));
        }
    }

    // ---------------------------------------------------------------- debt
    private static List<DebtPayoff.Debt> activeDebts(Facts f) {
        return f.debts().stream().filter(d -> d.balance() > 0).toList();
    }

    private static void debtRates(Facts f, List<Insight> out) {
        List<DebtPayoff.Debt> debts = activeDebts(f);
        if (debts.isEmpty()) return;
        double avg = b("fed_card_apr");
        double assumed = b("assumed_return");
        List<DebtPayoff.Debt> above = debts.stream().filter(d -> d.apr() > avg).toList();
        if (!above.isEmpty()) {
            double saving = above.stream().mapToDouble(d -> d.balance() * (d.apr() - avg) / 100.0).sum();
            String names = String.join(" and ", above.stream().map(d -> d.name() + " (" + pc(d.apr()) + ")").toList());
            out.add(new Insight("debt_above_average", "WATCH", above.size() == 1 ? "One debt charges more than the average card rate" : above.size() + " debts charge more than the average card rate",
                    names + " " + (above.size() == 1 ? "is" : "are") + " above the national average of " + pc2(avg) + " for cards that carry a balance (Federal Reserve, August 2026). "
                            + "A rate at the average could save about " + m0(saving) + " a year on " + (above.size() == 1 ? "this balance" : "these balances")
                            + ". Calling the lender to ask for a lower rate, or comparing a credit-union loan, are options to explore.", List.of("fed_card_apr")));
            return;
        }
        DebtPayoff.Debt top = debts.stream().max(Comparator.comparingDouble(DebtPayoff.Debt::apr)).orElseThrow();
        if (top.apr() > assumed) {
            out.add(new Insight("debt_vs_investing", "INFO", "Paying down " + top.name() + " is a sure return of " + pc(top.apr()),
                    "Its rate of " + pc(top.apr()) + " is above the " + pc(assumed) + " long-term investing return this app assumes (an assumption, not a forecast), and below the average card rate of "
                            + pc2(avg) + ".", List.of("assumed_return", "fed_card_apr")));
        } else {
            out.add(new Insight("debt_rates_low", "GOOD", "Your debt rates are at or below " + pc(assumed),
                    "Your highest rate is " + pc(top.apr()) + ". Debt at low rates can usually be paid on schedule while you build savings.", List.of("assumed_return")));
        }
    }

    private static void interestCost(Facts f, List<Insight> out) {
        double monthly = activeDebts(f).stream().mapToDouble(d -> d.balance() * d.apr() / 1200.0).sum();
        if (monthly < 1) return;
        out.add(new Insight("interest_cost", "INFO", "Your debts cost about " + m0(monthly) + " a month in interest",
                "That is about " + m0(monthly * 12) + " a year at today's balances, before any payments reduce them.", List.of()));
    }

    private static void debtToIncome(Facts f, List<Insight> out) {
        double gm = grossMonthly(f);
        if (gm <= 0 || f.monthlyDebtMinimums() <= 0) return;
        double ratio = f.monthlyDebtMinimums() / gm * 100.0;
        String detail = "Your minimum debt payments are about " + m0(f.monthlyDebtMinimums()) + " a month, which is " + pc(ratio) + " of your gross income (not counting rent). Lenders generally look for "
                + pc(b("dti_lender")) + " or less, and the CFPB suggests renters aim for 15% to 20% or lower.";
        String severity = ratio > b("dti_lender") ? "ALERT" : ratio > b("dti_renter") ? "WATCH" : "GOOD";
        String title = ratio > b("dti_lender") ? "Debt payments take " + pc(ratio) + " of your gross income" : ratio > b("dti_renter") ? "Debt payments are on the high side at " + pc(ratio) + " of gross income"
                : "Debt payments are a manageable " + pc(ratio) + " of gross income";
        out.add(new Insight("debt_to_income", severity, title, detail, List.of("dti_lender", "dti_renter")));
    }

    // ---------------------------------------------------------------- budget
    private static void housing(Facts f, List<Insight> out) {
        double gm = grossMonthly(f);
        double cost = spent(f.spendingByCategory(), "Housing") + spent(f.spendingByCategory(), "Utilities");
        if (gm <= 0 || cost <= 0) return;
        double share = cost / gm * 100.0;
        String detail = "Housing and utilities are " + m0(cost) + " this month, " + pc(share) + " of your gross income. Federal housing programs call housing affordable at "
                + pc(b("hud_cost_burden")) + " or less, 'cost burdened' above that, and 'severely cost burdened' above " + pc(b("hud_severe")) + ". This uses the entries you have logged so far this month.";
        if (share > b("hud_severe")) out.add(new Insight("housing_burden", "ALERT", "Housing takes " + pc(share) + " of your gross income", detail, List.of("hud_cost_burden", "hud_severe")));
        else if (share > b("hud_cost_burden")) out.add(new Insight("housing_burden", "WATCH", "Housing takes " + pc(share) + " of your gross income", detail, List.of("hud_cost_burden", "hud_severe")));
        else out.add(new Insight("housing_burden", "GOOD", "Housing is within the usual affordability line at " + pc(share) + " of gross income", detail, List.of("hud_cost_burden", "hud_severe")));
    }

    private static void spendingMix(Facts f, List<Insight> out) {
        double total = f.monthlyExpenses();
        if (total < 200) return;
        double housing = (spent(f.spendingByCategory(), "Housing") + spent(f.spendingByCategory(), "Utilities")) / total * 100.0;
        double transport = spent(f.spendingByCategory(), "Transportation") / total * 100.0;
        double food = (spent(f.spendingByCategory(), "Groceries") + spent(f.spendingByCategory(), "Dining")) / total * 100.0;
        List<String> high = new ArrayList<>();
        if (housing > b("bls_housing_share") + 8) high.add("housing and utilities " + pc(housing) + " of your spending (national average " + pc(b("bls_housing_share")) + ")");
        if (transport > b("bls_transport_share") + 8) high.add("transportation " + pc(transport) + " (average " + pc(b("bls_transport_share")) + ")");
        if (food > b("bls_food_share") + 8) high.add("food " + pc(food) + " (average " + pc(b("bls_food_share")) + ")");
        List<String> src = List.of("bls_housing_share", "bls_transport_share", "bls_food_share");
        String note = " This is a rough comparison: the national shares include taxes and insurance, and your list only has what you have logged.";
        if (high.isEmpty()) out.add(new Insight("spending_mix", "INFO", "Your spending mix is close to the national average", "Housing, transportation and food are each within about 8 points of the BLS 2025 averages." + note, src));
        else out.add(new Insight("spending_mix", "WATCH", "Part of your spending is well above the national average", "Compared with BLS 2025 averages: " + String.join("; ", high) + "." + note, src));
    }

    private static void budget503020(Facts f, List<Insight> out) {
        double take = f.monthlyTakeHome();
        if (take <= 0 || f.monthlyExpenses() <= 0) return;
        double needs = needsSpending(f) + f.monthlyDebtMinimums();
        double wants = f.monthlyExpenses() - needsSpending(f);
        double savings = take - needs - wants;
        double needsPct = needs / take * 100.0, wantsPct = wants / take * 100.0, savingsPct = savings / take * 100.0;
        String detail = "Needs (housing, utilities, groceries, transportation and minimum debt payments) are " + pc(needsPct) + " of your take-home pay against a guideline of " + pc(b("rule_needs"))
                + ". Wants are " + pc(wantsPct) + " (guideline " + pc(b("rule_wants")) + "), and what is left for savings or extra debt payments is " + pc(savingsPct) + " (guideline " + pc(b("rule_savings")) + ").";
        List<String> src = List.of("rule_needs", "rule_wants", "rule_savings");
        if (savings < 0) out.add(new Insight("budget_503020", "ALERT", "You are spending more than you take home", detail, src));
        else if (needsPct > b("rule_needs")) out.add(new Insight("budget_503020", "WATCH", "Needs take " + pc(needsPct) + " of your take-home pay", detail, src));
        else out.add(new Insight("budget_503020", "GOOD", "Your budget fits the 50/30/20 guideline", detail, src));
    }

    private static void emergencyFund(Facts f, List<Insight> out) {
        double essential = needsSpending(f) + f.monthlyDebtMinimums();
        if (essential <= 0) return;
        double low = b("emergency_months_low"), high = b("emergency_months_high");
        List<String> src = List.of("emergency_months_low", "emergency_months_high", "shed_cover_400");
        GoalFact fund = f.goals().stream().filter(g -> g.name().toLowerCase(Locale.ROOT).contains("emergency")).findFirst().orElse(null);
        if (fund == null) {
            out.add(new Insight("emergency_none", "INFO", "Start an emergency fund",
                    "A common guideline is " + (int) low + " to " + (int) high + " months of essential costs, about " + m0(essential * low) + " to " + m0(essential * high) + " for you. The Federal Reserve found that only "
                            + pc(b("shed_cover_400")) + " of adults could cover a $400 surprise with cash, so a first step of $500 to $1,000 counts. You can add a goal named \"Emergency fund\" on the Goals page.", src));
            return;
        }
        double months = fund.saved() / essential;
        String detail = "You have " + m0(fund.saved()) + " saved, about " + mo(months) + " months of your essential costs (" + m0(essential) + " a month). The common guideline is " + (int) low + " to " + (int) high
                + " months, which is " + m0(essential * low) + " to " + m0(essential * high) + " for you.";
        String severity = months < 1 ? "WATCH" : months < low ? "INFO" : "GOOD";
        out.add(new Insight("emergency_cover", severity, "Your emergency fund covers about " + mo(months) + " months", detail, src));
    }

    private static void goalInflation(Facts f, List<Insight> out) {
        double cpi = b("cpi_12mo");
        int shown = 0;
        for (GoalFact g : f.goals()) {
            if (shown >= 3) break;
            if (g.monthsLeft() <= 0 || g.monthsLeft() > 36 || g.target() <= g.saved()) continue;
            double adjusted = inflate(g.target(), cpi, g.monthsLeft());
            double extra = adjusted - g.target();
            if (extra < 25) continue;
            double need = (adjusted - g.saved()) / g.monthsLeft();
            double plain = (g.target() - g.saved()) / g.monthsLeft();
            out.add(new Insight("goal_inflation_" + (shown + 1), "INFO", g.name() + ": prices may rise before your date",
                    "At the recent inflation rate of " + pc(cpi) + " (BLS, September 2026), a " + m0(g.target()) + " goal could cost about " + m0(adjusted) + " in " + g.monthsLeft()
                            + " months, which is " + m0(extra) + " more. That assumes the goal's cost rises with prices. Saving toward " + m0(adjusted) + " would take about " + m0(need) + " a month instead of " + m0(plain) + ".",
                    List.of("cpi_12mo")));
            shown++;
        }
    }
}
