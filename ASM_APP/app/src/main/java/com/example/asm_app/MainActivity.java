package com.example.asm_app;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.asm_app.ui.BudgetFragment;
import com.example.asm_app.ui.HomeFragment;
import com.example.asm_app.ui.RecurringFragment;
import com.example.asm_app.ui.TransactionsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNav);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selected = getFragmentFor(item.getItemId());
            if (selected != null) {
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
}
