package com.example.asm_app.ui;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.example.asm_app.R;
import com.example.asm_app.model.Expense;
import com.example.asm_app.repositories.ExpenseRepository;
import com.example.asm_app.util.FormatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TransactionsFragment extends Fragment {

    private final List<TextView> chipViews = new ArrayList<>();
    private String selectedCategory = "Tất cả";
    private EditText searchInput;
    private LinearLayout transactionList;
    private List<Expense> sourceExpenses;
    private ExpenseRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);
        repository = new ExpenseRepository(requireContext());
        setup(view);
        return view;
    }

    private void setup(View view) {
        Context context = requireContext();
        searchInput = view.findViewById(R.id.searchInput);
        LinearLayout categoryChips = view.findViewById(R.id.categoryChips);
        transactionList = view.findViewById(R.id.transactionList);
        View addButton = view.findViewById(R.id.addTransactionBtn);
        sourceExpenses = repository.getExpenses();

        String[] categories = getResources().getStringArray(R.array.expense_categories);
        addChip(categoryChips, "Tất cả", true);
        for (String category : categories) {
            addChip(categoryChips, category, false);
        }

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                renderTransactions();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        addButton.setOnClickListener(v -> Toast.makeText(context, "Tạo giao dịch mới (demo)", Toast.LENGTH_SHORT).show());

        renderTransactions();
    }

    private void addChip(LinearLayout container, String label, boolean selected) {
        TextView chip = new TextView(requireContext());
        chip.setText(label);
        chip.setTextSize(13);
        int horizontal = dp(20);
        int vertical = dp(10);
        chip.setPadding(horizontal, vertical, horizontal, vertical);
        chip.setAllCaps(false);
        chip.setBackground(selected ? requireContext().getDrawable(R.drawable.bg_chip_selected) : requireContext().getDrawable(R.drawable.bg_chip_unselected));
        chip.setTextColor(selected ? getResources().getColor(R.color.white) : getResources().getColor(R.color.gray_700));
        chip.setOnClickListener(v -> {
            selectedCategory = label;
            updateChipStyles(label);
            renderTransactions();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 0, 12, 0);
        container.addView(chip, params);
        chipViews.add(chip);
    }

    private int dp(int value) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (value * density);
    }

    private void updateChipStyles(String selectedLabel) {
        for (TextView chip : chipViews) {
            boolean active = chip.getText().toString().equals(selectedLabel);
            chip.setBackground(active ? requireContext().getDrawable(R.drawable.bg_chip_selected) : requireContext().getDrawable(R.drawable.bg_chip_unselected));
            chip.setTextColor(active ? getResources().getColor(R.color.white) : getResources().getColor(R.color.gray_700));
        }
    }

    private void renderTransactions() {
        Context context = requireContext();
        String query = searchInput.getText() != null ? searchInput.getText().toString().toLowerCase(Locale.getDefault()) : "";
        transactionList.removeAllViews();

        for (Expense item : sourceExpenses) {
            boolean matchCategory = selectedCategory.equals("Tất cả") || item.getCategory().equalsIgnoreCase(selectedCategory);
            boolean matchQuery = item.getTitle().toLowerCase(Locale.getDefault()).contains(query);
            if (!matchCategory || !matchQuery) {
                continue;
            }
            View row = LayoutInflater.from(context).inflate(R.layout.item_transaction, transactionList, false);
            TextView avatar = row.findViewById(R.id.transactionAvatar);
            TextView title = row.findViewById(R.id.transactionTitle);
            TextView meta = row.findViewById(R.id.transactionMeta);
            TextView amount = row.findViewById(R.id.transactionAmount);

            avatar.setText(firstLetter(item.getCategory()));
            ViewCompat.setBackgroundTintList(avatar, android.content.res.ColorStateList.valueOf(item.getColorRes()));
            title.setText(item.getTitle());
            meta.setText(FormatUtils.formatDate(item.getDate()) + "  •  " + item.getCategory());
            amount.setText("-" + FormatUtils.formatCurrency(item.getAmount()));
            transactionList.addView(row);
        }
    }

    private String firstLetter(String value) {
        if (value == null || value.isEmpty()) {
            return "?";
        }
        return value.substring(0, 1).toUpperCase();
    }
}
