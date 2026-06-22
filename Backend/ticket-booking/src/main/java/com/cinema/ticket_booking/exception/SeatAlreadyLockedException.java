package com.cinema.ticket_booking.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Ngoại lệ này được ném ra khi có xung đột giữ ghế (Race Condition).
 * Cụ thể: Khi một người dùng cố gắng đặt hoặc tạm giữ ghế trong lúc ghế đó đã
 * bị một phiên đặt vé (hoặc một giao dịch khác) lock trước trên Redis hoặc Database.
 * Kế thừa từ ConflictException nhằm trả về mã HTTP 409 Conflict cho phía Client.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class SeatAlreadyLockedException extends ConflictException {

    /**
     * Khởi tạo ngoại lệ với thông điệp chi tiết về ghế bị xung đột.
     *
     * @param message Thông điệp mô tả chi tiết (ví dụ: "Ghế A5 đang được giữ bởi người khác")
     */
    public SeatAlreadyLockedException(String message) {
        super(message);
    }
}
