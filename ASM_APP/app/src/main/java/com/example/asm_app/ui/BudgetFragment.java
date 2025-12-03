package com.example.asm_app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.example.asm_app.R;
import com.example.asm_app.model.BudgetCategory;
import com.example.asm_app.repositories.ExpenseRepository;
import com.example.asm_app.util.FormatUtils;
import com.example.asm_app.util.SessionManager;

import java.util.List;

public class BudgetFragment extends Fragment {

    private ExpenseRepository repository;
    private SessionManager sessionManager;
    private LinearLayout budgetList;

    private final int[] colorPalette = new int[]{
            R.color.danger_red,
            R.color.blue_600,
            R.color.warning_yellow,
            R.color.success_green,
            R.color.teal,
            R.color.gray_700,
            R.color.blue_light,
            R.color.gray_300
    };
    private final String[] colorLabels = new String[]{
            "Đỏ",
            "Xanh dương",
            "Vàng",
            "Xanh lá",
            "Xanh ngọc",
            "Xám đậm",
            "Xanh nhạt",
            "Xám nhạt"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_budget, container, false);
        sessionManager = new SessionManager(requireContext());
        repository = new ExpenseRepository(requireContext(), sessionManager.getUserId());
        budgetList = view.findViewById(R.id.budgetList);
        repository.ensureDefaultCategoriesIfEmpty();
        setup(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        repository.ensureDefaultCategoriesIfEmpty();
        renderBudgets();
    }

    private void setup(View view) {
        Button addCategoryBtn = view.findViewById(R.id.addCategoryBtn);
        addCategoryBtn.setOnClickListener(v -> showAddCategoryDialog());
        renderBudgets();
    }

    private void renderBudgets() {
        Context context = requireContext();
        budgetList.removeAllViews();
        List<BudgetCategory> budgets = repository.getBudgets();
        if (budgets.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("Chưa có danh mục chi tiêu. Thêm danh mục mới để theo dõi ngân sách.");
            empty.setTextColor(getResources().getColor(R.color.gray_700));
            empty.setPadding(16, 16, 16, 16);
            budgetList.addView(empty);
            return;
        }
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

    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        EditText nameInput = dialogView.findViewById(R.id.categoryNameInput);
        EditText limitInput = dialogView.findViewById(R.id.categoryLimitInput);
        Spinner colorSpinner = dialogView.findViewById(R.id.categoryColorSpinner);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, colorLabels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        colorSpinner.setAdapter(adapter);

        new AlertDialog.Builder(requireContext())
                .setTitle("Thêm danh mục")
                .setView(dialogView)
                .setPositiveButton("Lưu", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String limitText = limitInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Tên danh mục không được để trống", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Double limit = null;
                    if (!limitText.isEmpty()) {
                        try {
                            limit = Double.parseDouble(limitText);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), "Hạn mức không hợp lệ", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    int colorRes = colorPalette[colorSpinner.getSelectedItemPosition() % colorPalette.length];
                    int color = ContextCompat.getColor(requireContext(), colorRes);
                    long result = repository.addCategory(name, color, limit);
                    if (result == -1) {
                        Toast.makeText(requireContext(), "Danh mục đã tồn tại", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Đã lưu danh mục", Toast.LENGTH_SHORT).show();
                    }
                    renderBudgets();
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private String firstLetter(String value) {
        if (value == null || value.isEmpty()) {
            return "?";
        }
        return value.substring(0, 1).toUpperCase();
    }
}
