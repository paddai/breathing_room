package com.breathingroom.backend.logic;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns a question plus the user's saved numbers into: what the question is about, the facts that matter for it,
 * and two or three choices WITH their trade-offs. Every number here is calculated in Java (DebtPayoff, ScenarioMath).
 * The AI may only explain these; it never creates a choice or a figure. No Spring, no database - easy to test.
 */
public final class AdvisorPlanner {

    private AdvisorPlanner() {}

    public enum Intent { DEBT, RETIREMENT, SAVINGS, SPENDING, GENERAL }

    public record DebtLine(String name, double balance, double apr, double minPayment) {}

    public record Snapshot(boolean hasPaycheck, double grossPerPeriod, int periodsPerYear, double monthlyTakeHome,
                           double monthlyExpenses, double monthlyDebtMinimums, double monthlySurplus,
                           double contributionPercent, double matchLimitPercent,
                           Map<String, Double> spendingByCategory, List<DebtLine> debts,
                           double goalsSaved, double goalsTarget) {}

    public record Choice(String label, String detail, String tradeoff) {}

    public record Plan(Intent intent, String headline, Map<String, String> facts, List<Choice> choices) {
        /** Every text the AI is allowed to quote numbers from. */
        public List<String> allowedSources() {
            List<String> out = new ArrayList<>(facts.values());
            out.add(headline);
            for (Choice c : choices) { out.add(c.label()); out.add(c.detail()); out.add(c.tradeoff()); }
            return out;
        }
    }

    private static final List<String> WANTS = List.of("Dining", "Entertainment", "Subscriptions", "Travel", "Shopping", "Miscellaneous");

    public static Intent detect(String question) {
        String q = question == null ? "" : question.toLowerCase(Locale.ROOT);
        if (has(q, "401", "retire", "match", "ira ", "employer")) return Intent.RETIREMENT;
        if (has(q, "debt", "owe", "loan", "credit card", "apr", "interest", "payoff", "pay off", "snowball", "avalanche")) return Intent.DEBT;
        if (has(q, "emergency", "save", "saving", "goal", "afford", "cushion", "rainy")) return Intent.SAVINGS;
        if (has(q, "spend", "budget", "cut", "expense", "groceries", "dining", "subscription", "where does my money")) return Intent.SPENDING;
        return Intent.GENERAL;
    }

    private static boolean has(String q, String... words) {
        for (String w : words) if (q.contains(w)) return true;
        return false;
    }

    static String money(double v) { return GoalMath.money(v); }

    static String pct(double v) { return String.format(Locale.US, "%.1f%%", v); }

    static String duration(int months) {
        if (months <= 0) return "under a month";
        int y = months / 12, m = months % 12;
        if (y == 0) return months + (months == 1 ? " month" : " months");
        return y + (y == 1 ? " year" : " years") + (m == 0 ? "" : " " + m + (m == 1 ? " month" : " months"));
    }

    public static Plan plan(Intent intent, Snapshot s) {
        Map<String, String> facts = baseFacts(s);
        return switch (intent) {
            case DEBT -> debtPlan(s, facts);
            case RETIREMENT -> retirementPlan(s, facts);
            case SAVINGS -> savingsPlan(s, facts);
            case SPENDING -> spendingPlan(s, facts);
            case GENERAL -> generalPlan(s, facts);
        };
    }

    private static Map<String, String> baseFacts(Snapshot s) {
        Map<String, String> f = new LinkedHashMap<>();
        if (!s.hasPaycheck()) {
            f.put("Paycheck", "Not entered yet");
            return f;
        }
        f.put("Monthly take-home pay", money(s.monthlyTakeHome()));
        f.put("Spent this month", money(s.monthlyExpenses()));
        f.put("Monthly debt minimums", money(s.monthlyDebtMinimums()));
        f.put("Free money each month (surplus)", money(s.monthlySurplus()));
        return f;
    }

    private static Plan needPaycheck(Intent intent, Map<String, String> facts) {
        return new Plan(intent, "Enter your paycheck first so the app can work out how much free money you have each month.", facts, List.of());
    }

