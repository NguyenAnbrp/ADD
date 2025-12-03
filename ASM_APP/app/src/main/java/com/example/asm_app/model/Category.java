package com.example.asm_app.model;

public class Category {
    private final long id;
    private final String name;
    private final int color;
    private final Double limit;

    public Category(long id, String name, int color, Double limit) {
        this.id = id;
        this.name = name;
        this.color = color;
        this.limit = limit;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getColor() {
        return color;
    }

    public Double getLimit() {
        return limit;
    }
}
