package com.breathingroom.backend.controller;

import com.breathingroom.backend.model.Expense;
import com.breathingroom.backend.repository.ExpenseRepository;
import com.breathingroom.backend.security.CurrentUser;
import com.breathingroom.backend.validation.FieldValidationException;
import com.breathingroom.backend.service.FinanceService;
import java.time.LocalDate;
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
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final ExpenseRepository repo;

    public ExpenseController(ExpenseRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Expense> list() {
        List<Expense> all = new ArrayList<>(repo.findByUserId(CurrentUser.id()));
        all.sort((a, b) -> Long.compare(b.getId(), a.getId())); // newest first
        return all;
    }

    @PostMapping
    public Expense create(@RequestBody Expense body) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (body.getDescription() == null || body.getDescription().isBlank()) errors.put("description", "Enter a description, for example \"Groceries at Aldi\".");
        else if (body.getDescription().trim().length() > 120) errors.put("description", "Use 120 characters or fewer.");
        if (body.getCategory() == null || body.getCategory().isBlank()) errors.put("category", "Choose a category.");
        if (body.getAmount() <= 0) errors.put("amount", "Enter an amount greater than 0.");
        else if (body.getAmount() > 10_000_000) errors.put("amount", "That amount is too large.");
        if (!errors.isEmpty()) throw new FieldValidationException(errors);
        Expense e = new Expense(); // fresh object: the browser can never choose the id or the owner
        e.setUserId(CurrentUser.id());
        e.setDescription(FinanceService.clip(body.getDescription(), 120));
        e.setCategory(FinanceService.clip(body.getCategory(), 40));
        e.setAmount(body.getAmount());
        e.setExpenseDate(body.getExpenseDate() == null ? LocalDate.now() : body.getExpenseDate());
        return repo.save(e);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.findByIdAndUserId(id, CurrentUser.id()).ifPresent(repo::delete); // someone else's row is simply not found
        return ResponseEntity.noContent().build();
    }
}
