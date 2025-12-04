package com.example.asm_app.model;

public class BudgetCategory {
    private final long id;
    private final String name;
    private final double spent;
    private final Double limit;
    private final int colorRes;

    public BudgetCategory(long id, String name, double spent, Double limit, int colorRes) {
        this.id = id;
        this.name = name;
        this.spent = spent;
        this.limit = limit;
        this.colorRes = colorRes;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getSpent() {
        return spent;
    }

    public Double getLimit() {
        return limit;
    }

    public int getColorRes() {
        return colorRes;
    }
}
