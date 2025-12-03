package com.example.asm_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.asm_app.ui.BudgetFragment;
import com.example.asm_app.ui.HomeFragment;
import com.example.asm_app.ui.RecurringFragment;
import com.example.asm_app.ui.TransactionsFragment;
import com.example.asm_app.util.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.bottomsheet.BottomSheetDialog;

public class MainActivity extends AppCompatActivity {

    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessionManager = new SessionManager(this);
        if (sessionManager.getUserId() <= 0) {
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selected = getFragmentFor(item.getItemId());
            if (item.getItemId() == R.id.nav_profile) {
                showAccountBottomSheet();
                return false;
            } else if (selected != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragmentContainer, selected)
                        .commit();
                return true;
            }
            return false;
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    private Fragment getFragmentFor(int itemId) {
        if (itemId == R.id.nav_home) {
            return new HomeFragment();
        } else if (itemId == R.id.nav_transactions) {
            return new TransactionsFragment();
        } else if (itemId == R.id.nav_budget) {
            return new BudgetFragment();
        } else if (itemId == R.id.nav_recurring) {
            return new RecurringFragment();
        }
        return null;
    }

    private void showAccountBottomSheet() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View sheet = getLayoutInflater().inflate(R.layout.bottom_sheet_account, null);
        dialog.setContentView(sheet);

        TextView name = sheet.findViewById(R.id.sheetAccountName);
        TextView email = sheet.findViewById(R.id.sheetAccountEmail);
        RadioGroup reportRange = sheet.findViewById(R.id.sheetReportRange);
        View logoutBtn = sheet.findViewById(R.id.sheetLogoutBtn);

        name.setText(sessionManager.getUserName().isEmpty() ? "Người dùng" : sessionManager.getUserName());
        email.setText(sessionManager.getUserEmail().isEmpty() ? "Chưa có email" : sessionManager.getUserEmail());

        sheet.findViewById(R.id.sheetReportBtn).setOnClickListener(v -> {
            int checked = reportRange.getCheckedRadioButtonId();
            String choice = "Tuần";
            if (checked == R.id.rangeMonth) {
                choice = "Tháng";
            } else if (checked == R.id.rangeYear) {
                choice = "Năm";
            }
            android.widget.Toast.makeText(this, "Xem báo cáo: " + choice, android.widget.Toast.LENGTH_SHORT).show();
        });

        logoutBtn.setOnClickListener(v -> {
            dialog.dismiss();
            sessionManager.clear();
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finishAffinity();
        });

        dialog.show();
    }
}
