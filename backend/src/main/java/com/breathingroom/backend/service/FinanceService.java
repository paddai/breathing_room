package com.breathingroom.backend.service;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.logic.GoalMath;
import com.breathingroom.backend.model.Debt;
import com.breathingroom.backend.model.Deduction;
import com.breathingroom.backend.model.Expense;
import com.breathingroom.backend.model.Goal;
import com.breathingroom.backend.model.PaycheckProfile;
import com.breathingroom.backend.repository.DebtRepository;
import com.breathingroom.backend.repository.DeductionRepository;
import com.breathingroom.backend.repository.ExpenseRepository;
import com.breathingroom.backend.repository.GoalRepository;
import com.breathingroom.backend.repository.PaycheckProfileRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** All the deterministic money math. Java calculates; nothing in here talks to an AI. */
@Service
public class FinanceService {

    private final PaycheckProfileRepository profileRepo;
    private final DeductionRepository deductionRepo;
    private final ExpenseRepository expenseRepo;
    private final DebtRepository debtRepo;
    private final GoalRepository goalRepo;

    public FinanceService(PaycheckProfileRepository profileRepo, DeductionRepository deductionRepo,
                          ExpenseRepository expenseRepo, DebtRepository debtRepo, GoalRepository goalRepo) {
        this.profileRepo = profileRepo;
        this.deductionRepo = deductionRepo;
        this.expenseRepo = expenseRepo;
        this.debtRepo = debtRepo;
        this.goalRepo = goalRepo;
    }

    private static void bad(String message) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public static int periodsPerYear(String frequency) {
        if (frequency == null) return 26;
        return switch (frequency) {
            case "weekly" -> 52;
            case "semimonthly" -> 24;
            case "monthly" -> 12;
            default -> 26; // biweekly
        };
    }

    public static double grossPerPeriod(PaycheckProfile p) {
        if ("salary".equals(p.getPayType())) return p.getSalaryAmount();
        return p.getHourlyRate() * p.getRegularHours()
                + p.getHourlyRate() * p.getOvertimeHours() * p.getOvertimeMultiplier();
    }

    // ------------------------------------------------------------------ paycheck (4.x)

    public PaycheckProfile profile() {
        return profileRepo.findById(1L).orElseGet(PaycheckProfile::new);
    }

    public Dtos.PaycheckDto paycheck() {
        PaycheckProfile p = profile();
        List<Deduction> stored = new ArrayList<>(deductionRepo.findAll());
        stored.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        List<Dtos.DeductionDto> ds = new ArrayList<>();
        for (Deduction d : stored) ds.add(new Dtos.DeductionDto(d.getName(), d.getAmount()));
        return new Dtos.PaycheckDto(p.getPayType(), p.getHourlyRate(), p.getRegularHours(), p.getOvertimeHours(),
                p.getOvertimeMultiplier(), p.getSalaryAmount(), p.getFrequency(), p.getContribution401kPercent(),
                p.getEmployerMatchRate(), p.getEmployerMatchLimit(), ds);
    }

