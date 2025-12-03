package com.example.asm_app.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.core.content.ContextCompat;

import com.example.asm_app.R;
import com.example.asm_app.database.SQLiteDbHelper;
import com.example.asm_app.model.BudgetCategory;
import com.example.asm_app.model.Category;
import com.example.asm_app.model.Expense;
import com.example.asm_app.model.RecurringExpense;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ExpenseRepository {
    private final SQLiteDbHelper dbHelper;
    private final long userId;
    private final Context appContext;

    public ExpenseRepository(Context context, long userId) {
        this.dbHelper = new SQLiteDbHelper(context);
        this.userId = userId;
        this.appContext = context.getApplicationContext();
    }

    public List<Category> getCategories() {
        List<Category> items = new ArrayList<>();
        if (userId <= 0) {
            return items;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT id, name, color, limitAmount FROM categories WHERE userId = ? ORDER BY name",
                new String[]{String.valueOf(userId)});
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            int color = cursor.getInt(2);
            Double limit = cursor.isNull(3) ? null : cursor.getDouble(3);
            items.add(new Category(id, name, color, limit));
        }
        cursor.close();
        return items;
    }

    public long addCategory(String name, int color, Double limit) {
        if (userId <= 0) {
            return -1;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userId", userId);
        values.put("name", name);
        values.put("color", color);
        if (limit != null) {
            values.put("limitAmount", limit);
        } else {
            values.putNull("limitAmount");
        }
        return db.insert("categories", null, values);
    }

    public List<Expense> getExpenses() {
        List<Expense> items = new ArrayList<>();
        if (userId <= 0) {
            return items;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT e.title, c.name, c.color, e.amount, e.dateMillis " +
                "FROM expenses e LEFT JOIN categories c ON e.categoryId = c.id " +
                "WHERE e.userId = ? ORDER BY e.dateMillis DESC";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
        while (cursor.moveToNext()) {
            String title = cursor.getString(0);
            String category = cursor.isNull(1) ? "Không phân loại" : cursor.getString(1);
            int color = cursor.isNull(2) ? defaultCategoryColor() : cursor.getInt(2);
            double amount = cursor.getDouble(3);
            long dateMillis = cursor.getLong(4);
            items.add(new Expense(title, category, amount, new Date(dateMillis), color));
        }
        cursor.close();
        return items;
    }

    public double getTotalExpenses() {
        if (userId <= 0) {
            return 0;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT IFNULL(SUM(amount), 0) FROM expenses WHERE userId = ?", new String[]{String.valueOf(userId)});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public List<BudgetCategory> getBudgets() {
        List<BudgetCategory> items = new ArrayList<>();
        if (userId <= 0) {
            return items;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT c.name, c.color, c.limitAmount, IFNULL(SUM(e.amount), 0) AS spent " +
                "FROM categories c LEFT JOIN expenses e ON e.categoryId = c.id " +
                "WHERE c.userId = ? " +
                "GROUP BY c.id, c.name, c.color, c.limitAmount " +
                "ORDER BY spent DESC";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
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

    public void addExpense(String title, long categoryId, double amount, Date date) {
        if (userId <= 0) {
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userId", userId);
        values.put("title", title);
        if (categoryId > 0) {
            values.put("categoryId", categoryId);
        } else {
            values.putNull("categoryId");
        }
        values.put("amount", amount);
        values.put("dateMillis", date.getTime());
        db.insert("expenses", null, values);
    }

    public List<RecurringExpense> getRecurring() {
        List<RecurringExpense> items = new ArrayList<>();
        if (userId <= 0) {
            return items;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT r.title, r.amount, c.name, r.startDateMillis " +
                "FROM recurring_expenses r LEFT JOIN categories c ON r.categoryId = c.id " +
                "WHERE r.userId = ? ORDER BY r.startDateMillis DESC";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
        while (cursor.moveToNext()) {
            String title = cursor.getString(0);
            double amount = cursor.getDouble(1);
            String category = cursor.isNull(2) ? "Không phân loại" : cursor.getString(2);
            long startDate = cursor.getLong(3);
            items.add(new RecurringExpense(title, amount, category, new Date(startDate)));
        }
        cursor.close();
        return items;
    }

    public void addRecurring(String title, double amount, long categoryId, Date startDate) {
        if (userId <= 0) {
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("userId", userId);
        values.put("title", title);
        values.put("amount", amount);
        if (categoryId > 0) {
            values.put("categoryId", categoryId);
        } else {
            values.putNull("categoryId");
        }
        values.put("startDateMillis", startDate.getTime());
        db.insert("recurring_expenses", null, values);
    }

    public void deleteAllUserData() {
        if (userId <= 0) {
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("recurring_expenses", "userId = ?", new String[]{String.valueOf(userId)});
        db.delete("expenses", "userId = ?", new String[]{String.valueOf(userId)});
        db.delete("categories", "userId = ?", new String[]{String.valueOf(userId)});
    }

    private int defaultCategoryColor() {
        return ContextCompat.getColor(appContext, R.color.gray_500);
    }
}
