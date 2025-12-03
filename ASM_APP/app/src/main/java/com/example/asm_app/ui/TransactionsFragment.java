package com.example.asm_app.ui;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.Fragment;

import com.example.asm_app.R;
import com.example.asm_app.model.Category;
import com.example.asm_app.model.Expense;
import com.example.asm_app.repositories.ExpenseRepository;
import com.example.asm_app.util.FormatUtils;
import com.example.asm_app.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TransactionsFragment extends Fragment {

    private static final String ALL_LABEL = "All";

    private final List<TextView> chipViews = new ArrayList<>();
    private String selectedCategory = ALL_LABEL;
    private EditText searchInput;
    private LinearLayout transactionList;
    private LinearLayout categoryChipContainer;
    private List<Expense> sourceExpenses = new ArrayList<>();
    private final List<Category> categories = new ArrayList<>();
    private ExpenseRepository repository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_transactions, container, false);
        sessionManager = new SessionManager(requireContext());
        repository = new ExpenseRepository(requireContext(), sessionManager.getUserId());
        repository.ensureDefaultCategoriesIfEmpty();
        setup(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        repository.ensureDefaultCategoriesIfEmpty();
        loadData();
    }

    private void setup(View view) {
        searchInput = view.findViewById(R.id.searchInput);
        categoryChipContainer = view.findViewById(R.id.categoryChips);
        transactionList = view.findViewById(R.id.transactionList);
        View addButton = view.findViewById(R.id.addTransactionBtn);

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

        addButton.setOnClickListener(v -> showAddTransactionDialog());

        buildCategoryChips(categoryChipContainer);
        renderTransactions();
        loadData();
    }

    private void loadData() {
        categories.clear();
        categories.addAll(repository.getCategories());
        sourceExpenses = repository.getExpenses();
        if (categoryChipContainer != null) {
            buildCategoryChips(categoryChipContainer);
        }
        renderTransactions();
    }

    private void buildCategoryChips(LinearLayout container) {
        container.removeAllViews();
        chipViews.clear();
        addChip(container, ALL_LABEL, true);
        for (Category category : categories) {
            addChip(container, category.getName(), false);
        }
        if (!selectedCategory.equals(ALL_LABEL)) {
            boolean stillExists = false;
            for (Category category : categories) {
                if (category.getName().equals(selectedCategory)) {
                    stillExists = true;
                    break;
                }
            }
            if (!stillExists) {
                selectedCategory = ALL_LABEL;
            }
        }
        updateChipStyles(selectedCategory);
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

        if (sourceExpenses.isEmpty()) {
            TextView empty = new TextView(context);
            empty.setText("No transactions yet. Add one to start tracking.");
            empty.setTextColor(getResources().getColor(R.color.gray_700));
            empty.setPadding(16, 16, 16, 16);
            transactionList.addView(empty);
            return;
        }

        for (Expense item : sourceExpenses) {
            boolean matchCategory = selectedCategory.equals(ALL_LABEL) || item.getCategory().equalsIgnoreCase(selectedCategory);
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
            meta.setText(FormatUtils.formatDate(item.getDate()) + " • " + item.getCategory());
            amount.setText("-" + FormatUtils.formatCurrency(item.getAmount()));
            transactionList.addView(row);
        }
    }

    private void showAddTransactionDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_transaction, null);
        EditText titleInput = dialogView.findViewById(R.id.transactionTitleInput);
        EditText amountInput = dialogView.findViewById(R.id.transactionAmountInput);
        EditText dateInput = dialogView.findViewById(R.id.transactionDateInput);
        Spinner categorySpinner = dialogView.findViewById(R.id.transactionCategorySpinner);

        List<String> labels = new ArrayList<>();
        labels.add("Uncategorized");
        for (Category category : categories) {
            labels.add(category.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        Calendar calendar = Calendar.getInstance();
        dateInput.setText(FormatUtils.formatDate(calendar.getTime()));
        dateInput.setOnClickListener(v -> showDatePicker(calendar, dateInput));

        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Add transaction")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String title = titleInput.getText().toString().trim();
                    String amountText = amountInput.getText().toString().trim();
                    if (title.isEmpty() || amountText.isEmpty()) {
                        Toast.makeText(requireContext(), "Enter both title and amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double amount;
                    try {
                        amount = Double.parseDouble(amountText);
                    } catch (NumberFormatException e) {
                        Toast.makeText(requireContext(), "Invalid amount", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Calendar chosen = Calendar.getInstance();
                    chosen.setTime(parseDate(dateInput.getText().toString()));
                    long categoryId = resolveCategoryId(categorySpinner.getSelectedItemPosition());
                    repository.addExpense(title, categoryId, amount, chosen.getTime());
                    Toast.makeText(requireContext(), "Transaction added", Toast.LENGTH_SHORT).show();
                    loadData();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDatePicker(Calendar calendar, EditText dateInput) {
        DatePickerDialog dialog = new DatePickerDialog(requireContext(),
                (view, year, month, dayOfMonth) -> {
                    calendar.set(year, month, dayOfMonth);
                    dateInput.setText(FormatUtils.formatDate(calendar.getTime()));
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private long resolveCategoryId(int position) {
        if (position <= 0 || position - 1 >= categories.size()) {
            return -1;
        }
        return categories.get(position - 1).getId();
    }

    private java.util.Date parseDate(String value) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.parse(value);
        } catch (Exception e) {
            return new java.util.Date();
        }
    }

    private String firstLetter(String value) {
        if (value == null || value.isEmpty()) {
            return "?";
        }
        return value.substring(0, 1).toUpperCase();
    }
}
