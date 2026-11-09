package com.breathingroom.backend.repository;

import com.breathingroom.backend.model.Debt;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtRepository extends JpaRepository<Debt, Long> {
    List<Debt> findByUserId(Long userId);

    Optional<Debt> findByIdAndUserId(Long id, Long userId);

    void deleteByUserId(Long userId);
}
