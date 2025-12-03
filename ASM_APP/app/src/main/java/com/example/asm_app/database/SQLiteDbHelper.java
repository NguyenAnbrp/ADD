package com.example.asm_app.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.asm_app.R;

public class SQLiteDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "campus_expense.db";
    private static final int DB_VERSION = 1;

    private final Context appContext;

    public SQLiteDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        this.appContext = context.getApplicationContext();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL UNIQUE," +
                "color INTEGER," +
                "limitAmount REAL" +
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "categoryName TEXT," +
                "categoryColor INTEGER," +
                "amount REAL NOT NULL," +
                "dateMillis INTEGER NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS recurring_expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "title TEXT NOT NULL," +
                "amount REAL NOT NULL," +
                "categoryName TEXT," +
                "startDateMillis INTEGER NOT NULL" +
                ")");

        seedInitialData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS recurring_expenses");
        db.execSQL("DROP TABLE IF EXISTS expenses");
        db.execSQL("DROP TABLE IF EXISTS categories");
        onCreate(db);
    }

    private void seedInitialData(SQLiteDatabase db) {
        insertCategory(db, "Ăn uống", color(R.color.danger_red));
        insertCategory(db, "Thuê nhà", color(R.color.blue_600));
        insertCategory(db, "Di chuyển", color(R.color.warning_yellow));
        insertCategory(db, "Học phí", color(R.color.success_green));
        insertCategory(db, "Giải trí", color(R.color.blue_light));
        insertCategory(db, "Điện nước", color(R.color.gray_700));
        insertCategory(db, "Mua sắm", color(R.color.danger_red));
        insertCategory(db, "Sức khỏe", color(R.color.teal));
        insertCategory(db, "Khác", color(R.color.gray_300));

        insertExpense(db, "Đi siêu thị", "Ăn uống", color(R.color.danger_red), 450_000, parseDateMillis("03/12/2025"));
        insertExpense(db, "Vé xe bus", "Di chuyển", color(R.color.warning_yellow), 7_000, parseDateMillis("03/12/2025"));
        insertExpense(db, "Sách giáo trình", "Học phí", color(R.color.success_green), 250_000, parseDateMillis("02/12/2025"));

        insertRecurring(db, "Tiền trọ", 1_500_000, "Thuê nhà", parseDateMillis("12/03/2025"));
        insertRecurring(db, "Internet", 200_000, "Điện nước", parseDateMillis("12/05/2025"));
    }

    private void insertCategory(SQLiteDatabase db, String name, int color) {
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("color", color);
        db.insertWithOnConflict("categories", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    private void insertExpense(SQLiteDatabase db, String title, String categoryName, int categoryColor, double amount, long dateMillis) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("categoryName", categoryName);
        values.put("categoryColor", categoryColor);
        values.put("amount", amount);
        values.put("dateMillis", dateMillis);
        db.insert("expenses", null, values);
    }

    private void insertRecurring(SQLiteDatabase db, String title, double amount, String categoryName, long startDateMillis) {
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("amount", amount);
        values.put("categoryName", categoryName);
        values.put("startDateMillis", startDateMillis);
        db.insert("recurring_expenses", null, values);
    }

    private int color(int resId) {
        return appContext.getColor(resId);
    }

    private long parseDateMillis(String value) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
            return sdf.parse(value).getTime();
        } catch (Exception e) {
            return System.currentTimeMillis();
        }
    }
}
