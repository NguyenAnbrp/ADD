package com.example.asm_app.model;

import java.util.Date;

public class RecurringExpense {
    private final String title;
    private final double amount;
    private final String category;
    private final Date startDate;

    public RecurringExpense(String title, double amount, String category, Date startDate) {
        this.title = title;
        this.amount = amount;
        this.category = category;
        this.startDate = startDate;
    }

    public String getTitle() {
        return title;
    }

    public double getAmount() {
        return amount;
    }

    public String getCategory() {
        return category;
    }

    public Date getStartDate() {
        return startDate;
    }
}
