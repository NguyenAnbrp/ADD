package com.example.asm_app.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
            "Red",
            "Blue",
            "Yellow",
            "Green",
            "Teal",
            "Dark gray",
            "Light blue",
            "Light gray"
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
            empty.setText("No budget categories yet. Add one to start tracking.");
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
            TextView actionBtn = item.findViewById(R.id.budgetAction);
            ImageButton deleteBtn = item.findViewById(R.id.budgetDeleteBtn);

            avatar.setText(firstLetter(category.getName()));
            ViewCompat.setBackgroundTintList(avatar, ColorStateList.valueOf(category.getColorRes()));
            name.setText(category.getName());
            spent.setText(FormatUtils.formatCurrency(category.getSpent()));

            if (category.getLimit() != null && category.getLimit() > 0) {
                int percent = Math.min(100, (int) ((category.getSpent() / category.getLimit()) * 100));
                progressBar.setProgress(percent);
                limitText.setText("Used " + percent + "% / " + FormatUtils.formatCurrency(category.getLimit()));
                actionBtn.setText("Edit limit");
            } else {
                progressBar.setProgress(0);
                limitText.setText("No limit set");
                actionBtn.setText("+ Set limit");
            }

            // Set limit button click
            actionBtn.setOnClickListener(v -> showSetLimitDialog(category));

            // Delete button click
            deleteBtn.setOnClickListener(v -> showDeleteCategoryDialog(category));

            budgetList.addView(item);
        }
    }

    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_category, null);
        EditText nameInput = dialogView.findViewById(R.id.categoryNameInput);
        EditText limitInput = dialogView.findViewById(R.id.categoryLimitInput);
        Spinner colorSpinner = dialogView.findViewById(R.id.categoryColorSpinner);

        // Create adapter with placeholder
        String[] labelsWithPlaceholder = new String[colorLabels.length + 1];
        labelsWithPlaceholder[0] = "Select color";
        System.arraycopy(colorLabels, 0, labelsWithPlaceholder, 1, colorLabels.length);
        
        int[] paletteWithPlaceholder = new int[colorPalette.length + 1];
        paletteWithPlaceholder[0] = R.color.gray_300; // Placeholder color
        System.arraycopy(colorPalette, 0, paletteWithPlaceholder, 1, colorPalette.length);
        
        ColorSpinnerAdapter adapter = new ColorSpinnerAdapter(requireContext(), labelsWithPlaceholder, paletteWithPlaceholder, true);
        colorSpinner.setAdapter(adapter);
        
        // Set initial selection to placeholder (position 0)
        colorSpinner.setSelection(0);
        
        // Add listener to refresh view when selection changes
        colorSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                // Update spinner background color when a color is selected
                if (position > 0) { // Not placeholder
                    int colorRes = colorPalette[(position - 1) % colorPalette.length];
                    int color = ContextCompat.getColor(requireContext(), colorRes);
                    // Set spinner background to selected color
                    GradientDrawable spinnerBg = new GradientDrawable();
                    spinnerBg.setShape(GradientDrawable.RECTANGLE);
                    float density = requireContext().getResources().getDisplayMetrics().density;
                    spinnerBg.setCornerRadius(12 * density);
                    spinnerBg.setColor(color);
                    int borderColor = ContextCompat.getColor(requireContext(), R.color.gray_300);
                    spinnerBg.setStroke((int)(1 * density), borderColor);
                    colorSpinner.setBackground(spinnerBg);
                } else {
                    // Reset to default background when placeholder is selected
                    colorSpinner.setBackground(ContextCompat.getDrawable(requireContext(), R.drawable.bg_input));
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        new AlertDialog.Builder(requireContext())
                .setTitle("Add category")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String name = nameInput.getText().toString().trim();
                    String limitText = limitInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(), "Category name cannot be empty", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Double limit = null;
                    if (!limitText.isEmpty()) {
                        try {
                            limit = Double.parseDouble(limitText);
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), "Invalid limit amount", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    int selectedPosition = colorSpinner.getSelectedItemPosition();
                    if (selectedPosition <= 0) {
                        Toast.makeText(requireContext(), "Please select a color", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int colorRes = colorPalette[(selectedPosition - 1) % colorPalette.length];
                    int color = ContextCompat.getColor(requireContext(), colorRes);
                    long result = repository.addCategory(name, color, limit);
                    if (result == -1) {
                        Toast.makeText(requireContext(), "Category already exists", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(requireContext(), "Category saved", Toast.LENGTH_SHORT).show();
                    }
                    renderBudgets();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSetLimitDialog(BudgetCategory category) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_set_limit, null);
        EditText limitInput = dialogView.findViewById(R.id.limitInput);

        // Pre-fill current limit if exists
        if (category.getLimit() != null && category.getLimit() > 0) {
            limitInput.setText(String.valueOf(category.getLimit()));
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Set limit for " + category.getName())
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String limitText = limitInput.getText().toString().trim();
                    Double limit = null;
                    if (!limitText.isEmpty()) {
                        try {
                            limit = Double.parseDouble(limitText);
                            if (limit <= 0) {
                                Toast.makeText(requireContext(), "Limit must be greater than 0", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } catch (NumberFormatException e) {
                            Toast.makeText(requireContext(), "Invalid limit amount", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    boolean success = repository.updateCategoryLimit(category.getId(), limit);
                    if (success) {
                        Toast.makeText(requireContext(), "Limit updated", Toast.LENGTH_SHORT).show();
                        renderBudgets();
                    } else {
                        Toast.makeText(requireContext(), "Failed to update limit", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Remove limit", (dialog, which) -> {
                    boolean success = repository.updateCategoryLimit(category.getId(), null);
                    if (success) {
                        Toast.makeText(requireContext(), "Limit removed", Toast.LENGTH_SHORT).show();
                        renderBudgets();
                    } else {
                        Toast.makeText(requireContext(), "Failed to remove limit", Toast.LENGTH_SHORT).show();
                    }
                })
                .show();
    }

    private void showDeleteCategoryDialog(BudgetCategory category) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete category")
                .setMessage("Are you sure you want to delete \"" + category.getName() + "\"?\n\nAll expenses in this category will become uncategorized.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    if (repository.hasExpensesForCategory(category.getId())) {
                        Toast.makeText(requireContext(),
                                "Cannot delete. There are transactions in this category.",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    boolean success = repository.deleteCategory(category.getId());
                    if (success) {
                        Toast.makeText(requireContext(), "Category deleted", Toast.LENGTH_SHORT).show();
                        renderBudgets();
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete category", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String firstLetter(String value) {
        if (value == null || value.isEmpty()) {
            return "?";
        }
        return value.substring(0, 1).toUpperCase();
    }

    private class ColorSpinnerAdapter extends ArrayAdapter<String> {
        private final String[] labels;
        private final int[] colorPalette;
        private final boolean hasPlaceholder;

        public ColorSpinnerAdapter(Context context, String[] labels, int[] colorPalette, boolean hasPlaceholder) {
            super(context, R.layout.item_color_spinner, labels);
            this.labels = labels;
            this.colorPalette = colorPalette;
            this.hasPlaceholder = hasPlaceholder;
        }

        @NonNull
        @Override
        public View getView(int position, @androidx.annotation.Nullable View convertView, @NonNull ViewGroup parent) {
            // Always create new view or update existing one to ensure it shows the selected color
            View view = getCustomView(position, convertView, parent);
            // Force update the view
            view.invalidate();
            return view;
        }

        @Override
        public View getDropDownView(int position, @androidx.annotation.Nullable View convertView, @NonNull ViewGroup parent) {
            return getCustomView(position, convertView, parent);
        }

        private View getCustomView(int position, View convertView, ViewGroup parent) {
            View view;
            if (convertView == null) {
                view = LayoutInflater.from(getContext()).inflate(R.layout.item_color_spinner, parent, false);
            } else {
                view = convertView;
            }

            View colorPreview = view.findViewById(R.id.colorPreview);
            TextView colorLabel = view.findViewById(R.id.colorLabel);

            if (position >= 0 && position < labels.length && colorPreview != null && colorLabel != null) {
                int colorRes = colorPalette[position % colorPalette.length];
                int color = ContextCompat.getColor(getContext(), colorRes);
                
                // Always create new drawable to ensure it's updated
                GradientDrawable drawable = new GradientDrawable();
                drawable.setShape(GradientDrawable.RECTANGLE);
                float density = getContext().getResources().getDisplayMetrics().density;
                drawable.setCornerRadius(6 * density);
                drawable.setColor(color);
                int borderColor = ContextCompat.getColor(getContext(), R.color.gray_300);
                drawable.setStroke((int)(2 * density), borderColor);
                colorPreview.setBackground(drawable);
                
                // For placeholder, show hint text style
                if (hasPlaceholder && position == 0) {
                    colorLabel.setText(labels[position]);
                    colorLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.gray_500));
                } else {
                    colorLabel.setText(labels[position]);
                    colorLabel.setTextColor(ContextCompat.getColor(getContext(), R.color.navy_900));
                }
            }

            return view;
        }
    }
}
