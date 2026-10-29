package com.breathingroom.backend.controller;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.security.CurrentUser;
import com.breathingroom.backend.service.DecisionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class DecisionController {

    private final DecisionService decisions;

    public DecisionController(DecisionService decisions) {
        this.decisions = decisions;
    }

    /** Snowball vs avalanche vs minimums only. Send {extra} to use your saved debts, or {extra, debts} to try sample debts. */
    @PostMapping("/debts/payoff")
    public Dtos.PayoffComparison payoff(@RequestBody(required = false) Dtos.PayoffRequest body) {
        return decisions.payoff(CurrentUser.id(), body);
    }

    /** Current plan vs lowering the 401(k) to the employer match and redirecting the difference to debt. */
    @GetMapping("/recommendation/scenario")
    public Dtos.ScenarioDto scenario(@RequestParam(defaultValue = "100") double afterTax) {
        return decisions.scenario(CurrentUser.id(), afterTax);
    }
}
