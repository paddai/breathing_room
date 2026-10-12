package com.breathingroom.backend.repository;

import com.breathingroom.backend.model.Deduction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeductionRepository extends JpaRepository<Deduction, Long> {
}
