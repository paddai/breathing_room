package com.breathingroom.backend.service;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.logic.GoalMath;
import com.breathingroom.backend.logic.PaycheckMath;
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
import com.breathingroom.backend.validation.FieldValidationException;
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
        return PaycheckMath.periodsPerYear(frequency);
    }

    public static double grossPerPeriod(PaycheckProfile p) {
        return PaycheckMath.grossPerPeriod(p.getPayType(), p.getHourlyRate(), p.getRegularHours(), p.getOvertimeHours(),
                p.getOvertimeMultiplier(), p.getSalaryAmount());
    }

    // ------------------------------------------------------------------ paycheck (4.x)

    public PaycheckProfile profile(Long userId) {
        return profileRepo.findById(userId).orElseGet(() -> {
            PaycheckProfile fresh = new PaycheckProfile();
            fresh.setId(userId);
            return fresh;
        });
    }

    public Dtos.PaycheckDto paycheck(Long userId) {
        PaycheckProfile p = profile(userId);
        List<Deduction> stored = new ArrayList<>(deductionRepo.findByUserId(userId));
        stored.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        List<Dtos.DeductionDto> ds = new ArrayList<>();
        for (Deduction d : stored) ds.add(new Dtos.DeductionDto(d.getName(), d.getAmount()));
        return new Dtos.PaycheckDto(p.getPayType(), p.getHourlyRate(), p.getRegularHours(), p.getOvertimeHours(),
                p.getOvertimeMultiplier(), p.getSalaryAmount(), p.getFrequency(), p.getContribution401kPercent(),
                p.getEmployerMatchRate(), p.getEmployerMatchLimit(), ds);
    }

    private static void checkRange(Map<String, String> e, String key, double v, double max, String label) {
        if (v < 0) e.put(key, label + " cannot be negative.");
        else if (v > max) e.put(key, label + " is too large.");
    }

    /** Values that are impossible (negative, absurdly large). Used by both the live preview and Save. */
    private static Map<String, String> rangeErrors(Dtos.PaycheckDto d) {
        Map<String, String> e = new LinkedHashMap<>();
        if (d == null) {
            e.put("form", "Missing paycheck data.");
            return e;
        }
        if (!"hourly".equals(d.payType()) && !"salary".equals(d.payType())) e.put("payType", "Choose Hourly or Salary.");
        if (!List.of("weekly", "biweekly", "semimonthly", "monthly").contains(d.frequency())) e.put("frequency", "Choose how often you are paid.");
        checkRange(e, "hourlyRate", d.hourlyRate(), 10_000, "Hourly rate");
        checkRange(e, "regularHours", d.regularHours(), 1_000, "Regular hours");
        checkRange(e, "overtimeHours", d.overtimeHours(), 1_000, "Overtime hours");
        checkRange(e, "overtimeMultiplier", d.overtimeMultiplier(), 10, "Overtime multiplier");
        checkRange(e, "salaryAmount", d.salaryAmount(), 10_000_000, "Salary");
        if (d.contribution401kPercent() < 0 || d.contribution401kPercent() > 100) e.put("contribution401kPercent", "Enter a percent from 0 to 100.");
        if (d.employerMatchRate() < 0 || d.employerMatchRate() > 200) e.put("employerMatchRate", "Enter a percent from 0 to 200.");
        if (d.employerMatchLimit() < 0 || d.employerMatchLimit() > 100) e.put("employerMatchLimit", "Enter a percent from 0 to 100.");
        if (d.overtimeHours() > 0 && d.overtimeMultiplier() < 1) e.putIfAbsent("overtimeMultiplier", "The overtime multiplier must be at least 1.");
        if (d.deductions() != null) {
            for (Dtos.DeductionDto x : d.deductions()) {
                if (x.amount() < 0) e.put("deductions", "Deduction amounts cannot be negative.");
                else if (x.amount() > 10_000_000) e.put("deductions", "A deduction amount is too large.");
            }
        }
        return e;
    }

    /** What must be filled in before a paycheck can be SAVED (the live preview does not need these). */
    private static Map<String, String> missingErrors(Dtos.PaycheckDto d) {
        Map<String, String> e = new LinkedHashMap<>();
        if ("salary".equals(d.payType())) {
            if (d.salaryAmount() <= 0) e.put("salaryAmount", "Enter your salary for one pay period.");
        } else {
            if (d.hourlyRate() <= 0) e.put("hourlyRate", "Enter your hourly rate.");
            if (d.regularHours() <= 0) e.put("regularHours", "Enter the hours you work in one pay period.");
        }
        return e;
    }

    private static void validatePaycheck(Dtos.PaycheckDto d, boolean mustBeComplete) {
        Map<String, String> errors = rangeErrors(d);
        if (mustBeComplete && !errors.containsKey("form")) missingErrors(d).forEach(errors::putIfAbsent);
        if (!errors.isEmpty()) throw new FieldValidationException(errors);
    }

    /** Live "what would my paycheck be?" for the Paycheck page. Calculates with the same formulas as the Summary; saves nothing. */
    public Dtos.PaycheckPreview preview(Dtos.PaycheckDto d) {
        validatePaycheck(d, false);
        double deductions = 0;
        if (d.deductions() != null) for (Dtos.DeductionDto x : d.deductions()) if (x.amount() > 0 && x.amount() <= 10_000_000) deductions += x.amount();
        PaycheckMath.Result r = PaycheckMath.compute(d.payType(), d.frequency(), d.hourlyRate(), d.regularHours(), d.overtimeHours(),
                d.overtimeMultiplier(), d.salaryAmount(), d.contribution401kPercent(), d.employerMatchRate(), d.employerMatchLimit(), deductions);
        return new Dtos.PaycheckPreview(GoalMath.round2(r.grossPerPeriod()), r.periodsPerYear(), GoalMath.round2(r.grossPerYear()),
                GoalMath.round2(r.totalDeductions()), GoalMath.round2(r.k401PerPeriod()), GoalMath.round2(r.employerMatchPerPeriod()),
                GoalMath.round2(r.takeHomePerPeriod()), GoalMath.round2(r.takeHomePerMonth()));
    }

    @Transactional
    public void savePaycheck(Long userId, Dtos.PaycheckDto d) {
        validatePaycheck(d, true);

        PaycheckProfile p = profile(userId);
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

        deductionRepo.deleteByUserId(userId);
        if (d.deductions() != null) {
            for (Dtos.DeductionDto x : d.deductions()) {
                if (x.name() == null || x.name().isBlank() || x.amount() <= 0 || x.amount() > 10_000_000) continue;
                Deduction ded = new Deduction();
                ded.setUserId(userId);
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

    public Dtos.SummaryDto summary(Long userId) {
        PaycheckProfile p = profile(userId);
        double deductionsTotal = 0;
        for (Deduction d : deductionRepo.findByUserId(userId)) deductionsTotal += d.getAmount();
        PaycheckMath.Result pay = PaycheckMath.compute(p.getPayType(), p.getFrequency(), p.getHourlyRate(), p.getRegularHours(),
                p.getOvertimeHours(), p.getOvertimeMultiplier(), p.getSalaryAmount(), p.getContribution401kPercent(),
                p.getEmployerMatchRate(), p.getEmployerMatchLimit(), deductionsTotal);
        double gross = pay.grossPerPeriod();
        double takeHome = pay.takeHomePerPeriod();
        int periods = pay.periodsPerYear();
        double monthlyTakeHome = takeHome * periods / 12.0;

        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        List<Expense> all = new ArrayList<>(expenseRepo.findByUserId(userId));
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
        for (Debt d : debtRepo.findByUserId(userId)) {
            totalDebt += d.getBalance();
            minimums += d.getMinPayment();
        }

        int goalCount = 0;
        double saved = 0;
        double target = 0;
        for (Goal g : goalRepo.findByUserId(userId)) {
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

    public Dtos.RecommendationFacts recommendationFacts(Long userId) {
        PaycheckProfile p = profile(userId);
        double gross = grossPerPeriod(p);
        if (gross <= 0) return noData("Enter your paycheck first so we can compare your 401(k) with your employer match.");
        if (p.getEmployerMatchLimit() <= 0) return noData("Add your employer match on the Paycheck page to see this comparison.");

        Debt top = null;
        for (Debt d : debtRepo.findByUserId(userId)) if (top == null || d.getApr() > top.getApr()) top = d;

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
        double surplus = Math.max(0, summary(g.getUserId()).monthlySurplus());
        double others = 0;
        for (Goal other : goalRepo.findByUserId(g.getUserId())) {
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

    public List<Dtos.GoalView> goalViews(Long userId) {
        List<Goal> all = new ArrayList<>(goalRepo.findByUserId(userId));
        all.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        List<Dtos.GoalView> out = new ArrayList<>();
        for (Goal g : all) out.add(goalView(g));
        return out;
    }
}
