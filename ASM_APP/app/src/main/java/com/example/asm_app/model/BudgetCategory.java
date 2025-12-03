package com.example.asm_app.model;

public class BudgetCategory {
    private final String name;
    private final double spent;
    private final Double limit;
    private final int colorRes;

    public BudgetCategory(String name, double spent, Double limit, int colorRes) {
        this.name = name;
        this.spent = spent;
        this.limit = limit;
        this.colorRes = colorRes;
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