    // ---------------------------------------------------------------- debt
    private static Plan debtPlan(Snapshot s, Map<String, String> facts) {
        List<DebtPayoff.Debt> debts = s.debts().stream().filter(d -> d.balance() > 0)
                .map(d -> new DebtPayoff.Debt(d.name(), d.balance(), d.apr(), d.minPayment())).toList();
        if (debts.isEmpty()) return new Plan(Intent.DEBT, "You have no debts entered. Add them on the Debts page to compare payoff plans.", facts, List.of());
        if (!s.hasPaycheck()) return needPaycheck(Intent.DEBT, facts);

        double avg = Benchmarks.value("fed_card_apr");
        double total = debts.stream().mapToDouble(DebtPayoff.Debt::balance).sum();
        DebtPayoff.Debt top = debts.stream().max(Comparator.comparingDouble(DebtPayoff.Debt::apr)).orElseThrow();
        facts.put("Total debt", money(total));
        facts.put("Average credit card rate on balances (Federal Reserve)", pct(avg));
        int i = 1;
        List<DebtPayoff.Debt> above = new ArrayList<>();
        for (DebtPayoff.Debt d : debts) {
            String gap = d.apr() > avg ? ", " + String.format(Locale.US, "%.1f", d.apr() - avg) + " percentage points above that average"
                    : ", at or below that average";
            facts.put("Debt " + i++, d.name() + ": balance " + money(d.balance()) + ", " + pct(d.apr()) + " APR, minimum " + money(d.minPayment()) + gap);
            if (d.apr() > avg) above.add(d);
        }
        facts.put("Highest-rate debt", top.name() + " at " + pct(top.apr()) + " APR");

        double surplus = Math.max(0, s.monthlySurplus());
        double half = Math.floor(surplus / 2.0);
        DebtPayoff.Result base = DebtPayoff.simulate(debts, 0, DebtPayoff.Strategy.AVALANCHE);
        List<String[]> picks = new ArrayList<>(); // {label, detail, tradeoff} before letters are added

        // Option: ask for a lower rate, or move the balance. Works even when there is no free money.
        if (!above.isEmpty()) {
            double now = 0, lower = 0;
            StringBuilder names = new StringBuilder();
            for (DebtPayoff.Debt d : above) {
                now += d.balance() * d.apr() / 1200.0;
                lower += d.balance() * avg / 1200.0;
                if (names.length() > 0) names.append(" and ");
                names.append(d.name());
            }
            picks.add(new String[]{"Ask for a lower rate on " + names + ", or move the balance to a lower-rate card or loan",
                    "If the rate came down to the " + pct(avg) + " average, monthly interest on " + (above.size() > 1 ? "them" : "it") + " would fall from about "
                            + money(now) + " to about " + money(lower) + ", roughly " + money(now - lower) + " a month.",
                    "Costs nothing to ask, but the issuer can say no. A balance transfer or consolidation loan usually has a fee and can briefly lower your credit score."});
        }
        if (half > 0) {
            DebtPayoff.Result r = DebtPayoff.simulate(debts, half, DebtPayoff.Strategy.AVALANCHE);
            picks.add(new String[]{"Add " + money(half) + " a month to " + top.name() + " (highest rate first)",
                    "Debt-free in about " + duration(r.months()) + ", about " + money(r.totalInterest()) + " in interest"
                            + (base.paidOff() && r.paidOff() ? ", saving about " + money(base.totalInterest() - r.totalInterest()) + " versus minimums." : "."),
                    "Half your free money still stays available for savings or surprises."});
            DebtPayoff.Result sn = DebtPayoff.simulate(debts, half, DebtPayoff.Strategy.SNOWBALL);
            if (debts.size() > 1 && sn.months() != r.months() && picks.size() < 3) {
                picks.add(new String[]{"Same " + money(half) + ", but smallest balance first (snowball)",
                        "Debt-free in about " + duration(sn.months()) + ", about " + money(sn.totalInterest()) + " in interest.",
                        "Costs about " + money(Math.abs(sn.totalInterest() - r.totalInterest())) + (sn.totalInterest() >= r.totalInterest() ? " more" : " less")
                                + " in interest than adding it to the highest rate, but clears whole debts sooner, which some people find motivating."});
            } else if (surplus > half && picks.size() < 3) {
                DebtPayoff.Result all = DebtPayoff.simulate(debts, surplus, DebtPayoff.Strategy.AVALANCHE);
                picks.add(new String[]{"Send all " + money(surplus) + " of free money to debt",
                        "Debt-free in about " + duration(all.months()) + ", about " + money(all.totalInterest()) + " in interest.",
                        "Fastest payoff, but it leaves no room in the budget for savings or an unexpected bill."});
            }
        } else {
            // No free money: show what trimming flexible spending could do for the highest-rate debt.
            double flexible = 0;
            for (Map.Entry<String, Double> e : s.spendingByCategory().entrySet()) if (WANTS.contains(e.getKey())) flexible += e.getValue();
            double ten = Math.round(flexible * 0.10 * 100.0) / 100.0;
            if (ten > 0) {
                DebtPayoff.Result r = DebtPayoff.simulate(debts, ten, DebtPayoff.Strategy.AVALANCHE);
                picks.add(new String[]{"Trim flexible spending by 10% and send it to " + top.name(),
                        "Frees about " + money(ten) + " a month; debt-free in about " + duration(r.months()) + ", about " + money(r.totalInterest()) + " in interest"
                                + (base.paidOff() && r.paidOff() ? ", saving about " + money(base.totalInterest() - r.totalInterest()) + " versus minimums." : "."),
                        "Needs small everyday sacrifices (dining, entertainment, subscriptions), but it is the only way to add money to debt when the budget has no free money."});
            }
        }
        if (picks.size() < 3) {
            picks.add(new String[]{"Keep paying only the minimums",
                    base.paidOff() ? "Debt-free in about " + duration(base.months()) + ", with about " + money(base.totalInterest()) + " in total interest."
                            : "At the minimums alone these debts are not paid off within 50 years.",
                    "Lowest monthly strain, but you pay the most interest and stay in debt longest."});
        }
        List<Choice> choices = new ArrayList<>();
        for (int k = 0; k < picks.size(); k++) {
            choices.add(new Choice((char) ('A' + k) + ". " + picks.get(k)[0], picks.get(k)[1], picks.get(k)[2]));
        }
        String head = above.isEmpty()
                ? "Your highest rate is " + top.name() + " at " + pct(top.apr()) + " APR, which is not above the " + pct(avg) + " average credit card rate."
                : "Your highest rate is " + top.name() + " at " + pct(top.apr()) + " APR, above the " + pct(avg) + " average credit card rate.";
        head += surplus <= 0 ? " Your budget has no free money after bills and debt minimums, so extra payments would need spending to come down first."
                : " You have " + money(surplus) + " of free money each month.";
        return new Plan(Intent.DEBT, head, facts, choices);
    }

