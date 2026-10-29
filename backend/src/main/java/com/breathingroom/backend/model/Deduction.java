package com.breathingroom.backend.model;

import jakarta.persistence.*;

@Entity
public class Deduction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private double amount;

    private Long userId;

    public Deduction() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

}
