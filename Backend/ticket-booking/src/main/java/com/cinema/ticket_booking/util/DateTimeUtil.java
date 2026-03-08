package com.cinema.ticket_booking.util;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Locale;

/**
 * Tiện ích xử lý ngày giờ dùng chung trong project.
 */
public final class DateTimeUtil {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
    private static final DateTimeFormatter VNPAY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final Locale VI_LOCALE = new Locale("vi", "VN");

    private DateTimeUtil() {
    }

    /** "25/12/2024" */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    /** "20:30" */
    public static String formatTime(LocalDateTime dt) {
        return dt != null ? dt.format(TIME_FORMATTER) : "";
    }

    /** "20:30 - 25/12/2024" */
    public static String formatDateTime(LocalDateTime dt) {
        return dt != null ? dt.format(DATETIME_FORMATTER) : "";
    }

    /** "20241225203000" — dùng cho VNPay */
    public static String toVnpayFormat(LocalDateTime dt) {
        return dt != null ? dt.format(VNPAY_FORMATTER) : "";
    }

    /** "Thứ Tư, 25/12/2024" */
    public static String formatDateWithDayOfWeek(LocalDate date) {
        if (date == null)
            return "";
        String dow = date.getDayOfWeek().getDisplayName(TextStyle.FULL, VI_LOCALE);
        // Viết hoa chữ đầu
        dow = Character.toUpperCase(dow.charAt(0)) + dow.substring(1);
        return dow + ", " + date.format(DATE_FORMATTER);
    }

    /** Trả về true nếu ngày là cuối tuần (để tính giá vé cao điểm) */
    public static boolean isWeekend(LocalDateTime dt) {
        DayOfWeek dow = dt.getDayOfWeek();
        return dow == DayOfWeek.SATURDAY || dow == DayOfWeek.SUNDAY;
    }

    /** Trả về true nếu giờ là khung giờ vàng (18:00–22:00) */
    public static boolean isPeakHour(LocalDateTime dt) {
        int hour = dt.getHour();
        return hour >= 18 && hour < 22;
    }

    /** Số phút còn lại đến thời điểm hết hạn (âm nếu đã quá hạn) */
    public static long minutesUntil(LocalDateTime deadline) {
        return java.time.Duration.between(LocalDateTime.now(), deadline).toMinutes();
    }
}
