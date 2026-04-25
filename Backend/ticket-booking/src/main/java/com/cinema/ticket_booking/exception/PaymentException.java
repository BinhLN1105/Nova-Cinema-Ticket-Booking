package com.cinema.ticket_booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ném khi có lỗi liên quan đến thanh toán VNPay.
 * VD: chữ ký không hợp lệ, giao dịch thất bại, booking đã được thanh toán.
 */
@ResponseStatus(HttpStatus.PAYMENT_REQUIRED)
public class PaymentException extends RuntimeException {

    public PaymentException(String message) {
        super(message);
    }
}
