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

/** Sample data for ONE user, so a classmate can sign up and see a working app in one click. */
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

    /** Deletes everything this user owns. Used by "Delete all data" and by account deletion. */
    @Transactional
    public void reset(Long userId) {
        decisionRepo.deleteByUserId(userId);
        goalRepo.deleteByUserId(userId);
        debtRepo.deleteByUserId(userId);
        expenseRepo.deleteByUserId(userId);
        deductionRepo.deleteByUserId(userId);
        profileRepo.findById(userId).ifPresent(profileRepo::delete);
    }

    @Transactional
    public void seed(Long userId) {
        reset(userId);

        PaycheckProfile p = new PaycheckProfile();
        p.setId(userId);
        p.setUserId(userId);
        p.setPayType("salary");
        p.setSalaryAmount(2150);
        p.setFrequency("biweekly");
        p.setContribution401kPercent(9);
        p.setEmployerMatchRate(50);
        p.setEmployerMatchLimit(6);
        profileRepo.save(p);

        addDeduction(userId, "Federal tax", 215.00);
        addDeduction(userId, "State tax", 65.00);
        addDeduction(userId, "Social Security + Medicare", 164.48);
        addDeduction(userId, "Health insurance", 80.00);

        LocalDate today = LocalDate.now();
        addExpense(userId, "Rent", "Housing", 1150.00, today.withDayOfMonth(1));
        addExpense(userId, "Whole Foods", "Groceries", 186.40, today.minusDays(2));
        addExpense(userId, "Corner Market", "Groceries", 94.25, today.minusDays(6));
        addExpense(userId, "Gas", "Transportation", 58.00, today.minusDays(4));
        addExpense(userId, "Electric bill", "Utilities", 112.30, today.minusDays(3));
        addExpense(userId, "Internet", "Utilities", 55.00, today.minusDays(5));
        addExpense(userId, "Dinner out", "Dining", 62.50, today.minusDays(1));
        addExpense(userId, "Clothes", "Shopping", 74.00, today.minusDays(7));

        addDebt(userId, "Store card", 600, 26, 25);
        addDebt(userId, "Credit card", 4200, 24, 90);
        addDebt(userId, "Car loan", 9800, 7, 220);

        addGoal(userId, "Emergency fund", 6000, 3200, today.plusMonths(8), 100);
        addGoal(userId, "Family trip", 1800, 350, today.plusMonths(6), 0);
    }

    private void addDeduction(Long userId, String name, double amount) {
        Deduction d = new Deduction();
        d.setUserId(userId);
        d.setName(name);
        d.setAmount(amount);
        deductionRepo.save(d);
    }

    private void addExpense(Long userId, String description, String category, double amount, LocalDate date) {
        Expense e = new Expense();
        e.setUserId(userId);
        e.setDescription(description);
        e.setCategory(category);
        e.setAmount(amount);
        e.setExpenseDate(date);
        expenseRepo.save(e);
    }

    private void addDebt(Long userId, String name, double balance, double apr, double minPayment) {
        Debt d = new Debt();
        d.setUserId(userId);
        d.setName(name);
        d.setBalance(balance);
        d.setApr(apr);
        d.setMinPayment(minPayment);
        debtRepo.save(d);
    }

    private void addGoal(Long userId, String name, double target, double saved, LocalDate date, double monthly) {
        Goal g = new Goal();
        g.setUserId(userId);
        g.setName(name);
        g.setTargetAmount(target);
        g.setSavedAmount(saved);
        g.setTargetDate(date);
        g.setMonthlyContribution(monthly);
        goalRepo.save(g);
    }
}
