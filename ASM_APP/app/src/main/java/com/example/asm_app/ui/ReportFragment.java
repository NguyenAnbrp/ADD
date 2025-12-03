package com.example.asm_app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.LinearLayout;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;

import com.example.asm_app.LoginActivity;
import com.example.asm_app.R;
import com.example.asm_app.repositories.ExpenseRepository;
import com.example.asm_app.repositories.ExpenseRepository.CategorySpend;
import com.example.asm_app.util.FormatUtils;
import com.example.asm_app.util.SessionManager;

import java.util.Calendar;
import java.util.ArrayList;
import java.util.List;

public class ReportFragment extends Fragment {

    private SessionManager sessionManager;
    private ExpenseRepository repository;

    private TextView userName;
    private TextView userEmail;
    private TextView totalLimitText;
    private TextView totalSpentText;
    private TextView balanceText;
    private TextView extraLine1;
    private TextView extraLine2;
    private TextView extraLine3;
    private RadioGroup rangeGroup;
    private PieChartView pieChartView;
    private LinearLayout legendContainer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_report, container, false);
        sessionManager = new SessionManager(requireContext());
        if (sessionManager.getUserId() <= 0) {
            logout();
            return view;
        }
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
        extraLine1 = view.findViewById(R.id.reportExtra1);
        extraLine2 = view.findViewById(R.id.reportExtra2);
        extraLine3 = view.findViewById(R.id.reportExtra3);
        rangeGroup = view.findViewById(R.id.reportRangeGroup);
        pieChartView = view.findViewById(R.id.reportPieChart);
        legendContainer = view.findViewById(R.id.reportLegend);

        view.findViewById(R.id.reportLogoutBtn).setOnClickListener(v -> logout());

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

        bindPie(totalSpent, totalLimitWithRecurring);
        bindExtraStats(totalLimitWithRecurring, totalSpent, start, end);
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

    private void bindPie(double totalSpent, double totalLimitWithRecurring) {
        legendContainer.removeAllViews();
        List<PieChartView.Segment> segments = new ArrayList<>();

        long[] range = getSelectedRange();
        List<CategorySpend> categorySpends = repository.getCategorySpendBetween(range[0], range[1]);
        double remaining = totalLimitWithRecurring - totalSpent;
        if (remaining < 0) remaining = 0;

        double totalForPercent = totalSpent + remaining;
        if (totalForPercent <= 0) totalForPercent = 1; // avoid div by zero

        for (CategorySpend item : categorySpends) {
            if (item.spent <= 0) continue;
            segments.add(new PieChartView.Segment((float) item.spent, item.color));
            double percent = (item.spent / totalForPercent) * 100.0;
            addLegendItem(item.name + " (" + Math.round(percent) + "%)", item.spent, item.color);
        }

        // Remaining slice
        if (remaining > 0) {
            double percent = (remaining / totalForPercent) * 100.0;
            int remainingColor = ContextCompat.getColor(requireContext(), R.color.success_green);
            segments.add(new PieChartView.Segment((float) remaining, remainingColor));
            addLegendItem(getString(R.string.label_remaining) + " (" + Math.round(percent) + "%)", remaining, remainingColor);
        }

        if (segments.isEmpty()) {
            pieChartView.setSegments(new ArrayList<>());
            int gray = ContextCompat.getColor(requireContext(), R.color.gray_300);
            addLegendItem("Chưa có dữ liệu", 0, gray);
        } else {
            pieChartView.setSegments(segments);
        }
    }

    private void addLegendItem(String label, double amount, int colorInt) {
        View item = LayoutInflater.from(requireContext()).inflate(R.layout.item_pie_legend, legendContainer, false);
        View dot = item.findViewById(R.id.legendDot);
        TextView title = item.findViewById(R.id.legendLabel);
        TextView value = item.findViewById(R.id.legendValue);

        if (dot.getBackground() != null) {
            dot.getBackground().setTint(colorInt);
        } else {
            dot.setBackgroundColor(colorInt);
        }
        title.setText(label);
        value.setText(FormatUtils.formatCurrency(amount));
        legendContainer.addView(item);
    }

    private void bindExtraStats(double totalLimit, double totalSpent, long start, long end) {
        int checked = rangeGroup.getCheckedRadioButtonId();
        extraLine1.setVisibility(View.GONE);
        extraLine2.setVisibility(View.GONE);
        extraLine3.setVisibility(View.GONE);

        if (checked == R.id.reportRangeMonth) {
            // Average weekly spend in month
            double days = Math.max(1, (end - start + 1) / (1000.0 * 60 * 60 * 24));
            double avgWeekly = totalSpent / Math.max(1, (days / 7.0));
            boolean exceeded = totalSpent > totalLimit;
            extraLine1.setText("Chi tiêu TB tuần: " + FormatUtils.formatCurrency(avgWeekly));
            extraLine2.setText(exceeded ? "Đã vượt hạn mức tháng" : "Chưa vượt hạn mức tháng");
            extraLine1.setVisibility(View.VISIBLE);
            extraLine2.setVisibility(View.VISIBLE);
        } else if (checked == R.id.reportRangeYear) {
            double avgMonthlyLimit = totalLimit / 12.0;
            double avgMonthlyBalance = (totalLimit - totalSpent) / 12.0;
            int year = Calendar.getInstance().get(Calendar.YEAR);
            int monthsExceeded = repository.countMonthsExceededInYear(year);
            extraLine1.setText("Hạn mức TB tháng: " + FormatUtils.formatCurrency(avgMonthlyLimit));
            extraLine2.setText("Số dư TB tháng: " + FormatUtils.formatCurrency(avgMonthlyBalance));
            extraLine3.setText("Số tháng vượt hạn mức: " + monthsExceeded);
            extraLine1.setVisibility(View.VISIBLE);
            extraLine2.setVisibility(View.VISIBLE);
            extraLine3.setVisibility(View.VISIBLE);
        } else {
            // week view: no extra lines
        }
    }
}
