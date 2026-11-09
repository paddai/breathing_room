package com.breathingroom.backend.model;

import jakarta.persistence.*;

@Entity
public class PaycheckProfile {

    @Id
    private Long id; // equals the owner's user id (one profile per user)

    private String payType = "hourly";

    private double hourlyRate;

    private double regularHours;

    private double overtimeHours;

    private double overtimeMultiplier = 1.5;

    private double salaryAmount;

    private String frequency = "biweekly";

    private double contribution401kPercent;

    private double employerMatchRate;

    private double employerMatchLimit;

    private Long userId;

    public PaycheckProfile() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getPayType() { return payType; }
    public void setPayType(String payType) { this.payType = payType; }

    public double getHourlyRate() { return hourlyRate; }
    public void setHourlyRate(double hourlyRate) { this.hourlyRate = hourlyRate; }

    public double getRegularHours() { return regularHours; }
    public void setRegularHours(double regularHours) { this.regularHours = regularHours; }

    public double getOvertimeHours() { return overtimeHours; }
    public void setOvertimeHours(double overtimeHours) { this.overtimeHours = overtimeHours; }

    public double getOvertimeMultiplier() { return overtimeMultiplier; }
    public void setOvertimeMultiplier(double overtimeMultiplier) { this.overtimeMultiplier = overtimeMultiplier; }

    public double getSalaryAmount() { return salaryAmount; }
    public void setSalaryAmount(double salaryAmount) { this.salaryAmount = salaryAmount; }

    public String getFrequency() { return frequency; }
    public void setFrequency(String frequency) { this.frequency = frequency; }

    public double getContribution401kPercent() { return contribution401kPercent; }
    public void setContribution401kPercent(double contribution401kPercent) { this.contribution401kPercent = contribution401kPercent; }

    public double getEmployerMatchRate() { return employerMatchRate; }
    public void setEmployerMatchRate(double employerMatchRate) { this.employerMatchRate = employerMatchRate; }

    public double getEmployerMatchLimit() { return employerMatchLimit; }
    public void setEmployerMatchLimit(double employerMatchLimit) { this.employerMatchLimit = employerMatchLimit; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

}
