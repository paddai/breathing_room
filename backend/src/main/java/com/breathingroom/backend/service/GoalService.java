package com.breathingroom.backend.service;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.logic.GoalMath;
import com.breathingroom.backend.model.Goal;
import com.breathingroom.backend.model.GoalDecision;
import com.breathingroom.backend.repository.GoalDecisionRepository;
import com.breathingroom.backend.repository.GoalRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * Goal changes. The semi-autonomous rule lives here: the AI can SUGGEST, but data only changes when
 * the user approves an option, and the app re-checks that option itself (it never trusts numbers from the browser).
 */
@Service
public class GoalService {

    private final GoalRepository goalRepo;
    private final GoalDecisionRepository decisionRepo;
    private final FinanceService finance;
    private final AdvisorService advisor;

    public GoalService(GoalRepository goalRepo, GoalDecisionRepository decisionRepo, FinanceService finance,
                       AdvisorService advisor) {
        this.goalRepo = goalRepo;
        this.decisionRepo = decisionRepo;
        this.finance = finance;
        this.advisor = advisor;
    }

    private static ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private Goal find(Long userId, Long id) {
        return goalRepo.findByIdAndUserId(id, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Goal not found."));
    }

    @Transactional
    public Dtos.GoalView create(Long userId, Dtos.GoalInput in) {
        if (in == null || in.name() == null || in.name().isBlank()) throw bad("Give the goal a name.");
        if (in.targetAmount() <= 0 || in.targetAmount() > 100_000_000) throw bad("Target amount must be greater than 0.");
        if (in.savedAmount() < 0 || in.monthlyContribution() < 0) throw bad("Amounts cannot be negative.");
        LocalDate date;
        try {
            date = LocalDate.parse(in.targetDate());
        } catch (DateTimeParseException | NullPointerException e) {
            throw bad("Pick a valid target date.");
        }
        Goal g = new Goal();
        g.setUserId(userId);
        g.setName(FinanceService.clip(in.name(), 60));
        g.setTargetAmount(in.targetAmount());
        g.setSavedAmount(in.savedAmount());
        g.setTargetDate(date);
        g.setMonthlyContribution(in.monthlyContribution());
        return finance.goalView(goalRepo.save(g));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        goalRepo.delete(find(userId, id));
    }

    @Transactional
    public Dtos.GoalView addSavings(Long userId, Long id, double amount) {
        if (amount <= 0 || amount > 1_000_000) throw bad("Enter an amount between $0.01 and $1,000,000.");
        Goal g = find(userId, id);
        g.setSavedAmount(g.getSavedAmount() + amount);
        return finance.goalView(goalRepo.save(g));
    }

    private List<GoalMath.Option> optionsFor(Goal g) {
        return GoalMath.options(g.getTargetAmount(), g.getSavedAmount(), g.getTargetDate(),
                g.getMonthlyContribution(), finance.availableForGoal(g), LocalDate.now());
    }

    public Dtos.GoalAdvice advice(Long userId, Long id) {
        Goal g = find(userId, id);
        Dtos.GoalView view = finance.goalView(g);
        double surplus = Math.max(0, finance.summary(userId).monthlySurplus());
        double available = finance.availableForGoal(g);
        List<GoalMath.Option> options = optionsFor(g);
        AdvisorService.GoalAdviceText text = advisor.explainGoal(userId, view, surplus, available, options);
        return new Dtos.GoalAdvice(view, GoalMath.round2(surplus), GoalMath.round2(available), options,
                text.choiceId(), text.explanation(), text.aiUsed(), text.note());
    }

    @Transactional
    public Dtos.GoalView decide(Long userId, Long id, Dtos.DecisionRequest req) {
        if (req == null || req.optionId() == null || req.status() == null) throw bad("Missing decision.");
        String status = req.status().toUpperCase(Locale.ROOT);
        if (!status.equals("APPROVED") && !status.equals("DECLINED")) throw bad("Status must be APPROVED or DECLINED.");

        Goal g = find(userId, id);
        GoalMath.Option chosen = null;
        for (GoalMath.Option o : optionsFor(g)) if (o.id().equals(req.optionId())) chosen = o;
        if (chosen == null) throw bad("That option is no longer available. Refresh the advice.");

        if (status.equals("APPROVED")) {
            g.setMonthlyContribution(chosen.monthlyAmount());
            if (chosen.newTargetDate() != null) g.setTargetDate(LocalDate.parse(chosen.newTargetDate()));
            goalRepo.save(g);
        }
        GoalDecision d = new GoalDecision();
        d.setUserId(userId);
        d.setGoalId(g.getId());
        d.setGoalName(g.getName());
        d.setOptionId(chosen.id());
        d.setLabel(chosen.label());
        d.setStatus(status);
        d.setCreatedAt(LocalDateTime.now());
        decisionRepo.save(d);
        return finance.goalView(g);
    }

    public List<GoalDecision> recentDecisions(Long userId) {
        List<GoalDecision> all = new ArrayList<>(decisionRepo.findByUserId(userId));
        all.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        return new ArrayList<>(all.subList(0, Math.min(15, all.size())));
    }
}
