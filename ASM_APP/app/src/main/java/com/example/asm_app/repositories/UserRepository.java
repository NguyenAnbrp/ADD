package com.example.asm_app.repositories;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.example.asm_app.database.SQLiteDbHelper;
import com.example.asm_app.model.User;

public class UserRepository {

    private final SQLiteDbHelper dbHelper;

    public UserRepository(Context context) {
        this.dbHelper = new SQLiteDbHelper(context);
    }

    public User register(String name, String email, String password) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", name);
        values.put("email", email);
        values.put("password", password);
        long id = db.insert("users", null, values);
        if (id == -1) {
            return null;
        }
        return new User(id, name, email);
    }

    public User login(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, email FROM users WHERE email = ? AND password = ? LIMIT 1",
                new String[]{email, password});
        User user = null;
        if (cursor.moveToFirst()) {
            long id = cursor.getLong(0);
            String name = cursor.getString(1);
            String mail = cursor.getString(2);
            user = new User(id, name, mail);
        }
        cursor.close();
        return user;
    }

    public boolean emailExists(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id FROM users WHERE email = ? LIMIT 1", new String[]{email});
        boolean exists = cursor.moveToFirst();
        cursor.close();
        return exists;
    }

    public User findById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT id, name, email FROM users WHERE id = ? LIMIT 1",
                new String[]{String.valueOf(id)});
        User user = null;
        if (cursor.moveToFirst()) {
            long userId = cursor.getLong(0);
            String name = cursor.getString(1);
            String email = cursor.getString(2);
            user = new User(userId, name, email);
        }
        cursor.close();
        return user;
    }
}
