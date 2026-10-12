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

    // ---- Advisor ----
    public record AskRequest(String question) {}

    public record AskResponse(String answer, boolean aiUsed, String note) {}

    public record AiStatus(boolean configured, String model) {}
}
