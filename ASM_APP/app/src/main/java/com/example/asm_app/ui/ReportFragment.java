package com.example.asm_app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.asm_app.LoginActivity;
import com.example.asm_app.R;
import com.example.asm_app.repositories.ExpenseRepository;
import com.example.asm_app.util.FormatUtils;
import com.example.asm_app.util.SessionManager;

import java.util.Calendar;

public class ReportFragment extends Fragment {

    private SessionManager sessionManager;
    private ExpenseRepository repository;

    private TextView userName;
    private TextView userEmail;
    private TextView totalLimitText;
    private TextView totalSpentText;
    private TextView balanceText;
    private RadioGroup rangeGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);
        sessionManager = new SessionManager(requireContext());
        repository = new ExpenseRepository(requireContext(), sessionManager.getUserId());
        bindViews(view);
        bindUserInfo();
        bindReport();
        return view;
    }

    private void bindViews(View view) {
        userName = view.findViewById(R.id.reportUserName);
        userEmail = view.findViewById(R.id.reportUserEmail);
        totalLimitText = view.findViewById(R.id.reportTotalLimit);
        totalSpentText = view.findViewById(R.id.reportTotalSpent);
        balanceText = view.findViewById(R.id.reportBalance);
        rangeGroup = view.findViewById(R.id.reportRangeGroup);

        view.findViewById(R.id.reportLogoutBtn).setOnClickListener(v -> logout());
        view.findViewById(R.id.reportApplyBtn).setOnClickListener(v -> bindReport());

        rangeGroup.setOnCheckedChangeListener((group, checkedId) -> bindReport());
    }

    private void bindUserInfo() {
        userName.setText(sessionManager.getUserName().isEmpty() ? "Người dùng" : sessionManager.getUserName());
        userEmail.setText(sessionManager.getUserEmail().isEmpty() ? "Chưa có email" : sessionManager.getUserEmail());
    }

    private void bindReport() {
        long[] range = getSelectedRange();
        long start = range[0];
        long end = range[1];

        // Tổng hạn mức (cộng thêm định kỳ để không âm)
        double limit = repository.getTotalLimit();
        double recurringPlanned = repository.getRecurringPlannedForCurrentMonth();
        double totalLimitWithRecurring = limit + recurringPlanned;

        double spentTransactions = repository.getExpensesTotalBetween(start, end);
        double spentRecurring = repository.getRecurringTotalBetween(start, end);
        double totalSpent = spentTransactions + spentRecurring;
        double balance = totalLimitWithRecurring - totalSpent;

        totalLimitText.setText(FormatUtils.formatCurrency(totalLimitWithRecurring));
        totalSpentText.setText(FormatUtils.formatCurrency(totalSpent));
        balanceText.setText(FormatUtils.formatCurrency(balance));
    }

    private long[] getSelectedRange() {
        Calendar cal = Calendar.getInstance();
        int checked = rangeGroup.getCheckedRadioButtonId();
        if (checked == R.id.reportRangeMonth) {
            cal.set(Calendar.DAY_OF_MONTH, 1);
        } else if (checked == R.id.reportRangeYear) {
            cal.set(Calendar.DAY_OF_YEAR, 1);
        } else {
            // Tuần: đưa về đầu tuần (thứ 2)
            cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        }
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long start = cal.getTimeInMillis();

        Calendar end = Calendar.getInstance();
        end.setTimeInMillis(start);
        if (checked == R.id.reportRangeMonth) {
            end.add(Calendar.MONTH, 1);
        } else if (checked == R.id.reportRangeYear) {
            end.add(Calendar.YEAR, 1);
        } else {
            end.add(Calendar.WEEK_OF_YEAR, 1);
        }
        end.add(Calendar.MILLISECOND, -1);
        return new long[]{start, end.getTimeInMillis()};
    }

    private void logout() {
        sessionManager.clear();
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        requireActivity().finishAffinity();
    }
}
