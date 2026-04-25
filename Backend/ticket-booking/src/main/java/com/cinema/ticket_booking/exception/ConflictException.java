package com.cinema.ticket_booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ném khi tạo resource bị xung đột với dữ liệu đã tồn tại.
 * VD: email đã đăng ký, mã voucher trùng, lịch chiếu bị trùng giờ.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
