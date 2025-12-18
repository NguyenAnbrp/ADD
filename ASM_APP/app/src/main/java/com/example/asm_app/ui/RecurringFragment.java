package com.example.asm_app.ui;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.asm_app.R;
import com.example.asm_app.model.RecurringExpense;
import com.example.asm_app.repositories.ExpenseRepository;
import com.example.asm_app.util.FormatUtils;
import com.example.asm_app.util.SessionManager;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class RecurringFragment extends Fragment {

    private EditText dateInput;
    private EditText titleInput;
    private EditText amountInput;
    private LinearLayout recurringList;
    private ExpenseRepository repository;
    private SessionManager sessionManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recurring, container, false);
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
        renderRecurringList();
    }

    private void setup(View view) {
        Button addRecurringBtn = view.findViewById(R.id.addRecurringBtn);
        recurringList = view.findViewById(R.id.recurringList);
        dateInput = view.findViewById(R.id.recurringDateInput);
        titleInput = view.findViewById(R.id.recurringTitleInput);
        amountInput = view.findViewById(R.id.recurringAmountInput);

        Calendar calendar = Calendar.getInstance();
        dateInput.setText(FormatUtils.formatDate(calendar.getTime()));
        dateInput.setOnClickListener(v -> showDatePicker(calendar));

        addRecurringBtn.setOnClickListener(v -> saveRecurring());

        renderRecurringList();
    }

    private void saveRecurring() {
        String title = titleInput.getText().toString().trim();
        String amountText = amountInput.getText().toString().trim();
        if (title.isEmpty() || amountText.isEmpty()) {
            Toast.makeText(requireContext(), "Enter description and amount", Toast.LENGTH_SHORT).show();
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
        long categoryId = -1;
        repository.addRecurring(title, amount, categoryId, chosen.getTime());
        Toast.makeText(requireContext(), "Recurring expense saved", Toast.LENGTH_SHORT).show();
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
            empty.setText("No recurring expenses yet.");
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
            date.setText("Starts " + FormatUtils.formatDate(item.getStartDate()));
            
            View editBtn = row.findViewById(R.id.editRecurringBtn);
            View deleteBtn = row.findViewById(R.id.deleteRecurringBtn);

            // Xử lý Xóa
            deleteBtn.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(requireContext())
                    .setTitle("Confirm Delete")
                    .setMessage("Are you sure you want to delete this recurring expense?")
                    .setPositiveButton("Delete", (dialog, which) -> {
                        if (repository.deleteRecurring(item.getId())) {
                            Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show();
                            renderRecurringList();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            });

            // Xử lý Sửa (Đổ dữ liệu lên form phía trên để sửa)
            editBtn.setOnClickListener(v -> {
                titleInput.setText(item.getTitle());
                amountInput.setText(String.valueOf(item.getAmount()));
                dateInput.setText(FormatUtils.formatDate(item.getStartDate()));
                
                // Thay đổi nút "Add" thành "Update"
                Button mainBtn = getView().findViewById(R.id.addRecurringBtn);
                mainBtn.setText("Update Expense");
                mainBtn.setOnClickListener(v2 -> {
                    // Gọi repository.updateRecurring(...) tương tự saveRecurring()
                    // Sau đó set lại nút về mặc định
                });
            });
            recurringList.addView(row);
        }
    }

    private java.util.Date parseDate(String value) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.parse(value);
        } catch (Exception e) {
            return new java.util.Date();
        }
    }
}
