package com.breathingroom.backend.repository;

import com.breathingroom.backend.model.GoalDecision;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GoalDecisionRepository extends JpaRepository<GoalDecision, Long> {
    List<GoalDecision> findByUserId(Long userId);

    void deleteByUserId(Long userId);
}
