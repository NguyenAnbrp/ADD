package com.example.asm_app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.example.asm_app.R;
import com.example.asm_app.model.BudgetCategory;
import com.example.asm_app.repositories.ExpenseRepository;
import com.example.asm_app.util.FormatUtils;

import java.util.List;

public class BudgetFragment extends Fragment {

    private ExpenseRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);
        repository = new ExpenseRepository(requireContext());
        setup(view);
        return view;
    }

    private void setup(View view) {
        Context context = requireContext();
        LinearLayout budgetList = view.findViewById(R.id.budgetList);
        Button addCategoryBtn = view.findViewById(R.id.addCategoryBtn);
        addCategoryBtn.setOnClickListener(v -> Toast.makeText(context, "Thêm danh mục sẽ có trong phiên bản tiếp theo", Toast.LENGTH_SHORT).show());

        List<BudgetCategory> budgets = repository.getBudgets();
        budgetList.removeAllViews();
        for (BudgetCategory category : budgets) {
            View item = LayoutInflater.from(context).inflate(R.layout.item_budget, budgetList, false);
            TextView avatar = item.findViewById(R.id.budgetAvatar);
            TextView name = item.findViewById(R.id.budgetName);
            TextView spent = item.findViewById(R.id.budgetSpent);
            ProgressBar progressBar = item.findViewById(R.id.budgetProgressBar);
            TextView limitText = item.findViewById(R.id.budgetLimitText);

            avatar.setText(firstLetter(category.getName()));
            ViewCompat.setBackgroundTintList(avatar, ColorStateList.valueOf(category.getColorRes()));
            name.setText(category.getName());
            spent.setText(FormatUtils.formatCurrency(category.getSpent()));

            if (category.getLimit() != null && category.getLimit() > 0) {
                int percent = Math.min(100, (int) ((category.getSpent() / category.getLimit()) * 100));
                progressBar.setProgress(percent);
                limitText.setText("Đã dùng " + percent + "% / " + FormatUtils.formatCurrency(category.getLimit()));
            } else {
                progressBar.setProgress(0);
                limitText.setText("Chưa đặt hạn mức");
            }

            budgetList.addView(item);
        }
    }

    private String firstLetter(String value) {
        if (value == null || value.isEmpty()) {
            return "?";
        }
        return value.substring(0, 1).toUpperCase();
    }
}
