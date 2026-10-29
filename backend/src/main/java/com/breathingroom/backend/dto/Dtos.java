package com.breathingroom.backend.dto;

import com.breathingroom.backend.logic.GoalMath;
import com.breathingroom.backend.model.Expense;
import java.util.List;
import java.util.Map;

/** Plain data shapes sent to / received from the React app. Records keep them short and immutable. */
public final class Dtos {

    private Dtos() {}

    // ---- Paycheck ----
    public record DeductionDto(String name, double amount) {}

    public record PaycheckDto(String payType, double hourlyRate, double regularHours, double overtimeHours,
                              double overtimeMultiplier, double salaryAmount, String frequency,
                              double contribution401kPercent, double employerMatchRate, double employerMatchLimit,
                              List<DeductionDto> deductions) {}

    // ---- Summary (dashboard, budget, surplus) ----
    public record SummaryDto(boolean hasPaycheck, double grossPerPeriod, double takeHomePerPeriod, int periodsPerYear,
                             double monthlyTakeHome, double monthlyExpenses, double monthlyDebtMinimums,
                             double monthlySurplus, double totalDebt, Map<String, Double> spendingByCategory,
                             List<Expense> recentExpenses, int goalCount, double goalsSaved, double goalsTarget) {}

    // ---- 401(k) vs debt recommendation ----
    public record RecommendationOption(String id, String label, String detail) {}

    public record RecommendationFacts(boolean hasData, String message, double contributionPercent,
                                      double matchLimitPercent, double contributionAbovePercent,
                                      double amountAbovePerPeriod, String debtName, double debtApr,
                                      double debtBalance, double estimatedAnnualInterestSaved, boolean flagged,
                                      List<RecommendationOption> options) {}

    public record RecommendationDto(RecommendationFacts facts, String explanation, boolean aiUsed, String aiNote) {}

    // ---- Goals ----
    public record GoalInput(String name, double targetAmount, double savedAmount, String targetDate,
                            double monthlyContribution) {}

    public record GoalView(Long id, String name, double targetAmount, double savedAmount, String targetDate,
                           double monthlyContribution, double percentComplete, int monthsLeft,
                           double requiredMonthly, String status) {}

    public record GoalAdvice(GoalView goal, double monthlySurplus, double availableForGoal,
                             List<GoalMath.Option> options, String recommendedOptionId, String explanation,
                             boolean aiUsed, String aiNote) {}

    public record DecisionRequest(String optionId, String status) {}

    public record AddSavingsRequest(double amount) {}

    // ---- Payoff, scenario and paycheck preview (all calculated in Java) ----
    public record DebtInput(String name, double balance, double apr, double minPayment) {}

    public record PayoffRequest(Double extra, List<DebtInput> debts) {}

    public record PayoffStep(String name, int month) {}

    public record PayoffResult(String strategy, int months, boolean paidOff, double totalInterest, List<PayoffStep> order) {}

    public record PayoffComparison(double extra, int debtCount, double totalDebt, PayoffResult avalanche, PayoffResult snowball,
                                   PayoffResult minimumsOnly, String message) {}

    public record ScenarioDto(boolean applicable, String note, double freedGrossPerMonth, double extraToDebtPerMonth, double afterTaxPercent,
                              String targetDebt, double targetApr, PayoffResult current, PayoffResult alternative, int monthsSaved,
                              double interestSaved, double retirementGivenUp, String caution) {}

    public record PaycheckPreview(double grossPerPeriod, int periodsPerYear, double grossPerYear, double totalDeductions,
                                  double k401PerPeriod, double employerMatchPerPeriod, double takeHomePerPeriod, double takeHomePerMonth) {}

    // ---- Accounts ----
    public record AuthRequest(String email, String password) {}

    public record PasswordRequest(String password) {}

    public record UserDto(Long id, String email) {}

    public record AuthResponse(String token, long expiresInSeconds, UserDto user) {}

    // ---- Advisor ----
    public record AskRequest(String question) {}

    public record AskResponse(String answer, boolean aiUsed, String note) {}

    public record AiStatus(boolean configured, String model) {}
}
