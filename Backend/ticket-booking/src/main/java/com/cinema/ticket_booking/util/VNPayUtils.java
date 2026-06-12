package com.cinema.ticket_booking.util;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

public class VNPayUtils {

    /**
     * Sắp xếp các tham số và xây dựng chuỗi Hash Data theo chuẩn VNPay 2.1.0.
     * Sử dụng US_ASCII để encode các value của tham số, giữ nguyên dấu '+' (khoảng
     * trắng)
     * để khớp 100% với cơ chế verify signature phía VNPAY.
     */
    public static String buildHashData(Map<String, String> params) {
        Map<String, String> sortedParams = new TreeMap<>(params);
        StringBuilder sb = new StringBuilder();

        sortedParams.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                if (!sb.isEmpty()) {
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
                if (!sb.isEmpty()) {
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

    /**
     * Xác thực chữ ký VNPay sử dụng chung cho các service.
     */
    public static boolean verifySignature(Map<String, String> params, String receivedHash, String secret) {
        if (receivedHash == null || receivedHash.isBlank()) {
            return false;
        }
        Map<String, String> vnpParams = new HashMap<>(params);
        vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        Map<String, String> filtered = new TreeMap<>();
        vnpParams.forEach((k, v) -> {
            if (k.startsWith("vnp_") && v != null && !v.isBlank()) {
                filtered.put(k, v);
            }
        });

        try {
            String hashData = buildHashData(filtered);
            String computedHash = hmacSha512(hashData, secret);
            return computedHash.equalsIgnoreCase(receivedHash);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Tạo URL thanh toán VNPay dùng chung cho các service.
     */
    public static String buildPaymentUrl(
            String vnpayUrl,
            String tmnCode,
            String hashSecret,
            BigDecimal amount,
            String txnRef,
            String orderInfo,
            String returnUrl) {
        try {
            Map<String, String> params = new TreeMap<>();
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "pay");
            params.put("vnp_TmnCode", tmnCode);
            params.put("vnp_Amount", String.valueOf(amount.multiply(BigDecimal.valueOf(100)).longValue()));
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", txnRef);
            params.put("vnp_OrderInfo", orderInfo);
            params.put("vnp_OrderType", "190000");
            params.put("vnp_Locale", "vn");
            params.put("vnp_ReturnUrl", returnUrl);
            params.put("vnp_IpAddr", "127.0.0.1");
            params.put("vnp_CreateDate", LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")));

            String hashData = buildHashData(params);
            String signature = hmacSha512(hashData, hashSecret);
            String queryString = buildQueryString(params) + "&vnp_SecureHash=" + signature;

            return vnpayUrl + "?" + queryString;
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo URL thanh toán VNPay", e);
        }
    }
}