    // ---------------------------------------------------------------- retirement / 401(k)
    private static Plan retirementPlan(Snapshot s, Map<String, String> facts) {
        if (!s.hasPaycheck()) return needPaycheck(Intent.RETIREMENT, facts);
        facts.put("Your 401(k) contribution", pct(s.contributionPercent()) + " of pay");
        facts.put("Employer match applies up to", pct(s.matchLimitPercent()) + " of pay");
        facts.put("IRS 401(k) limit per year", money(Benchmarks.value("irs_401k_limit")));
        facts.put("Suggested total retirement saving (employee plus employer)", pct(Benchmarks.value("vanguard_target_low")) + " to " + pct(Benchmarks.value("vanguard_target_high")) + " of pay");

        List<DebtPayoff.Debt> debts = s.debts().stream().map(d -> new DebtPayoff.Debt(d.name(), d.balance(), d.apr(), d.minPayment())).toList();
        List<Choice> choices = new ArrayList<>();
        if (s.matchLimitPercent() <= 0) {
            return new Plan(Intent.RETIREMENT, "No employer match is entered, so the app cannot say how much free employer money you are getting. Add it on the Paycheck page.", facts, choices);
        }
        if (s.contributionPercent() < s.matchLimitPercent()) {
            double gap = s.matchLimitPercent() - s.contributionPercent();
            double perMonth = s.grossPerPeriod() * gap / 100.0 * s.periodsPerYear() / 12.0;
            choices.add(new Choice("A. Raise your contribution to " + pct(s.matchLimitPercent()) + " to capture the full match",
                    "That is " + pct(gap) + " more of pay, about " + money(perMonth) + " a month before tax.",
                    "Your take-home drops a little, but you stop leaving employer match money unclaimed."));
            choices.add(new Choice("B. Keep your current " + pct(s.contributionPercent()) + " for now",
                    "No change to your paycheck.", "You give up part of the employer match until you raise it."));
            return new Plan(Intent.RETIREMENT, "You contribute " + pct(s.contributionPercent()) + " but your employer matches up to " + pct(s.matchLimitPercent()) + ".", facts, choices);
        }
        double above = s.contributionPercent() - s.matchLimitPercent();
        ScenarioMath.Scenario full = ScenarioMath.compute(s.grossPerPeriod(), s.periodsPerYear(), s.contributionPercent(), s.matchLimitPercent(), 100, debts);
        choices.add(new Choice("A. Keep your " + pct(s.contributionPercent()) + " contribution",
                above <= 0 ? "You are contributing exactly what the employer matches." : "You save " + pct(above) + " of pay beyond the match.",
                "Builds retirement savings steadily, but any high-rate debt keeps charging interest meanwhile."));
        if (full.applicable()) {
            ScenarioMath.Scenario half = ScenarioMath.compute(s.grossPerPeriod(), s.periodsPerYear(),
                    s.matchLimitPercent() + above / 2.0, s.matchLimitPercent(), 100, debts);
            if (half.applicable()) {
                choices.add(new Choice("B. Trim to " + pct(s.matchLimitPercent() + above / 2.0) + " and send the difference to " + full.targetDebt(),
                        "About " + money(half.freedGrossPerMonth()) + " a month before tax moves to debt"
                                + (half.monthsSaved() > 0 ? ", finishing about " + duration(half.monthsSaved()) + " sooner and saving about " + money(half.interestSaved()) + " in interest." : "."),
                        "A middle path: you still keep the full match and keep saving above it."));
            }
            choices.add(new Choice("C. Drop to " + pct(s.matchLimitPercent()) + " (the match) and send everything above it to " + full.targetDebt(),
                    "About " + money(full.freedGrossPerMonth()) + " a month before tax moves to debt"
                            + (full.monthsSaved() > 0 ? ", finishing about " + duration(full.monthsSaved()) + " sooner and saving about " + money(full.interestSaved()) + " in interest." : "."),
                    full.caution()));
            facts.put("Highest-rate debt", full.targetDebt() + " at " + pct(full.targetApr()) + " APR");
        }
        return new Plan(Intent.RETIREMENT, "You contribute " + pct(s.contributionPercent()) + " of pay and your employer matches up to " + pct(s.matchLimitPercent()) + ".", facts, choices);
    }

