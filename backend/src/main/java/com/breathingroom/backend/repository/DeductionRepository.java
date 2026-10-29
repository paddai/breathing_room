package com.breathingroom.backend.repository;

import com.breathingroom.backend.model.Deduction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeductionRepository extends JpaRepository<Deduction, Long> {
    List<Deduction> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
