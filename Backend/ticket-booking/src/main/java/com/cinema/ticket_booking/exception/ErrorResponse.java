package com.cinema.ticket_booking.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Cấu trúc JSON trả về khi có lỗi.
 * {
 * "success": false,
 * "status": 404,
 * "message": "Phim không tồn tại",
 * "errors": { "email": "Email đã tồn tại" }, // chỉ có khi validation lỗi
 * "timestamp": "2024-12-01T10:00:00"
 * }
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private boolean success;
    private int status;
    private String message;
    private Map<String, String> errors; // field-level validation errors

    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
}
