package com.breathingroom.backend.controller;

import com.breathingroom.backend.dto.Dtos;
import com.breathingroom.backend.service.FinanceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/paycheck")
public class PaycheckController {

    private final FinanceService finance;

    public PaycheckController(FinanceService finance) {
        this.finance = finance;
    }

    @GetMapping
    public Dtos.PaycheckDto get() {
        return finance.paycheck();
    }

    @PutMapping
    public Dtos.PaycheckDto save(@RequestBody Dtos.PaycheckDto body) {
        finance.savePaycheck(body);
        return finance.paycheck();
    }
}
