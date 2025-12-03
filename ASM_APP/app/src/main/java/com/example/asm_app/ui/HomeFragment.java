package com.example.asm_app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.example.asm_app.R;
import com.example.asm_app.model.BudgetCategory;
import com.example.asm_app.model.Expense;
import com.example.asm_app.repositories.ExpenseRepository;
import com.example.asm_app.util.FormatUtils;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment {

    private ExpenseRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        repository = new ExpenseRepository(requireContext());
        bindData(view);
        return view;
    }

    private void bindData(View view) {
        Context context = requireContext();
        TextView balanceText = view.findViewById(R.id.balanceText);
        TextView incomeAmount = view.findViewById(R.id.incomeAmount);
        TextView expenseAmount = view.findViewById(R.id.expenseAmount);
        ProgressBar monthProgress = view.findViewById(R.id.monthProgress);
        TextView budgetProgressLabel = view.findViewById(R.id.budgetProgressLabel);
        LinearLayout topCategoryList = view.findViewById(R.id.topCategoryList);
        LinearLayout recentTransactions = view.findViewById(R.id.recentTransactions);

        double income = 0;
        double expense = repository.getTotalExpenses();
        double balance = income - expense;

        balanceText.setText(FormatUtils.formatCurrency(balance));
        incomeAmount.setText(FormatUtils.formatCurrency(income));
        expenseAmount.setText(FormatUtils.formatCurrency(expense));

        int progressValue = 0;
        monthProgress.setProgress(progressValue);
        budgetProgressLabel.setText(progressValue + "% ngân sách đã dùng");

        List<BudgetCategory> budgets = repository.getBudgets();
        Collections.sort(budgets, Comparator.comparingDouble(BudgetCategory::getSpent).reversed());
        topCategoryList.removeAllViews();
        for (int i = 0; i < Math.min(3, budgets.size()); i++) {
            BudgetCategory category = budgets.get(i);
            View item = LayoutInflater.from(context).inflate(R.layout.item_top_category, topCategoryList, false);
            View dot = item.findViewById(R.id.categoryDot);
            TextView name = item.findViewById(R.id.categoryName);
            TextView amount = item.findViewById(R.id.categoryAmount);

            ViewCompat.setBackgroundTintList(dot, ColorStateList.valueOf(category.getColorRes()));
            name.setText(category.getName());
            amount.setText(FormatUtils.formatCurrency(category.getSpent()));
            topCategoryList.addView(item);
        }

        List<Expense> expenses = repository.getExpenses();
        recentTransactions.removeAllViews();
        for (Expense item : expenses) {
            View row = LayoutInflater.from(context).inflate(R.layout.item_transaction, recentTransactions, false);
            TextView avatar = row.findViewById(R.id.transactionAvatar);
            TextView title = row.findViewById(R.id.transactionTitle);
            TextView meta = row.findViewById(R.id.transactionMeta);
            TextView amount = row.findViewById(R.id.transactionAmount);

            avatar.setText(firstLetter(item.getCategory()));
            ViewCompat.setBackgroundTintList(avatar, ColorStateList.valueOf(item.getColorRes()));
            title.setText(item.getTitle());
            meta.setText(FormatUtils.formatDate(item.getDate()) + "  •  " + item.getCategory());
            amount.setText("-" + FormatUtils.formatCurrency(item.getAmount()));
            recentTransactions.addView(row);
        }
    }

    private String firstLetter(String value) {
        if (value == null || value.isEmpty()) {
            return "?";
        }
        return value.substring(0, 1).toUpperCase();
    }
}
