package com.breathingroom.backend.controller;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.model.GoalDecision;
import com.breathingroom.backend.security.CurrentUser;
import com.breathingroom.backend.service.FinanceService;
import com.breathingroom.backend.service.GoalService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class GoalController {

    private final GoalService goals;
    private final FinanceService finance;

    public GoalController(GoalService goals, FinanceService finance) {
        this.goals = goals;
        this.finance = finance;
    }

    @GetMapping("/goals")
    public List<Dtos.GoalView> list() {
        return finance.goalViews(CurrentUser.id());
    }

    @PostMapping("/goals")
    public Dtos.GoalView create(@RequestBody Dtos.GoalInput body) {
        return goals.create(CurrentUser.id(), body);
    }

    @DeleteMapping("/goals/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        goals.delete(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/goals/{id}/savings")
    public Dtos.GoalView addSavings(@PathVariable Long id, @RequestBody Dtos.AddSavingsRequest body) {
        return goals.addSavings(CurrentUser.id(), id, body.amount());
    }

    /** Java builds the options; AI (if configured) picks one and explains. Nothing changes here. */
    @GetMapping("/goals/{id}/advice")
    public Dtos.GoalAdvice advice(@PathVariable Long id) {
        return goals.advice(CurrentUser.id(), id);
    }

    /** The ONLY way advice changes a goal: the signed-in user approves an option. */
    @PostMapping("/goals/{id}/decision")
    public Dtos.GoalView decide(@PathVariable Long id, @RequestBody Dtos.DecisionRequest body) {
        return goals.decide(CurrentUser.id(), id, body);
    }

    @GetMapping("/goal-decisions")
    public List<GoalDecision> decisions() {
        return goals.recentDecisions(CurrentUser.id());
    }
}
