package com.example.asm_app.model;

import java.util.Date;

public class Expense {
    private final String title;
    private final String category;
    private final double amount;
    private final Date date;
    private final int colorRes;

    public Expense(String title, String category, double amount, Date date, int colorRes) {
        this.title = title;
        this.category = category;
        this.amount = amount;
        this.date = date;
        this.colorRes = colorRes;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }

    public double getAmount() {
        return amount;
    }

    public Date getDate() {
        return date;
    }

    public int getColorRes() {
        return colorRes;
    }
}
