package com.cinema.ticket_booking.service.impl;

import com.cinema.ticket_booking.model.Booking;
import com.cinema.ticket_booking.service.QrCodeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class QrCodeServiceImpl implements QrCodeService {

    private final ObjectMapper objectMapper;

    @Value("${app.qr.secret}")
    private String qrSecret;

    @Override
    public String generateQrContent(Booking booking) {
        try {
            Map<String, String> payload = Map.of(
                    "bookingId", booking.getId().toString(),
                    "bookingCode", booking.getBookingCode());

            String json = objectMapper.writeValueAsString(payload);
            String b64 = Base64.getUrlEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
            String signature = hmacSha256(b64, qrSecret);

            return b64 + "." + signature;
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo QR code", e);
        }
    }

    @Override
    public boolean verifyQrContent(String qrContent) {
        try {
            String[] parts = qrContent.split("\\.");
            if (parts.length != 2)
                return false;
            String expected = hmacSha256(parts[0], qrSecret);
            return expected.equals(parts[1]);
        } catch (Exception e) {
            return false;
        }
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
    }
}
