package com.cinema.ticket_booking.util;

import com.cinema.ticket_booking.exception.*;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Các validation logic dùng chung trong Service layer.
 * Bổ sung cho @Valid trên DTO — dùng khi cần validate theo ngữ cảnh (business
 * rule).
 */
public final class ValidationUtil {

    private static final Pattern PHONE_VN = Pattern.compile("^(0|\\+84)[3-9][0-9]{8}$");
    private static final Pattern EMAIL_BASIC = Pattern.compile("^[\\w._%+\\-]+@[\\w.\\-]+\\.[a-zA-Z]{2,}$");

    private ValidationUtil() {
    }

    /** Kiểm tra số điện thoại Việt Nam hợp lệ */
    public static boolean isValidVietnamesePhone(String phone) {
        return phone != null && PHONE_VN.matcher(phone.trim()).matches();
    }

    /** Kiểm tra email hợp lệ cơ bản */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_BASIC.matcher(email.trim()).matches();
    }

    /** Kiểm tra thời gian kết thúc phải sau thời gian bắt đầu */
    public static boolean isValidTimeRange(LocalDateTime start, LocalDateTime end) {
        return start != null && end != null && end.isAfter(start);
    }

    /** Kiểm tra thời gian trong tương lai */
    public static boolean isFuture(LocalDateTime dt) {
        return dt != null && dt.isAfter(LocalDateTime.now());
    }

    /**
     * Ném IllegalArgumentException với message nếu điều kiện false.
     * Dùng để guard đầu vào trong Service method.
     *
     * VD: ValidationUtil.require(amount.compareTo(BigDecimal.ZERO) > 0, "Số tiền
     * phải lớn hơn 0")
     */
    public static void require(boolean condition, String message) {
        if (!condition)
            throw new BadRequestException(message);
    }

    /** Ném ResourceNotFoundException nếu object là null */
    public static <T> T requireNonNull(T obj, String resourceName, Object id) {
        if (obj == null)
            throw new ResourceNotFoundException(resourceName, id);
        return obj;
    }
}
