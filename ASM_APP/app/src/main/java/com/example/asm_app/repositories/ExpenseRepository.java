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
import java.util.Calendar;
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

    public void ensureDefaultCategoriesIfEmpty() {
        if (userId <= 0) {
            return;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM categories WHERE userId = ?", new String[]{String.valueOf(userId)});
        long count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getLong(0);
        }
        cursor.close();
        if (count > 0) {
            return;
        }
        addCategory("Ăn uống", color(R.color.danger_red), 1_500_000d);
        addCategory("Đi lại", color(R.color.warning_yellow), 500_000d);
        addCategory("Đi chơi", color(R.color.blue_light), 800_000d);
        addCategory("Nhà & hoá đơn", color(R.color.gray_700), 1_200_000d);
        addCategory("Mua sắm", color(R.color.blue_600), 1_000_000d);
        addCategory("Sức khoẻ", color(R.color.teal), 600_000d);
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

    public double getRecurringPlannedForCurrentMonth() {
        if (userId <= 0) {
            return 0;
        }
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.set(java.util.Calendar.DAY_OF_MONTH, calendar.getActualMaximum(java.util.Calendar.DAY_OF_MONTH));
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 23);
        calendar.set(java.util.Calendar.MINUTE, 59);
        calendar.set(java.util.Calendar.SECOND, 59);
        calendar.set(java.util.Calendar.MILLISECOND, 999);
        long endOfMonth = calendar.getTimeInMillis();

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT IFNULL(SUM(amount), 0) FROM recurring_expenses WHERE userId = ? AND startDateMillis <= ?",
                new String[]{String.valueOf(userId), String.valueOf(endOfMonth)});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public double getTotalLimit() {
        if (userId <= 0) {
            return 0;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT IFNULL(SUM(limitAmount),0) FROM categories WHERE userId = ?",
                new String[]{String.valueOf(userId)});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public double getExpensesTotalBetween(long startMillis, long endMillis) {
        if (userId <= 0) {
            return 0;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT IFNULL(SUM(amount),0) FROM expenses WHERE userId = ? AND dateMillis BETWEEN ? AND ?",
                new String[]{String.valueOf(userId), String.valueOf(startMillis), String.valueOf(endMillis)});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public double getRecurringTotalBetween(long startMillis, long endMillis) {
        if (userId <= 0) {
            return 0;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT IFNULL(SUM(amount),0) FROM recurring_expenses WHERE userId = ? AND startDateMillis BETWEEN ? AND ?",
                new String[]{String.valueOf(userId), String.valueOf(startMillis), String.valueOf(endMillis)});
        double total = 0;
        if (cursor.moveToFirst()) {
            total = cursor.getDouble(0);
        }
        cursor.close();
        return total;
    }

    public List<CategorySpend> getCategorySpendBetween(long startMillis, long endMillis) {
        List<CategorySpend> items = new ArrayList<>();
        if (userId <= 0) return items;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT c.id, c.name, c.color, c.limitAmount, IFNULL(SUM(e.amount),0) " +
                "FROM categories c " +
                "LEFT JOIN expenses e ON e.categoryId = c.id AND e.dateMillis BETWEEN ? AND ? " +
                "WHERE c.userId = ? " +
                "GROUP BY c.id, c.name, c.color, c.limitAmount";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(startMillis), String.valueOf(endMillis), String.valueOf(userId)});
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            int color = cursor.getInt(2);
            Double limit = cursor.isNull(3) ? null : cursor.getDouble(3);
            double spent = cursor.getDouble(4);
            items.add(new CategorySpend(id, name, color, limit, spent));
        }
        cursor.close();
        return items;
    }

    public int countMonthsExceededInYear(int year) {
        if (userId <= 0) return 0;
        int exceeded = 0;
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        for (int month = 0; month < 12; month++) {
            cal.set(Calendar.MONTH, month);
            long start = cal.getTimeInMillis();
            cal.add(Calendar.MONTH, 1);
            cal.add(Calendar.MILLISECOND, -1);
            long end = cal.getTimeInMillis();
            cal.add(Calendar.MILLISECOND, 1);

            double limit = getTotalLimit();
            double recurring = getRecurringTotalBetween(start, end);
            double spent = getExpensesTotalBetween(start, end) + recurring;
            if (spent > limit + recurring) {
                exceeded++;
            }
        }
        return exceeded;
    }

    private int defaultCategoryColor() {
        return ContextCompat.getColor(appContext, R.color.gray_500);
    }

    private int color(int resId) {
        return ContextCompat.getColor(appContext, resId);
    }

    public static class CategorySpend {
        public final long id;
        public final String name;
        public final int color;
        public final Double limit;
        public final double spent;

        public CategorySpend(long id, String name, int color, Double limit, double spent) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.limit = limit;
            this.spent = spent;
        }
    }
}
