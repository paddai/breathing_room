package com.breathingroom.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class GoalDecision {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long goalId;

    private String goalName;

    private String optionId;

    @Column(length = 400)
    private String label;

    private String status;

    private LocalDateTime createdAt = LocalDateTime.now();

    private Long userId;

    public GoalDecision() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGoalId() { return goalId; }
    public void setGoalId(Long goalId) { this.goalId = goalId; }

    public String getGoalName() { return goalName; }
    public void setGoalName(String goalName) { this.goalName = goalName; }

    public String getOptionId() { return optionId; }
    public void setOptionId(String optionId) { this.optionId = optionId; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

}
