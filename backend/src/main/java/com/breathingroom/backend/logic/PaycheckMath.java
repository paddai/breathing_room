package com.breathingroom.backend.logic;

/** Take-home pay math. One place for the formulas, used by the Summary and by the live preview on the Paycheck page. */
public final class PaycheckMath {

    private PaycheckMath() {}

    public record Result(double grossPerPeriod, int periodsPerYear, double grossPerYear, double totalDeductions,
                         double k401PerPeriod, double employerMatchPerPeriod, double takeHomePerPeriod, double takeHomePerMonth) {}

    public static int periodsPerYear(String frequency) {
        if (frequency == null) return 26;
        return switch (frequency) {
            case "weekly" -> 52;
            case "semimonthly" -> 24;
            case "monthly" -> 12;
            default -> 26; // biweekly
        };
    }

    public static double grossPerPeriod(String payType, double hourlyRate, double regularHours, double overtimeHours,
                                        double overtimeMultiplier, double salaryAmount) {
        if ("salary".equals(payType)) return salaryAmount;
        return hourlyRate * regularHours + hourlyRate * overtimeHours * overtimeMultiplier;
    }

    public static Result compute(String payType, String frequency, double hourlyRate, double regularHours, double overtimeHours,
                                 double overtimeMultiplier, double salaryAmount, double contribution401kPercent,
                                 double employerMatchRatePercent, double employerMatchLimitPercent, double deductionsTotal) {
        double gross = grossPerPeriod(payType, hourlyRate, regularHours, overtimeHours, overtimeMultiplier, salaryAmount);
        int periods = periodsPerYear(frequency);
        double k401 = gross * contribution401kPercent / 100.0;
        double matched = Math.min(contribution401kPercent, employerMatchLimitPercent);
        double match = gross * matched / 100.0 * employerMatchRatePercent / 100.0;
        double takeHome = Math.max(0, gross - deductionsTotal - k401);
        return new Result(gross, periods, gross * periods, deductionsTotal, k401, match, takeHome, takeHome * periods / 12.0);
    }
}
