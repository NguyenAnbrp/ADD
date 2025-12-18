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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
//        addCategory("Food & Drinks", color(R.color.danger_red), null);
//        addCategory("Transport", color(R.color.warning_yellow), null);
//        addCategory("Entertainment", color(R.color.blue_light), null);
//        addCategory("Home & Bills", color(R.color.gray_700), null);
//        addCategory("Shopping", color(R.color.blue_600), null);
//        addCategory("Health", color(R.color.teal), null);
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
            String category = cursor.isNull(1) ? "Uncategorized" : cursor.getString(1);
            int color = cursor.isNull(2) ? defaultCategoryColor() : cursor.getInt(2);
            double amount = cursor.getDouble(3);
            long dateMillis = cursor.getLong(4);
            items.add(new Expense(title, category, amount, new Date(dateMillis), color));
        }
        cursor.close();
        return items;
    }

    public List<Expense> getExpensesByMonth(int year, int month) {
        List<Expense> items = new ArrayList<>();
        if (userId <= 0) {
            return items;
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long startMillis = cal.getTimeInMillis();
        
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.MILLISECOND, -1);
        long endMillis = cal.getTimeInMillis();
        
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT e.title, c.name, c.color, e.amount, e.dateMillis " +
                "FROM expenses e LEFT JOIN categories c ON e.categoryId = c.id " +
                "WHERE e.userId = ? AND e.dateMillis BETWEEN ? AND ? ORDER BY e.dateMillis DESC";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), String.valueOf(startMillis), String.valueOf(endMillis)});
        while (cursor.moveToNext()) {
            String title = cursor.getString(0);
            String category = cursor.isNull(1) ? "Uncategorized" : cursor.getString(1);
            int color = cursor.isNull(2) ? defaultCategoryColor() : cursor.getInt(2);
            double amount = cursor.getDouble(3);
            long dateMillis = cursor.getLong(4);
            items.add(new Expense(title, category, amount, new Date(dateMillis), color));
        }
        cursor.close();
        return items;
    }

    public void processRecurringExpensesForMonth(int year, int month) {
        if (userId <= 0) {
            return;
        }
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long monthStartMillis = cal.getTimeInMillis();
        
        cal.add(Calendar.MONTH, 1);
        cal.add(Calendar.MILLISECOND, -1);
        long monthEndMillis = cal.getTimeInMillis();
        
        // Get all recurring expenses that should be active in this month
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT id, title, amount, categoryId, startDateMillis FROM recurring_expenses " +
                "WHERE userId = ? AND startDateMillis <= ?";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId), String.valueOf(monthEndMillis)});
        
        List<RecurringExpenseData> recurringList = new ArrayList<>();
        while (cursor.moveToNext()) {
            long recurringId = cursor.getLong(0);
            String title = cursor.getString(1);
            double amount = cursor.getDouble(2);
            long categoryId = cursor.isNull(3) ? -1 : cursor.getLong(3);
            long startDateMillis = cursor.getLong(4);
            recurringList.add(new RecurringExpenseData(recurringId, title, amount, categoryId, startDateMillis));
        }
        cursor.close();
        
        // Check if each recurring expense has been added for this month
        for (RecurringExpenseData recurring : recurringList) {
            // Check if this recurring expense already exists in this month
            // Check by title, amount, and category to ensure exact match
            String checkSql = "SELECT COUNT(*) FROM expenses " +
                    "WHERE userId = ? AND title = ? AND amount = ? AND dateMillis BETWEEN ? AND ?";
            String[] checkArgs;
            if (recurring.categoryId > 0) {
                // If category is set, also check categoryId
                checkSql = "SELECT COUNT(*) FROM expenses " +
                        "WHERE userId = ? AND title = ? AND amount = ? AND categoryId = ? AND dateMillis BETWEEN ? AND ?";
                checkArgs = new String[]{
                        String.valueOf(userId),
                        recurring.title,
                        String.valueOf(recurring.amount),
                        String.valueOf(recurring.categoryId),
                        String.valueOf(monthStartMillis),
                        String.valueOf(monthEndMillis)
                };
            } else {
                // If no category, check that categoryId is NULL
                checkSql = "SELECT COUNT(*) FROM expenses " +
                        "WHERE userId = ? AND title = ? AND amount = ? AND categoryId IS NULL AND dateMillis BETWEEN ? AND ?";
                checkArgs = new String[]{
                        String.valueOf(userId),
                        recurring.title,
                        String.valueOf(recurring.amount),
                        String.valueOf(monthStartMillis),
                        String.valueOf(monthEndMillis)
                };
            }
            
            Cursor checkCursor = db.rawQuery(checkSql, checkArgs);
            boolean exists = false;
            if (checkCursor.moveToFirst()) {
                exists = checkCursor.getLong(0) > 0;
            }
            checkCursor.close();
            
            // If not exists, add it on the first day of the month
            if (!exists) {
                Calendar expenseDate = Calendar.getInstance();
                expenseDate.set(Calendar.YEAR, year);
                expenseDate.set(Calendar.MONTH, month);
                expenseDate.set(Calendar.DAY_OF_MONTH, 1);
                expenseDate.set(Calendar.HOUR_OF_DAY, 0);
                expenseDate.set(Calendar.MINUTE, 0);
                expenseDate.set(Calendar.SECOND, 0);
                expenseDate.set(Calendar.MILLISECOND, 0);
                
                addExpense(recurring.title, recurring.categoryId, recurring.amount, expenseDate.getTime(), recurring.id);
            }
        }
    }
    
    private static class RecurringExpenseData {
        final long id;
        final String title;
        final double amount;
        final long categoryId;
        final long startDateMillis;
        
        RecurringExpenseData(long id, String title, double amount, long categoryId, long startDateMillis) {
            this.id = id;
            this.title = title;
            this.amount = amount;
            this.categoryId = categoryId;
            this.startDateMillis = startDateMillis;
        }
    }

    public double getTotalExpenses() {
        if (userId <= 0) {
            return 0;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // Only count expenses that are NOT from recurring_expenses (recurringExpenseId IS NULL)
        // This prevents double counting since recurring expenses are already counted separately
        Cursor cursor = db.rawQuery("SELECT IFNULL(SUM(amount), 0) FROM expenses WHERE userId = ? AND recurringExpenseId IS NULL", new String[]{String.valueOf(userId)});
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
        // Only count expenses that are NOT from recurring_expenses (recurringExpenseId IS NULL)
        // This prevents double counting since recurring expenses are already counted separately
        String sql = "SELECT c.id, c.name, c.color, c.limitAmount, IFNULL(SUM(e.amount), 0) AS spent " +
                "FROM categories c LEFT JOIN expenses e ON e.categoryId = c.id AND e.recurringExpenseId IS NULL " +
                "WHERE c.userId = ? " +
                "GROUP BY c.id, c.name, c.color, c.limitAmount " +
                "ORDER BY spent DESC";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(userId)});
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            int color = cursor.getInt(2);
            Double limit = cursor.isNull(3) ? null : cursor.getDouble(3);
            double spent = cursor.getDouble(4);
            items.add(new BudgetCategory(id, name, spent, limit, color));
        }
        cursor.close();
        return items;
    }

    public void addExpense(String title, long categoryId, double amount, Date date) {
        addExpense(title, categoryId, amount, date, -1);
    }

    public void addExpense(String title, long categoryId, double amount, Date date, long recurringExpenseId) {
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
        if (recurringExpenseId > 0) {
            values.put("recurringExpenseId", recurringExpenseId);
        } else {
            values.putNull("recurringExpenseId");
        }
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
            String category = cursor.isNull(2) ? "Uncategorized" : cursor.getString(2);
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

    public boolean updateCategoryLimit(long categoryId, Double limit) {
        if (userId <= 0 || categoryId <= 0) {
            return false;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        if (limit != null && limit > 0) {
            values.put("limitAmount", limit);
        } else {
            values.putNull("limitAmount");
        }
        int rowsAffected = db.update("categories", values, "id = ? AND userId = ?", 
                new String[]{String.valueOf(categoryId), String.valueOf(userId)});
        return rowsAffected > 0;
    }

    public boolean deleteCategory(long categoryId) {
        if (userId <= 0 || categoryId <= 0) {
            return false;
        }
        if (hasExpensesForCategory(categoryId)) {
            return false;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Foreign key constraints will handle related expenses (SET NULL)
        int rowsAffected = db.delete("categories", "id = ? AND userId = ?", 
                new String[]{String.valueOf(categoryId), String.valueOf(userId)});
        return rowsAffected > 0;
    }

    public boolean hasExpensesForCategory(long categoryId) {
        if (userId <= 0 || categoryId <= 0) {
            return false;
        }
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM expenses WHERE userId = ? AND categoryId = ?",
                new String[]{String.valueOf(userId), String.valueOf(categoryId)}
        );
        boolean hasExpenses = false;
        if (cursor.moveToFirst()) {
            hasExpenses = cursor.getLong(0) > 0;
        }
        cursor.close();
        return hasExpenses;
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
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
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
        // Only count expenses that are NOT from recurring_expenses (recurringExpenseId IS NULL)
        // This prevents double counting since recurring expenses are already counted separately
        Cursor cursor = db.rawQuery("SELECT IFNULL(SUM(amount),0) FROM expenses WHERE userId = ? AND dateMillis BETWEEN ? AND ? AND recurringExpenseId IS NULL",
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

        Map<Long, CategorySpendAccumulator> accumulatorMap = new HashMap<>();
        String sql = "SELECT c.id, c.name, c.color, c.limitAmount, IFNULL(SUM(e.amount),0) " +
                "FROM categories c " +
                "LEFT JOIN expenses e ON e.categoryId = c.id AND e.dateMillis BETWEEN ? AND ? AND e.recurringExpenseId IS NULL " +
                "WHERE c.userId = ? " +
                "GROUP BY c.id, c.name, c.color, c.limitAmount";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(startMillis), String.valueOf(endMillis), String.valueOf(userId)});
        while (cursor.moveToNext()) {
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            int color = cursor.getInt(2);
            Double limit = cursor.isNull(3) ? null : cursor.getDouble(3);
            double spent = cursor.getDouble(4);
            CategorySpendAccumulator acc = new CategorySpendAccumulator(id, name, color, limit);
            acc.add(spent);
            accumulatorMap.put(id, acc);
        }
        cursor.close();

        double uncategorizedRecurring = 0;
        String recurringSql = "SELECT categoryId, IFNULL(SUM(amount),0) FROM recurring_expenses " +
                "WHERE userId = ? AND startDateMillis BETWEEN ? AND ? " +
                "GROUP BY categoryId";
        Cursor recurringCursor = db.rawQuery(recurringSql, new String[]{
                String.valueOf(userId),
                String.valueOf(startMillis),
                String.valueOf(endMillis)
        });
        while (recurringCursor.moveToNext()) {
            double amount = recurringCursor.getDouble(1);
            if (amount <= 0) continue;
            if (!recurringCursor.isNull(0)) {
                long categoryId = recurringCursor.getLong(0);
                CategorySpendAccumulator acc = accumulatorMap.get(categoryId);
                if (acc == null) {
                    acc = new CategorySpendAccumulator(
                            categoryId,
                            "Recurring",
                            defaultCategoryColor(),
                            null
                    );
                    accumulatorMap.put(categoryId, acc);
                }
                acc.add(amount);
            } else {
                uncategorizedRecurring += amount;
            }
        }
        recurringCursor.close();

        for (CategorySpendAccumulator acc : accumulatorMap.values()) {
            items.add(acc.toCategorySpend());
        }
        if (uncategorizedRecurring > 0) {
            items.add(new CategorySpend(
                    -1,
                    "Recurring (no category)",
                    defaultCategoryColor(),
                    null,
                    uncategorizedRecurring
            ));
        }
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

    public List<String> getCategoriesExceededBetween(long startMillis, long endMillis) {
        List<String> exceeded = new ArrayList<>();
        if (userId <= 0) return exceeded;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        // Only count expenses that are NOT from recurring_expenses (recurringExpenseId IS NULL)
        // This prevents double counting since recurring expenses are already counted separately
        String sql = "SELECT c.name, c.limitAmount, IFNULL(SUM(e.amount),0) AS spent " +
                "FROM categories c LEFT JOIN expenses e ON e.categoryId = c.id AND e.dateMillis BETWEEN ? AND ? AND e.recurringExpenseId IS NULL " +
                "WHERE c.userId = ? AND c.limitAmount IS NOT NULL AND c.limitAmount > 0 " +
                "GROUP BY c.id, c.name, c.limitAmount " +
                "HAVING spent > c.limitAmount";
        Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(startMillis), String.valueOf(endMillis), String.valueOf(userId)});
        while (cursor.moveToNext()) {
            exceeded.add(cursor.getString(0));
        }
        cursor.close();
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

    private static class CategorySpendAccumulator {
        final long id;
        final String name;
        final int color;
        final Double limit;
        double spent;

        CategorySpendAccumulator(long id, String name, int color, Double limit) {
            this.id = id;
            this.name = name;
            this.color = color;
            this.limit = limit;
            this.spent = 0;
        }

        void add(double amount) {
            this.spent += amount;
        }

        CategorySpend toCategorySpend() {
            return new CategorySpend(id, name, color, limit, spent);
        }
    }
    // Thêm vào trong class ExpenseRepository.java

    public boolean deleteRecurring(long id) {
        if (userId <= 0 || id <= 0) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        // Xóa khoản định kỳ
        int rows = db.delete("recurring_expenses", "id = ? AND userId = ?", 
                new String[]{String.valueOf(id), String.valueOf(userId)});
        
        // Tùy chọn: Xóa các hóa đơn đã được tự động tạo từ khoản này nếu muốn
        // db.delete("expenses", "recurringExpenseId = ?", new String[]{String.valueOf(id)});
        
        return rows > 0;
    }

    public boolean updateRecurring(long id, String title, double amount, long categoryId, Date startDate) {
        if (userId <= 0 || id <= 0) return false;
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("amount", amount);
        if (categoryId > 0) {
            values.put("categoryId", categoryId);
        } else {
            values.putNull("categoryId");
        }
        values.put("startDateMillis", startDate.getTime());
        
        int rows = db.update("recurring_expenses", values, "id = ? AND userId = ?", 
                new String[]{String.valueOf(id), String.valueOf(userId)});
        return rows > 0;
    }
}
