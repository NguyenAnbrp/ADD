package com.example.asm_app;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asm_app.model.User;
import com.example.asm_app.repositories.ExpenseRepository;
import com.example.asm_app.repositories.UserRepository;
import com.example.asm_app.util.SessionManager;

public class RegisterActivity extends AppCompatActivity {

    private EditText nameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private UserRepository userRepository;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusBar();
        setContentView(R.layout.activity_register);

        userRepository = new UserRepository(this);
        sessionManager = new SessionManager(this);

        if (sessionManager.getUserId() > 0) {
            openHome();
            finish();
            return;
        }

        nameInput = findViewById(R.id.registerNameInput);
        emailInput = findViewById(R.id.registerEmailInput);
        passwordInput = findViewById(R.id.registerPasswordInput);

        findViewById(R.id.registerBtn).setOnClickListener(v -> handleRegister());

        TextView goLogin = findViewById(R.id.goLogin);
        goLogin.setOnClickListener(v -> {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void handleRegister() {
        String name = nameInput.getText().toString().trim();
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString();
        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        String passwordError = validatePassword(password);
        if (passwordError != null) {
            Toast.makeText(this, passwordError, Toast.LENGTH_SHORT).show();
            return;
        }
        if (userRepository.emailExists(email)) {
            Toast.makeText(this, "Email already exists", Toast.LENGTH_SHORT).show();
            return;
        }
        User user = userRepository.register(name, email, password);
        if (user == null) {
            Toast.makeText(this, "Could not create account. Please try again later.", Toast.LENGTH_SHORT).show();
            return;
        }
        sessionManager.saveUser(user.getId(), user.getName(), user.getEmail());
        ExpenseRepository expenseRepository = new ExpenseRepository(this, user.getId());
        expenseRepository.ensureDefaultCategoriesIfEmpty();
        openHome();
    }

    private String validatePassword(String password) {
        if (password.length() < 6) {
            return "Mật khẩu phải có tối thiểu 6 ký tự";
        }
        
        boolean hasUpperCase = false;
        boolean hasDigit = false;
        boolean hasSpecialChar = false;
        
        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) {
                hasUpperCase = true;
            } else if (Character.isDigit(c)) {
                hasDigit = true;
            } else if (!Character.isLetterOrDigit(c)) {
                hasSpecialChar = true;
            }
        }
        
        if (!hasUpperCase) {
            return "Mật khẩu phải chứa ít nhất 1 chữ hoa";
        }
        if (!hasDigit) {
            return "Mật khẩu phải chứa ít nhất 1 số";
        }
        if (!hasSpecialChar) {
            return "Mật khẩu phải chứa ít nhất 1 ký tự đặc biệt";
        }
        
        return null; // Password is valid
    }

    private void openHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    private void hideStatusBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            decorView.setSystemUiVisibility(uiOptions);
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideStatusBar();
        }
    }
}