    @Transactional
    public void savePaycheck(Dtos.PaycheckDto d) {
        if (d == null) bad("Missing paycheck data.");
        if (!"hourly".equals(d.payType()) && !"salary".equals(d.payType())) bad("Pay type must be hourly or salary.");
        if (!List.of("weekly", "biweekly", "semimonthly", "monthly").contains(d.frequency())) bad("Unknown pay frequency.");
        double[] numbers = {d.hourlyRate(), d.regularHours(), d.overtimeHours(), d.overtimeMultiplier(),
                d.salaryAmount(), d.contribution401kPercent(), d.employerMatchRate(), d.employerMatchLimit()};
        for (double n : numbers) if (n < 0 || n > 10_000_000) bad("Numbers must be between 0 and 10,000,000.");
        if (d.contribution401kPercent() > 100 || d.employerMatchLimit() > 100 || d.employerMatchRate() > 1000) {
            bad("Percentages look too large.");
        }

        PaycheckProfile p = profile();
        p.setPayType(d.payType());
        p.setHourlyRate(d.hourlyRate());
        p.setRegularHours(d.regularHours());
        p.setOvertimeHours(d.overtimeHours());
        p.setOvertimeMultiplier(d.overtimeMultiplier());
        p.setSalaryAmount(d.salaryAmount());
        p.setFrequency(d.frequency());
        p.setContribution401kPercent(d.contribution401kPercent());
        p.setEmployerMatchRate(d.employerMatchRate());
        p.setEmployerMatchLimit(d.employerMatchLimit());
        profileRepo.save(p);

        deductionRepo.deleteAll();
        if (d.deductions() != null) {
            for (Dtos.DeductionDto x : d.deductions()) {
                if (x.name() == null || x.name().isBlank() || x.amount() <= 0 || x.amount() > 10_000_000) continue;
                Deduction ded = new Deduction();
                ded.setName(clip(x.name(), 60));
                ded.setAmount(x.amount());
                deductionRepo.save(ded);
            }
        }
    }

    public static String clip(String s, int max) {
        if (s == null) return "";
        String oneLine = s.replace('\n', ' ').replace('\r', ' ').trim();
        return oneLine.length() <= max ? oneLine : oneLine.substring(0, max);
    }

    // ------------------------------------------------------------------ budget summary (5.4, 5.5, 6.4, 8.x)

    public Dtos.SummaryDto summary() {
        PaycheckProfile p = profile();
        double gross = grossPerPeriod(p);
        double deductionsTotal = 0;
        for (Deduction d : deductionRepo.findAll()) deductionsTotal += d.getAmount();
        double k401 = gross * p.getContribution401kPercent() / 100.0;
        double takeHome = Math.max(0, gross - deductionsTotal - k401);
        int periods = periodsPerYear(p.getFrequency());
        double monthlyTakeHome = takeHome * periods / 12.0;

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        List<Expense> all = new ArrayList<>(expenseRepo.findAll());
        Map<String, Double> byCategory = new LinkedHashMap<>();
        double monthlyExpenses = 0;
        for (Expense e : all) {
            if (e.getExpenseDate() != null && e.getExpenseDate().isBefore(monthStart)) continue;
            monthlyExpenses += e.getAmount();
            String cat = e.getCategory() == null ? "Other" : e.getCategory();
            byCategory.merge(cat, e.getAmount(), Double::sum);
        }
        all.sort((a, b) -> {
            LocalDate da = a.getExpenseDate();
            LocalDate db = b.getExpenseDate();
            if (da == null && db == null) return 0;
            if (da == null) return 1;
            if (db == null) return -1;
            return db.compareTo(da);
        });
        List<Expense> recent = new ArrayList<>(all.subList(0, Math.min(5, all.size())));

        double totalDebt = 0;
        double minimums = 0;
        for (Debt d : debtRepo.findAll()) {
            totalDebt += d.getBalance();
            minimums += d.getMinPayment();
        }

        int goalCount = 0;
        double saved = 0;
        double target = 0;
        for (Goal g : goalRepo.findAll()) {
            goalCount++;
            saved += g.getSavedAmount();
            target += g.getTargetAmount();
        }

        double surplus = monthlyTakeHome - monthlyExpenses - minimums;
        Map<String, Double> rounded = new LinkedHashMap<>();
        byCategory.forEach((k, v) -> rounded.put(k, GoalMath.round2(v)));

        return new Dtos.SummaryDto(gross > 0, GoalMath.round2(gross), GoalMath.round2(takeHome), periods,
                GoalMath.round2(monthlyTakeHome), GoalMath.round2(monthlyExpenses), GoalMath.round2(minimums),
                GoalMath.round2(surplus), GoalMath.round2(totalDebt), rounded, recent, goalCount,
                GoalMath.round2(saved), GoalMath.round2(target));
    }

