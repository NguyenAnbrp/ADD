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
    private TextView monthSelector;
    private Calendar selectedMonth;

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
        // Initialize selected month if not set
        if (selectedMonth == null) {
            selectedMonth = Calendar.getInstance();
            selectedMonth.set(Calendar.DAY_OF_MONTH, 1);
            if (monthSelector != null) {
                updateMonthSelectorText();
            }
        }
        loadData();
    }

    private void setup(View view) {
        searchInput = view.findViewById(R.id.searchInput);
        categoryChipContainer = view.findViewById(R.id.categoryChips);
        transactionList = view.findViewById(R.id.transactionList);
        View addButton = view.findViewById(R.id.addTransactionBtn);
        monthSelector = view.findViewById(R.id.monthSelector);

        // Initialize selected month to current month
        selectedMonth = Calendar.getInstance();
        selectedMonth.set(Calendar.DAY_OF_MONTH, 1);
        updateMonthSelectorText();

        if (monthSelector != null) {
            monthSelector.setOnClickListener(v -> showMonthPicker());
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

        addButton.setOnClickListener(v -> showAddTransactionDialog());

        buildCategoryChips(categoryChipContainer);
        loadData();
    }

    private void loadData() {
        categories.clear();
        categories.addAll(repository.getCategories());
        
        // Process recurring expenses for selected month
        if (selectedMonth != null) {
            repository.processRecurringExpensesForMonth(
                    selectedMonth.get(Calendar.YEAR),
                    selectedMonth.get(Calendar.MONTH)
            );
            // Load expenses for selected month
            sourceExpenses = repository.getExpensesByMonth(
                    selectedMonth.get(Calendar.YEAR),
                    selectedMonth.get(Calendar.MONTH)
            );
        } else {
            sourceExpenses = repository.getExpenses();
        }
        
        if (categoryChipContainer != null) {
            buildCategoryChips(categoryChipContainer);
        }
        renderTransactions();
    }
    
    private void updateMonthSelectorText() {
        if (monthSelector != null && selectedMonth != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM/yyyy", Locale.getDefault());
            monthSelector.setText(sdf.format(selectedMonth.getTime()));
        }
    }
    
    private void showMonthPicker() {
        if (selectedMonth == null) {
            selectedMonth = Calendar.getInstance();
        }
        
        // Create a simple dialog with month/year picker
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_month_picker, null);
        Spinner monthSpinner = dialogView.findViewById(R.id.monthSpinner);
        Spinner yearSpinner = dialogView.findViewById(R.id.yearSpinner);
        
        // Setup month spinner
        String[] months = new String[]{"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        ArrayAdapter<String> monthAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, months);
        monthAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        monthSpinner.setAdapter(monthAdapter);
        monthSpinner.setSelection(selectedMonth.get(Calendar.MONTH));
        
        // Setup year spinner (current year ± 5 years)
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        List<String> years = new ArrayList<>();
        for (int i = currentYear - 5; i <= currentYear + 5; i++) {
            years.add(String.valueOf(i));
        }
        ArrayAdapter<String> yearAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, years);
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        yearSpinner.setAdapter(yearAdapter);
        int yearIndex = years.indexOf(String.valueOf(selectedMonth.get(Calendar.YEAR)));
        if (yearIndex >= 0) {
            yearSpinner.setSelection(yearIndex);
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Select Month")
                .setView(dialogView)
                .setPositiveButton("OK", (dialog, which) -> {
                    int month = monthSpinner.getSelectedItemPosition();
                    int year = Integer.parseInt(years.get(yearSpinner.getSelectedItemPosition()));
                    selectedMonth.set(Calendar.YEAR, year);
                    selectedMonth.set(Calendar.MONTH, month);
                    selectedMonth.set(Calendar.DAY_OF_MONTH, 1);
                    updateMonthSelectorText();
                    loadData();
                })
                .setNegativeButton("Cancel", null)
                .show();
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
        boolean hasCategories = !categories.isEmpty();
        if (hasCategories) {
            labels.add("Select category");
            for (Category category : categories) {
                labels.add(category.getName());
            }
        } else {
            labels.add("No categories available");
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
        categorySpinner.setEnabled(hasCategories);
        categorySpinner.setSelection(0);

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
                    if (!categorySpinner.isEnabled()) {
                        Toast.makeText(requireContext(), "Add a category first", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    long categoryId = resolveCategoryId(categorySpinner.getSelectedItemPosition());
                    if (categoryId <= 0) {
                        Toast.makeText(requireContext(), "Please select a category", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    repository.addExpense(title, categoryId, amount, chosen.getTime());
                    Toast.makeText(requireContext(), "Transaction added", Toast.LENGTH_SHORT).show();
                    // Refresh data to show new transaction
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
        if (categories.isEmpty()) {
            return -1;
        }
        // First position is placeholder "Select category"
        int index = position - 1;
        if (index < 0 || index >= categories.size()) {
            return -1;
        }
        return categories.get(index).getId();
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
