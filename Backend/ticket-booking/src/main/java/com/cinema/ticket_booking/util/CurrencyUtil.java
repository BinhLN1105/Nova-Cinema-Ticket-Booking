package com.cinema.ticket_booking.util;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Tiện ích định dạng tiền tệ VND.
 */
public final class CurrencyUtil {

    private static final Locale VN_LOCALE = Locale.of("vi", "VN");

    private CurrencyUtil() {
    }

    /**
     * Format số tiền ra chuỗi VND.
     * VD: 150000 → "150.000 ₫"
     */
    public static String formatVnd(BigDecimal amount) {
        if (amount == null)
            return "0 ₫";
        NumberFormat fmt = NumberFormat.getCurrencyInstance(VN_LOCALE);
        return fmt.format(amount);
    }

    /**
     * Format ngắn gọn cho hiển thị trên card/badge.
     * VD: 150000 → "150.000đ"
     */
    public static String formatShort(BigDecimal amount) {
        if (amount == null)
            return "0đ";
        NumberFormat fmt = NumberFormat.getNumberInstance(VN_LOCALE);
        return fmt.format(amount) + "đ";
    }

    /**
     * Làm tròn lên đến bội số của 1.000 (VNPay yêu cầu số nguyên nghìn).
     * VD: 149.500 → 150.000
     */
    public static BigDecimal roundToThousand(BigDecimal amount) {
        if (amount == null)
            return BigDecimal.ZERO;
        BigDecimal thousand = new BigDecimal("1000");
        return amount.divide(thousand, 0, java.math.RoundingMode.CEILING)
                .multiply(thousand);
    }
}
