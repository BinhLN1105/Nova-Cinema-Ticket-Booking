package com.cinema.ticket_booking.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class VNPayUtils {

    /**
     * Sắp xếp các tham số và xây dựng chuỗi Hash Data theo chuẩn VNPay 2.1.0.
     * Sử dụng US_ASCII để encode các value của tham số, giữ nguyên dấu '+' (khoảng trắng)
     * để khớp 100% với cơ chế verify signature phía VNPAY.
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
                    sb.append(k).append('=').append(URLEncoder.encode(v, StandardCharsets.US_ASCII.toString()));
                } catch (Exception e) {
                    // Bỏ qua lỗi mã hóa
                }
            }
        });
        return sb.toString();
    }

    /**
     * Xây dựng chuỗi Query String gửi đi trên URL thanh toán theo chuẩn VNPay.
     * Sử dụng US_ASCII và encode cả key lẫn value.
     */
    public static String buildQueryString(Map<String, String> params) {
        Map<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();
        
        sortedParams.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                try {
                    sb.append(URLEncoder.encode(k, StandardCharsets.US_ASCII.toString()))
                            .append('=')
                            .append(URLEncoder.encode(v, StandardCharsets.US_ASCII.toString()));
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
