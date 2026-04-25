package com.cinema.ticket_booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ném khi không tìm thấy resource theo ID.
 * VD: findById() trả về empty → throw new ResourceNotFoundException("Phim không
 * tồn tại")
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object id) {
        super(resource + " không tồn tại với id: " + id);
    }
}
