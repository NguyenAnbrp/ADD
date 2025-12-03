package com.example.asm_app.ui;

import android.app.DatePickerDialog;
import android.content.Context;
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
import com.example.asm_app.model.RecurringExpense;
import com.example.asm_app.repositories.ExpenseRepository;
import com.example.asm_app.util.FormatUtils;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecurringFragment extends Fragment {

    private EditText dateInput;
    private ExpenseRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recurring, container, false);
        repository = new ExpenseRepository(requireContext());
        setup(view);
        return view;
    }

    private void setup(View view) {
        Context context = requireContext();
        Spinner categorySpinner = view.findViewById(R.id.recurringCategorySpinner);
        Button addRecurringBtn = view.findViewById(R.id.addRecurringBtn);
        LinearLayout recurringList = view.findViewById(R.id.recurringList);
        dateInput = view.findViewById(R.id.recurringDateInput);
        EditText titleInput = view.findViewById(R.id.recurringTitleInput);
        EditText amountInput = view.findViewById(R.id.recurringAmountInput);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(context, R.array.expense_categories, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        Calendar calendar = Calendar.getInstance();
        dateInput.setText(FormatUtils.formatDate(calendar.getTime()));
        dateInput.setOnClickListener(v -> showDatePicker(calendar));

        addRecurringBtn.setOnClickListener(v -> {
            String title = titleInput.getText().toString().trim();
            String category = categorySpinner.getSelectedItem().toString();
            String amountText = amountInput.getText().toString().trim();
            if (title.isEmpty() || amountText.isEmpty()) {
                Toast.makeText(context, "Nhập đầy đủ mô tả và số tiền", Toast.LENGTH_SHORT).show();
                return;
            }
            double amount = Double.parseDouble(amountText);
            Calendar chosen = Calendar.getInstance();
            chosen.setTime(parseDate(dateInput.getText().toString()));
            repository.addRecurring(title, amount, category, chosen.getTime());
            Toast.makeText(context, "Đã lưu khoản định kỳ", Toast.LENGTH_SHORT).show();
            titleInput.setText("");
            amountInput.setText("");
            renderRecurringList(recurringList);
        });

        renderRecurringList(recurringList);
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

    private void renderRecurringList(LinearLayout container) {
        container.removeAllViews();
        List<RecurringExpense> items = repository.getRecurring();
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        for (RecurringExpense item : items) {
            View row = inflater.inflate(R.layout.item_recurring, container, false);
            TextView title = row.findViewById(R.id.recurringTitle);
            TextView category = row.findViewById(R.id.recurringCategory);
            TextView amount = row.findViewById(R.id.recurringAmount);
            TextView date = row.findViewById(R.id.recurringDate);

            title.setText(item.getTitle());
            category.setText(item.getCategory());
            amount.setText(FormatUtils.formatCurrency(item.getAmount()));
            date.setText("Bắt đầu " + FormatUtils.formatDate(item.getStartDate()));
            container.addView(row);
        }
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
