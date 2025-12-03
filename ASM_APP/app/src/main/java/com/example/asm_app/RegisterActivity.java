package com.example.asm_app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.asm_app.model.User;
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
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }
        if (userRepository.emailExists(email)) {
            Toast.makeText(this, "Email đã tồn tại", Toast.LENGTH_SHORT).show();
            return;
        }
        User user = userRepository.register(name, email, password);
        if (user == null) {
            Toast.makeText(this, "Không thể tạo tài khoản, thử lại sau", Toast.LENGTH_SHORT).show();
            return;
        }
        sessionManager.saveUser(user.getId(), user.getName(), user.getEmail());
        openHome();
    }

    private void openHome() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}