    // ---------------------------------------------------------------- savings / emergency fund
    private static Plan savingsPlan(Snapshot s, Map<String, String> facts) {
        if (!s.hasPaycheck()) return needPaycheck(Intent.SAVINGS, facts);
        double essentials = s.monthlyExpenses() + s.monthlyDebtMinimums();
        double surplus = Math.max(0, s.monthlySurplus());
        if (s.goalsTarget() > 0) {
            facts.put("Saved toward goals", money(s.goalsSaved()) + " of " + money(s.goalsTarget()));
        }
        List<Choice> choices = new ArrayList<>();
        if (surplus <= 0) {
            return new Plan(Intent.SAVINGS, "You have no free money after bills and debt minimums this month, so saving more means lowering spending or raising income first.", facts, choices);
        }
        double cushionTarget = essentials * Benchmarks.value("emergency_months_low");
        facts.put("Spending plus debt minimums per month (a rough stand-in for essential costs)", money(essentials));
        facts.put("A 3-month cushion would be", money(cushionTarget));
        double half = Math.floor(surplus / 2.0);
        double full = Math.floor(surplus);
        if (cushionTarget > 0) {
            choices.add(new Choice("A. Save " + money(full) + " a month (all your free money)",
                    "A 3-month cushion in about " + duration((int) Math.ceil(cushionTarget / full)) + ".",
                    "Fastest, but leaves nothing for fun or surprises, so it is harder to keep up."));
            if (half > 0) choices.add(new Choice("B. Save " + money(half) + " a month (half of it)",
                    "A 3-month cushion in about " + duration((int) Math.ceil(cushionTarget / half)) + ".",
                    "Slower, but leaves the other half for debt, goals or breathing room."));
        }
        double quarter = Math.floor(surplus / 4.0);
        if (quarter > 0 && cushionTarget > 0) choices.add(new Choice("C. Save " + money(quarter) + " a month (a quarter of it)",
                "A 3-month cushion in about " + duration((int) Math.ceil(cushionTarget / quarter)) + ".",
                "Easiest to sustain on a tight month, but it takes the longest."));
        return new Plan(Intent.SAVINGS, "You have " + money(surplus) + " of free money each month to split between savings, debt and everyday life.", facts, choices);
    }

