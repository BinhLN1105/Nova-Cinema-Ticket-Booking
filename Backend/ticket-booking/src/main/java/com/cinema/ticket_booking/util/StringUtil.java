package com.cinema.ticket_booking.util;

import java.security.SecureRandom;
import java.util.UUID;

/**
 * Các tiện ích xử lý chuỗi dùng chung.
 */
public final class StringUtil {

    private static final String ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private StringUtil() {
    }

    /**
     * Tạo mã ngẫu nhiên gồm chữ hoa + số.
     * VD: generateCode(8) → "K7X2P9QA"
     * Dùng cho: booking code, voucher code,...
     */
    public static String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHANUMERIC.charAt(RANDOM.nextInt(ALPHANUMERIC.length())));
        }
        return sb.toString();
    }

    /**
     * Tạo UUID dạng chuỗi không có dấu gạch ngang (32 ký tự hex).
     * Dùng cho refresh token, QR signature,...
     */
    public static String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * Mask email để hiển thị an toàn.
     * VD: "nguyenvana@gmail.com" → "ngu***@gmail.com"
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@"))
            return email;
        String[] parts = email.split("@");
        String name = parts[0];
        String domain = parts[1];
        if (name.length() <= 3)
            return email;
        return name.substring(0, 3) + "***@" + domain;
    }

    /**
     * Mask số điện thoại.
     * VD: "0912345678" → "091***5678"
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7)
            return phone;
        return phone.substring(0, 3) + "***" + phone.substring(phone.length() - 4);
    }

    /** Kiểm tra chuỗi null hoặc blank */
    public static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Cắt chuỗi an toàn, không ném exception */
    public static String truncate(String s, int maxLength) {
        if (s == null)
            return null;
        return s.length() <= maxLength ? s : s.substring(0, maxLength) + "...";
    }
}
