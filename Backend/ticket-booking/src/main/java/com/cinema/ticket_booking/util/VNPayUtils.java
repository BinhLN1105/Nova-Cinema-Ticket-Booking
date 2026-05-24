package com.cinema.ticket_booking.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VNPayUtils {

    /**
     * Sắp xếp các tham số và xây dựng chuỗi Hash Data theo chuẩn VNPay.
     * Đặc biệt tự động thay thế dấu '+' (sinh ra từ URLEncoder) thành '%20'
     * để khớp hoàn toàn với chuẩn mã hóa của cổng thanh toán VNPay.
     */
    public static String buildHashData(Map<String, String> params) {
        Map<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        
        sortedParams.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                try {
                    // VNPay chuẩn yêu cầu khoảng trắng mã hóa thành %20 chứ không phải dấu +
                    String encodedValue = URLEncoder.encode(v, StandardCharsets.UTF_8.toString()).replace("+", "%20");
                    sb.append(k).append('=').append(encodedValue);
                } catch (Exception e) {
                    // Bỏ qua lỗi mã hóa
                }
            }
        });
        return sb.toString();
    }

    /**
     * Tính toán chữ ký HmacSHA512 đồng bộ sử dụng chung bảng mã UTF-8.
     */
    public static String hmacSha512(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA512");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA512"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : raw) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
