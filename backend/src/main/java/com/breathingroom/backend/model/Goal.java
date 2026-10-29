package com.breathingroom.backend.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
public class Goal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private double targetAmount;

    private double savedAmount;

    private LocalDate targetDate;

    private double monthlyContribution;

    private Long userId;

    public Goal() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getTargetAmount() { return targetAmount; }
    public void setTargetAmount(double targetAmount) { this.targetAmount = targetAmount; }

    public double getSavedAmount() { return savedAmount; }
    public void setSavedAmount(double savedAmount) { this.savedAmount = savedAmount; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public double getMonthlyContribution() { return monthlyContribution; }
    public void setMonthlyContribution(double monthlyContribution) { this.monthlyContribution = monthlyContribution; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

}
