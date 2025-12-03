package com.example.asm_app.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.asm_app.database.SQLiteDbHelper;
import com.example.asm_app.model.BudgetCategory;
import com.example.asm_app.model.Expense;
import com.example.asm_app.model.RecurringExpense;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExpenseRepository {
    private final SQLiteDbHelper dbHelper;

    public ExpenseRepository(Context context) {
        this.dbHelper = new SQLiteDbHelper(context);
    }

    public List<Expense> getExpenses() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT title, categoryName, categoryColor, amount, dateMillis FROM expenses ORDER BY dateMillis DESC", null);
        List<Expense> items = new ArrayList<>();
        while (cursor.moveToNext()) {
            String title = cursor.getString(0);
            String category = cursor.getString(1);
            int color = cursor.getInt(2);
            double amount = cursor.getDouble(3);
            long dateMillis = cursor.getLong(4);
            items.add(new Expense(title, category, amount, new Date(dateMillis), color));
        }
        cursor.close();
        return items;
    }

    public double getTotalExpenses() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT IFNULL(SUM(amount), 0) FROM expenses", null);
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public List<BudgetCategory> getBudgets() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT c.name, c.color, c.limitAmount, IFNULL(SUM(e.amount), 0) AS spent " +
                "FROM categories c LEFT JOIN expenses e ON e.categoryName = c.name " +
                "GROUP BY c.name, c.color, c.limitAmount " +
                "ORDER BY spent DESC";
        Cursor cursor = db.rawQuery(sql, null);
        List<BudgetCategory> items = new ArrayList<>();
        while (cursor.moveToNext()) {
            String name = cursor.getString(0);
            int color = cursor.getInt(1);
            Double limit = cursor.isNull(2) ? null : cursor.getDouble(2);
            double spent = cursor.getDouble(3);
            items.add(new BudgetCategory(name, spent, limit, color));
        }
        cursor.close();
        return items;
    }

    public void addExpense(String title, String category, int categoryColor, double amount, Date date) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("categoryName", category);
        values.put("categoryColor", categoryColor);
        values.put("amount", amount);
        values.put("dateMillis", date.getTime());
        db.insert("expenses", null, values);
    }

    public List<RecurringExpense> getRecurring() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT title, amount, categoryName, startDateMillis FROM recurring_expenses ORDER BY startDateMillis DESC", null);
        List<RecurringExpense> items = new ArrayList<>();
        while (cursor.moveToNext()) {
            String title = cursor.getString(0);
            double amount = cursor.getDouble(1);
            String category = cursor.getString(2);
            long startDate = cursor.getLong(3);
            items.add(new RecurringExpense(title, amount, category, new Date(startDate)));
        }
        cursor.close();
        return items;
    }

    public void addRecurring(String title, double amount, String category, Date startDate) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("amount", amount);
        values.put("categoryName", category);
        values.put("startDateMillis", startDate.getTime());
        db.insert("recurring_expenses", null, values);
    }
}
