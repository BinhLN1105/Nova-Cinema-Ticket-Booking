package com.cinema.ticket_booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ném khi user đã đăng nhập nhưng không có quyền thực hiện thao tác.
 * VD: CUSTOMER cố gắng truy cập API của ADMIN.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }

    public ForbiddenException() {
        super("Bạn không có quyền thực hiện thao tác này");
    }
}
