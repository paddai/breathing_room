package com.breathingroom.backend.model;

import jakarta.persistence.*;

@Entity
public class Debt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private double balance;

    private double apr;

    private double minPayment;

    public Debt() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public double getApr() { return apr; }
    public void setApr(double apr) { this.apr = apr; }

    public double getMinPayment() { return minPayment; }
    public void setMinPayment(double minPayment) { this.minPayment = minPayment; }

}
