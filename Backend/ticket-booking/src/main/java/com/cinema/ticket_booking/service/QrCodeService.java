package com.cinema.ticket_booking.service;

import com.cinema.ticket_booking.model.Booking;

public interface QrCodeService {

    /**
     * Tạo nội dung QR dạng: base64(json) + "." + HMAC-SHA256(json)
     * Android dùng ZXing để đọc chuỗi này, sau đó gửi lên API /checkin.
     */
    String generateQrContent(Booking booking);

    /**
     * Xác minh QR trước khi check-in (optional — dùng ở tầng Service nếu cần).
     */
    boolean verifyQrContent(String qrContent);
}