    // ---------------------------------------------------------------- spending
    private static Plan spendingPlan(Snapshot s, Map<String, String> facts) {
        if (!s.hasPaycheck()) return needPaycheck(Intent.SPENDING, facts);
        List<Map.Entry<String, Double>> top = s.spendingByCategory().entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed()).limit(3).toList();
        int i = 1;
        for (Map.Entry<String, Double> e : top) facts.put("Top category " + i++, e.getKey() + " " + money(e.getValue()));
        List<Choice> choices = new ArrayList<>();
        double flexible = 0;
        String flexName = null; double flexAmt = 0;
        for (Map.Entry<String, Double> e : s.spendingByCategory().entrySet()) {
            if (WANTS.contains(e.getKey())) {
                flexible += e.getValue();
                if (e.getValue() > flexAmt) { flexAmt = e.getValue(); flexName = e.getKey(); }
            }
        }
        if (flexible > 0) {
            facts.put("Flexible spending (dining, entertainment, subscriptions, travel and similar)", money(flexible));
            double ten = Math.round(flexible * 0.10 * 100.0) / 100.0;
            double twenty = Math.round(flexible * 0.20 * 100.0) / 100.0;
            choices.add(new Choice("A. Trim flexible spending by 10%", "Frees about " + money(ten) + " a month.",
                    "Barely noticeable day to day, but a small effect on your budget."));
            choices.add(new Choice("B. Trim flexible spending by 20%", "Frees about " + money(twenty) + " a month.",
                    "A bigger lift, and it means saying no to some things you currently enjoy."));
            if (flexName != null) {
                double cutBig = Math.round(flexAmt * 0.25 * 100.0) / 100.0;
                choices.add(new Choice("C. Cut only " + flexName + " by 25%", "Frees about " + money(cutBig) + " a month.",
                        "Focuses on your largest flexible category and leaves the rest alone."));
            }
        }
        String head = top.isEmpty() ? "No expenses are logged yet, so the app cannot see where your money goes."
                : "Your biggest category is " + top.get(0).getKey() + " at " + money(top.get(0).getValue()) + " this month.";
        return new Plan(Intent.SPENDING, head, facts, choices);
    }

    // ---------------------------------------------------------------- general
    private static Plan generalPlan(Snapshot s, Map<String, String> facts) {
        if (!s.hasPaycheck()) return needPaycheck(Intent.GENERAL, facts);
        double total = s.debts().stream().mapToDouble(DebtLine::balance).sum();
        if (total > 0) facts.put("Total debt", money(total));
        int i = 1;
        for (DebtLine d : s.debts()) {
            if (d.balance() > 0) facts.put("Debt " + i++, d.name() + ": balance " + money(d.balance()) + ", " + pct(d.apr()) + " APR, minimum " + money(d.minPayment()));
        }
        if (s.goalsTarget() > 0) facts.put("Saved toward goals", money(s.goalsSaved()) + " of " + money(s.goalsTarget()));
        String head = s.monthlySurplus() >= 0
                ? "After bills and debt minimums you have " + money(s.monthlySurplus()) + " of free money each month."
                : "After bills and debt minimums you are short by " + money(-s.monthlySurplus()) + " each month.";
        return new Plan(Intent.GENERAL, head, facts, List.of());
    }
}
