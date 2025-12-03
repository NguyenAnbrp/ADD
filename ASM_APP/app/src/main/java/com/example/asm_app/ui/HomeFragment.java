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
import com.example.asm_app.util.SessionManager;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class HomeFragment extends Fragment {

    private ExpenseRepository repository;
    private SessionManager sessionManager;
    private View rootView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        rootView = view;
        sessionManager = new SessionManager(requireContext());
        repository = new ExpenseRepository(requireContext(), sessionManager.getUserId());
        repository.ensureDefaultCategoriesIfEmpty();
        bindData(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (rootView != null) {
            repository = new ExpenseRepository(requireContext(), sessionManager.getUserId());
            repository.ensureDefaultCategoriesIfEmpty();
            bindData(rootView);
        }
    }

    private void bindData(View view) {
        Context context = requireContext();
        TextView balanceText = view.findViewById(R.id.balanceText);
        TextView limitAmount = view.findViewById(R.id.incomeAmount);
        TextView expenseAmount = view.findViewById(R.id.expenseAmount);
        ProgressBar monthProgress = view.findViewById(R.id.monthProgress);
        TextView budgetProgressLabel = view.findViewById(R.id.budgetProgressLabel);
        LinearLayout topCategoryList = view.findViewById(R.id.topCategoryList);
        LinearLayout recentTransactions = view.findViewById(R.id.recentTransactions);

        List<BudgetCategory> budgets = repository.getBudgets();
        double totalLimit = 0;
        for (BudgetCategory item : budgets) {
            if (item.getLimit() != null && item.getLimit() > 0) {
                totalLimit += item.getLimit();
            }
        }
        double expense = repository.getTotalExpenses();
        double balance = totalLimit - expense;

        balanceText.setText(FormatUtils.formatCurrency(balance));
        limitAmount.setText(FormatUtils.formatCurrency(totalLimit));
        expenseAmount.setText(FormatUtils.formatCurrency(expense));

        double totalSpent = 0;
        for (BudgetCategory item : budgets) {
            if (item.getLimit() != null && item.getLimit() > 0) {
                totalSpent += item.getSpent();
            }
        }
        int progressValue = totalLimit > 0 ? Math.min(100, (int) ((totalSpent / totalLimit) * 100)) : 0;
        monthProgress.setProgress(progressValue);
        budgetProgressLabel.setText(progressValue + "% ngân sách đã dùng");

        Collections.sort(budgets, Comparator.comparingDouble(BudgetCategory::getSpent).reversed());
        topCategoryList.removeAllViews();
        if (budgets.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("Chưa có danh mục chi tiêu.");
            empty.setTextColor(getResources().getColor(R.color.gray_700));
            topCategoryList.addView(empty);
        } else {
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
        }

        List<Expense> expenses = repository.getExpenses();
        recentTransactions.removeAllViews();
        if (expenses.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("Chưa có giao dịch gần đây.");
            empty.setTextColor(getResources().getColor(R.color.gray_700));
            recentTransactions.addView(empty);
        } else {
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
    }

    private String firstLetter(String value) {
        if (value == null || value.isEmpty()) {
            return "?";
        }
        return value.substring(0, 1).toUpperCase();
    }
}
