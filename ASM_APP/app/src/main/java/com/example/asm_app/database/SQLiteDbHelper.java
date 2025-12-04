package com.example.asm_app.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "campus_expense.db";
    private static final int DB_VERSION = 3;

    public SQLiteDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT NOT NULL," +
                "email TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL" +
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS categories (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "userId INTEGER NOT NULL," +
                "name TEXT NOT NULL," +
                "color INTEGER NOT NULL," +
                "limitAmount REAL," +
                "UNIQUE(userId, name)," +
                "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE" +
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "userId INTEGER NOT NULL," +
                "categoryId INTEGER," +
                "title TEXT NOT NULL," +
                "amount REAL NOT NULL," +
                "dateMillis INTEGER NOT NULL," +
                "recurringExpenseId INTEGER," +
                "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE," +
                "FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL," +
                "FOREIGN KEY(recurringExpenseId) REFERENCES recurring_expenses(id) ON DELETE SET NULL" +
                ")");

        db.execSQL("CREATE TABLE IF NOT EXISTS recurring_expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "userId INTEGER NOT NULL," +
                "categoryId INTEGER," +
                "title TEXT NOT NULL," +
                "amount REAL NOT NULL," +
                "startDateMillis INTEGER NOT NULL," +
                "FOREIGN KEY(userId) REFERENCES users(id) ON DELETE CASCADE," +
                "FOREIGN KEY(categoryId) REFERENCES categories(id) ON DELETE SET NULL" +
                ")");

        db.execSQL("CREATE INDEX IF NOT EXISTS idx_categories_user ON categories(userId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_user ON expenses(userId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_category ON expenses(categoryId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_recurring ON expenses(recurringExpenseId)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_recurring_user ON recurring_expenses(userId)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 3) {
            // Add recurringExpenseId column to expenses table
            db.execSQL("ALTER TABLE expenses ADD COLUMN recurringExpenseId INTEGER");
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_expenses_recurring ON expenses(recurringExpenseId)");
        }
        if (oldVersion < 2) {
            db.execSQL("DROP TABLE IF EXISTS recurring_expenses");
            db.execSQL("DROP TABLE IF EXISTS expenses");
            db.execSQL("DROP TABLE IF EXISTS categories");
            db.execSQL("DROP TABLE IF EXISTS users");
            onCreate(db);
        }
    }
}
