package com.breathingroom.backend.repository;

import com.breathingroom.backend.model.Debt;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtRepository extends JpaRepository<Debt, Long> {
}
