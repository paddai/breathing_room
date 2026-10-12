package com.breathingroom.backend.service;

import com.breathingroom.backend.model.Debt;
import com.breathingroom.backend.model.Deduction;
import com.breathingroom.backend.model.Expense;
import com.breathingroom.backend.model.Goal;
import com.breathingroom.backend.model.PaycheckProfile;
import com.breathingroom.backend.repository.DebtRepository;
import com.breathingroom.backend.repository.DeductionRepository;
import com.breathingroom.backend.repository.ExpenseRepository;
import com.breathingroom.backend.repository.GoalDecisionRepository;
import com.breathingroom.backend.repository.GoalRepository;
import com.breathingroom.backend.repository.PaycheckProfileRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sample data so a classmate can clone the repo and see a working app in one click (task 10.3). */
@Service
public class DemoDataService {

    private final PaycheckProfileRepository profileRepo;
    private final DeductionRepository deductionRepo;
    private final ExpenseRepository expenseRepo;
    private final DebtRepository debtRepo;
    private final GoalRepository goalRepo;
    private final GoalDecisionRepository decisionRepo;

    public DemoDataService(PaycheckProfileRepository profileRepo, DeductionRepository deductionRepo,
                           ExpenseRepository expenseRepo, DebtRepository debtRepo, GoalRepository goalRepo,
                           GoalDecisionRepository decisionRepo) {
        this.profileRepo = profileRepo;
        this.deductionRepo = deductionRepo;
        this.expenseRepo = expenseRepo;
        this.debtRepo = debtRepo;
        this.goalRepo = goalRepo;
        this.decisionRepo = decisionRepo;
    }

    @Transactional
    public void reset() {
        decisionRepo.deleteAll();
        goalRepo.deleteAll();
        debtRepo.deleteAll();
        expenseRepo.deleteAll();
        deductionRepo.deleteAll();
        profileRepo.deleteAll();
    }

    @Transactional
    public void seed() {
        reset();

        PaycheckProfile p = new PaycheckProfile();
        p.setPayType("salary");
        p.setSalaryAmount(2150);
        p.setFrequency("biweekly");
        p.setContribution401kPercent(9);
        p.setEmployerMatchRate(50);
        p.setEmployerMatchLimit(6);
        profileRepo.save(p);

        addDeduction("Federal tax", 215.00);
        addDeduction("State tax", 65.00);
        addDeduction("Social Security + Medicare", 164.48);
        addDeduction("Health insurance", 80.00);

        LocalDate today = LocalDate.now();
        addExpense("Rent", "Housing", 1150.00, today.withDayOfMonth(1));
        addExpense("Whole Foods", "Groceries", 186.40, today.minusDays(2));
        addExpense("Corner Market", "Groceries", 94.25, today.minusDays(6));
        addExpense("Gas", "Transportation", 58.00, today.minusDays(4));
        addExpense("Electric bill", "Utilities", 112.30, today.minusDays(3));
        addExpense("Internet", "Utilities", 55.00, today.minusDays(5));
        addExpense("Dinner out", "Dining", 62.50, today.minusDays(1));
        addExpense("Clothes", "Shopping", 74.00, today.minusDays(7));

        addDebt("Store card", 600, 26, 25);
        addDebt("Credit card", 4200, 24, 90);
        addDebt("Car loan", 9800, 7, 220);

        addGoal("Emergency fund", 6000, 3200, today.plusMonths(8), 100);
        addGoal("Family trip", 1800, 350, today.plusMonths(6), 0);
    }

    private void addDeduction(String name, double amount) {
        Deduction d = new Deduction();
        d.setName(name);
        d.setAmount(amount);
        deductionRepo.save(d);
    }

    private void addExpense(String description, String category, double amount, LocalDate date) {
        Expense e = new Expense();
        e.setDescription(description);
        e.setCategory(category);
        e.setAmount(amount);
        e.setExpenseDate(date);
        expenseRepo.save(e);
    }

    private void addDebt(String name, double balance, double apr, double minPayment) {
        Debt d = new Debt();
        d.setName(name);
        d.setBalance(balance);
        d.setApr(apr);
        d.setMinPayment(minPayment);
        debtRepo.save(d);
    }

    private void addGoal(String name, double target, double saved, LocalDate date, double monthly) {
        Goal g = new Goal();
        g.setName(name);
        g.setTargetAmount(target);
        g.setSavedAmount(saved);
        g.setTargetDate(date);
        g.setMonthlyContribution(monthly);
        goalRepo.save(g);
    }
}
