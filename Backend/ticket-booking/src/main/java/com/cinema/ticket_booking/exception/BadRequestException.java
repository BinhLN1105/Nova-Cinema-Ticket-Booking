package com.cinema.ticket_booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ném khi client gửi request không hợp lệ về mặt business logic.
 * VD: ghế đã bị đặt, voucher hết hạn, đặt vé trùng,...
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
