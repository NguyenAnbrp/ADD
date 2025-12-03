package com.example.asm_app.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.asm_app.R;
import com.example.asm_app.model.Category;
import com.example.asm_app.model.RecurringExpense;
import com.example.asm_app.repositories.ExpenseRepository;
import com.example.asm_app.util.FormatUtils;
import com.example.asm_app.util.SessionManager;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecurringFragment extends Fragment {

    private EditText dateInput;
    private EditText titleInput;
    private EditText amountInput;
    private Spinner categorySpinner;
    private LinearLayout recurringList;
    private ExpenseRepository repository;
    private SessionManager sessionManager;
    private final List<Category> categories = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recurring, container, false);
        sessionManager = new SessionManager(requireContext());
        repository = new ExpenseRepository(requireContext(), sessionManager.getUserId());
        setup(view);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        renderRecurringList();
        loadCategories();
    }

    private void setup(View view) {
        categorySpinner = view.findViewById(R.id.recurringCategorySpinner);
        Button addRecurringBtn = view.findViewById(R.id.addRecurringBtn);
        recurringList = view.findViewById(R.id.recurringList);
        dateInput = view.findViewById(R.id.recurringDateInput);
        titleInput = view.findViewById(R.id.recurringTitleInput);
        amountInput = view.findViewById(R.id.recurringAmountInput);

        loadCategories();

        Calendar calendar = Calendar.getInstance();
        dateInput.setText(FormatUtils.formatDate(calendar.getTime()));
        dateInput.setOnClickListener(v -> showDatePicker(calendar));

        addRecurringBtn.setOnClickListener(v -> saveRecurring());

        renderRecurringList();
    }

    private void loadCategories() {
        categories.clear();
        categories.addAll(repository.getCategories());
        List<String> labels = new ArrayList<>();
        labels.add("Không phân loại");
        for (Category category : categories) {
            labels.add(category.getName());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);
    }

    private void saveRecurring() {
        String title = titleInput.getText().toString().trim();
        String amountText = amountInput.getText().toString().trim();
        if (title.isEmpty() || amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Nhập đầy đủ mô tả và số tiền", Toast.LENGTH_SHORT).show();
            return;
        }
        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }
        Calendar chosen = Calendar.getInstance();
        chosen.setTime(parseDate(dateInput.getText().toString()));
        long categoryId = selectedCategoryId();
        repository.addRecurring(title, amount, categoryId, chosen.getTime());
        Toast.makeText(requireContext(), "Đã lưu khoản định kỳ", Toast.LENGTH_SHORT).show();
        titleInput.setText("");
        amountInput.setText("");
        renderRecurringList();
    }

    private void showDatePicker(Calendar calendar) {
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

    private void renderRecurringList() {
        recurringList.removeAllViews();
        List<RecurringExpense> items = repository.getRecurring();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        if (items.isEmpty()) {
            TextView empty = new TextView(requireContext());
            empty.setText("Chưa có khoản định kỳ.");
            empty.setTextColor(getResources().getColor(R.color.gray_700));
            empty.setPadding(16, 16, 16, 16);
            recurringList.addView(empty);
            return;
        }
        for (RecurringExpense item : items) {
            View row = inflater.inflate(R.layout.item_recurring, recurringList, false);
            TextView title = row.findViewById(R.id.recurringTitle);
            TextView category = row.findViewById(R.id.recurringCategory);
            TextView amount = row.findViewById(R.id.recurringAmount);
            TextView date = row.findViewById(R.id.recurringDate);

            title.setText(item.getTitle());
            category.setText(item.getCategory());
            amount.setText(FormatUtils.formatCurrency(item.getAmount()));
            date.setText("Bắt đầu " + FormatUtils.formatDate(item.getStartDate()));
            recurringList.addView(row);
        }
    }

    private long selectedCategoryId() {
        int position = categorySpinner.getSelectedItemPosition();
        if (position <= 0 || position - 1 >= categories.size()) {
            return -1;
        }
        return categories.get(position - 1).getId();
    }

    private java.util.Date parseDate(String value) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.parse(value);
        } catch (Exception e) {
            return new java.util.Date();
        }
    }
}
