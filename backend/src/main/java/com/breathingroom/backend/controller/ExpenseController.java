package com.breathingroom.backend.controller;

import com.breathingroom.backend.model.Expense;
import com.breathingroom.backend.repository.ExpenseRepository;
import com.breathingroom.backend.service.FinanceService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
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
        List<Expense> all = new ArrayList<>(repo.findAll());
        all.sort((a, b) -> Long.compare(b.getId(), a.getId())); // newest first
        return all;
    }

    @PostMapping
    public Expense create(@RequestBody Expense body) {
        if (body.getDescription() == null || body.getDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Add a description.");
        }
        if (body.getAmount() <= 0 || body.getAmount() > 10_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be greater than 0.");
        }
        Expense e = new Expense(); // build a fresh object so the client can never set the id
        e.setDescription(FinanceService.clip(body.getDescription(), 120));
        e.setCategory(body.getCategory() == null || body.getCategory().isBlank() ? "Other" : FinanceService.clip(body.getCategory(), 40));
        e.setAmount(body.getAmount());
        e.setExpenseDate(body.getExpenseDate() == null ? LocalDate.now() : body.getExpenseDate());
        return repo.save(e);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
