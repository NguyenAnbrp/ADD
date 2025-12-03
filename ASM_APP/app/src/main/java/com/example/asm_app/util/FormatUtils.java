package com.example.asm_app.util;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class FormatUtils {
    private static final NumberFormat CURRENCY_FORMAT = NumberFormat.getInstance(new Locale("vi", "VN"));
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    private FormatUtils() {
    }

    public static String formatCurrency(double amount) {
        String formatted = CURRENCY_FORMAT.format(Math.abs(amount));
        if (amount < 0) {
            return "-" + formatted + " đ";
        }
        return formatted + " đ";
    }

    public static String formatDate(Date date) {
        return DATE_FORMAT.format(date);
    }
}
