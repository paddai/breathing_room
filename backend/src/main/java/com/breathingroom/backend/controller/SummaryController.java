package com.breathingroom.backend.controller;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.security.CurrentUser;
import com.breathingroom.backend.service.FinanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final FinanceService finance;

    public SummaryController(FinanceService finance) {
        this.finance = finance;
    }

    @GetMapping
    public Dtos.SummaryDto summary() {
        return finance.summary(CurrentUser.id());
    }
}