    // ------------------------------------------------------------------ 401(k) vs debt (7.x)

    private Dtos.RecommendationFacts noData(String message) {
        return new Dtos.RecommendationFacts(false, message, 0, 0, 0, 0, "", 0, 0, 0, false, List.of());
    }

    public Dtos.RecommendationFacts recommendationFacts() {
        PaycheckProfile p = profile();
        double gross = grossPerPeriod(p);
        if (gross <= 0) return noData("Enter your paycheck first so we can compare your 401(k) with your employer match.");
        if (p.getEmployerMatchLimit() <= 0) return noData("Add your employer match on the Paycheck page to see this comparison.");

        Debt top = null;
        for (Debt d : debtRepo.findAll()) if (top == null || d.getApr() > top.getApr()) top = d;

        double above = Math.max(0, p.getContribution401kPercent() - p.getEmployerMatchLimit());
        double amountAbove = gross * above / 100.0;
        boolean flagged = above > 0 && top != null && top.getApr() > 7.0;

        double annualRedirect = amountAbove * periodsPerYear(p.getFrequency());
        double saved = top == null ? 0 : Math.min(annualRedirect, top.getBalance()) * top.getApr() / 100.0;

        String limit = String.format(Locale.US, "%.1f", p.getEmployerMatchLimit());
        List<Dtos.RecommendationOption> options = List.of(
                new Dtos.RecommendationOption("reduce-401k", "Lower 401(k) to " + limit + "% (match only)",
                        "Send the difference to your highest-interest debt."),
                new Dtos.RecommendationOption("keep-401k", "Keep your 401(k) the same",
                        "Find the extra payment somewhere else in your budget."),
                new Dtos.RecommendationOption("keep-as-is", "Keep everything as is",
                        "You may have a reason, such as a vesting cliff or an upcoming raise."));

        return new Dtos.RecommendationFacts(true, "", GoalMath.round2(p.getContribution401kPercent()),
                GoalMath.round2(p.getEmployerMatchLimit()), GoalMath.round2(above), GoalMath.round2(amountAbove),
                top == null ? "" : top.getName(), top == null ? 0 : top.getApr(), top == null ? 0 : top.getBalance(),
                GoalMath.round2(saved), flagged, options);
    }

    // ------------------------------------------------------------------ goals (read side)

    public double availableForGoal(Goal g) {
        double surplus = Math.max(0, summary().monthlySurplus());
        double others = 0;
        for (Goal other : goalRepo.findAll()) {
            if (!other.getId().equals(g.getId())) others += other.getMonthlyContribution();
        }
        return Math.max(0, surplus - others);
    }

    public Dtos.GoalView goalView(Goal g) {
        double remaining = Math.max(0, g.getTargetAmount() - g.getSavedAmount());
        int months = GoalMath.monthsLeft(LocalDate.now(), g.getTargetDate());
        double required = GoalMath.requiredMonthly(g.getTargetAmount(), g.getSavedAmount(), months);
        double percent = g.getTargetAmount() <= 0 ? 0 : Math.min(100, g.getSavedAmount() / g.getTargetAmount() * 100.0);

        String status;
        if (remaining <= 0) status = "Complete";
        else if (months == 0) status = "Overdue";
        else if (g.getMonthlyContribution() + 0.005 >= required) status = "On track";
        else status = "Needs attention";

        return new Dtos.GoalView(g.getId(), g.getName(), g.getTargetAmount(), g.getSavedAmount(),
                g.getTargetDate() == null ? null : g.getTargetDate().toString(), g.getMonthlyContribution(),
                GoalMath.round2(percent), months, required, status);
    }

    public List<Dtos.GoalView> goalViews() {
        List<Goal> all = new ArrayList<>(goalRepo.findAll());
        all.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        List<Dtos.GoalView> out = new ArrayList<>();
        for (Goal g : all) out.add(goalView(g));
        return out;
    }
}
