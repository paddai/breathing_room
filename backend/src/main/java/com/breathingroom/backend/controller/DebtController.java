package com.breathingroom.backend.controller;

import com.breathingroom.backend.model.Debt;
import com.breathingroom.backend.repository.DebtRepository;
import com.breathingroom.backend.security.CurrentUser;
import com.breathingroom.backend.validation.FieldValidationException;
import com.breathingroom.backend.service.FinanceService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/debts")
public class DebtController {

    private final DebtRepository repo;

    public DebtController(DebtRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Debt> list() {
        List<Debt> all = new ArrayList<>(repo.findByUserId(CurrentUser.id()));
        all.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        return all;
    }

    @PostMapping
    public Debt create(@RequestBody Debt body) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (body.getName() == null || body.getName().isBlank()) errors.put("name", "Give this debt a name, for example \"Visa card\".");
        else if (body.getName().trim().length() > 60) errors.put("name", "Use 60 characters or fewer.");
        if (body.getBalance() <= 0) errors.put("balance", "Enter the balance you still owe.");
        else if (body.getBalance() > 100_000_000) errors.put("balance", "That balance is too large.");
        if (body.getApr() < 0 || body.getApr() > 100) errors.put("apr", "Enter the interest rate (APR) between 0 and 100. Use 0 if there is no interest.");
        if (body.getMinPayment() < 0) errors.put("minPayment", "The minimum payment cannot be negative.");
        else if (body.getMinPayment() > 10_000_000) errors.put("minPayment", "That payment is too large.");
        if (!errors.isEmpty()) throw new FieldValidationException(errors);
        Debt d = new Debt();
        d.setUserId(CurrentUser.id());
        d.setName(FinanceService.clip(body.getName(), 60));
        d.setBalance(body.getBalance());
        d.setApr(body.getApr());
        d.setMinPayment(body.getMinPayment());
        return repo.save(d);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.findByIdAndUserId(id, CurrentUser.id()).ifPresent(repo::delete);
        return ResponseEntity.noContent().build();
    }
}
