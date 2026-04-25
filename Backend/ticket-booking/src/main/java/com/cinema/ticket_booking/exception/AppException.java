package com.cinema.ticket_booking.exception;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AppException extends RuntimeException {
    private HttpStatus status;  // Mã lỗi HTTP (400, 401, 404, 500...)
    private String message;   // Mô tả lỗi ngắn gọn

    public AppException(HttpStatus status, String message) {
        super(message); // Gọi constructor của RuntimeException để ghi nhận lỗi
        this.status = status; // Gán mã lỗi HTTP
        this.message = message; // Gán mô tả lỗi
    }
}
