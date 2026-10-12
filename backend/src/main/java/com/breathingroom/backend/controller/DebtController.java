package com.breathingroom.backend.controller;

import com.breathingroom.backend.model.Debt;
import com.breathingroom.backend.repository.DebtRepository;
import com.breathingroom.backend.service.FinanceService;
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
@RequestMapping("/api/debts")
public class DebtController {

    private final DebtRepository repo;

    public DebtController(DebtRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public List<Debt> list() {
        List<Debt> all = new ArrayList<>(repo.findAll());
        all.sort((a, b) -> Long.compare(a.getId(), b.getId()));
        return all;
    }

    @PostMapping
    public Debt create(@RequestBody Debt body) {
        if (body.getName() == null || body.getName().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give the debt a name.");
        }
        if (body.getBalance() <= 0 || body.getBalance() > 100_000_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Balance must be greater than 0.");
        }
        if (body.getApr() < 0 || body.getApr() > 100 || body.getMinPayment() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "APR must be 0-100 and the minimum payment cannot be negative.");
        }
        Debt d = new Debt();
        d.setName(FinanceService.clip(body.getName(), 60));
        d.setBalance(body.getBalance());
        d.setApr(body.getApr());
        d.setMinPayment(body.getMinPayment());
        return repo.save(d);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
