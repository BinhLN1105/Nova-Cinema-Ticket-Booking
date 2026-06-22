package com.cinema.ticket_booking.util;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class VNPayUtilsTest {

    @Test
    void testBuildHashData_NullOrBlankValue() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Blank", "");
        params.put("vnp_Null", null);
        params.put("vnp_Command", "pay");

        String result = VNPayUtils.buildHashData(params);
        assertEquals("vnp_Command=pay&vnp_Version=2.1.0", result);
    }

    @Test
    void testBuildQueryString_NullOrBlankValue() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Blank", "");
        params.put("vnp_Null", null);
        params.put("vnp_Command", "pay");

        String result = VNPayUtils.buildQueryString(params);
        assertEquals("vnp_Command=pay&vnp_Version=2.1.0", result);
    }

    @Test
    void testHmacSha512_Success() throws Exception {
        String data = "vnp_Amount=1000000&vnp_Command=pay";
        String secret = "secret_key";
        String hash = VNPayUtils.hmacSha512(data, secret);
        assertNotNull(hash);
        assertFalse(hash.isBlank());
    }

    @Test
    void testVerifySignature_ReceivedHashBlank() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_TxnRef", "12345");
        assertFalse(VNPayUtils.verifySignature(params, "", "secret"));
        assertFalse(VNPayUtils.verifySignature(params, null, "secret"));
    }

    @Test
    void testVerifySignature_ValidSignature() throws Exception {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_SecureHash", "dummy");

        String hashData = VNPayUtils.buildHashData(params);
        // Exclude secure hash key
        Map<String, String> cleanParams = new HashMap<>(params);
        cleanParams.remove("vnp_SecureHash");
        String cleanHashData = VNPayUtils.buildHashData(cleanParams);

        String secret = "my_secret_key";
        String expectedHash = VNPayUtils.hmacSha512(cleanHashData, secret);

        assertTrue(VNPayUtils.verifySignature(params, expectedHash, secret));
    }

    @Test
    void testVerifySignature_InvalidSignature() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        assertFalse(VNPayUtils.verifySignature(params, "wrong_hash", "secret"));
    }

    @Test
    void testVerifySignature_ExceptionThrown() {
        Map<String, String> params = new HashMap<>();
        params.put("vnp_Version", "2.1.0");
        // Passing null secret will cause NullPointerException in SecretKeySpec which should be caught
        assertFalse(VNPayUtils.verifySignature(params, "some_hash", null));
    }

    @Test
    void testBuildPaymentUrl_Success() {
        String vnpayUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
        String tmnCode = "DEMO123";
        String hashSecret = "secret";
        BigDecimal amount = new BigDecimal("10000");
        String txnRef = "TXN12345";
        String orderInfo = "Thanh toan ve phim";
        String returnUrl = "http://localhost:8080/callback";

        String paymentUrl = VNPayUtils.buildPaymentUrl(vnpayUrl, tmnCode, hashSecret, amount, txnRef, orderInfo, returnUrl);
        assertNotNull(paymentUrl);
        assertTrue(paymentUrl.startsWith(vnpayUrl));
        assertTrue(paymentUrl.contains("vnp_TmnCode=" + tmnCode));
        assertTrue(paymentUrl.contains("vnp_Amount=1000000")); // 10000 * 100
        assertTrue(paymentUrl.contains("vnp_SecureHash="));
    }

    @Test
    void testBuildPaymentUrl_Exception() {
        // null amount causes NullPointerException, which gets wrapped in RuntimeException
        assertThrows(RuntimeException.class, () -> {
            VNPayUtils.buildPaymentUrl("url", "tmn", "secret", null, "ref", "info", "return");
        });
    }
}
