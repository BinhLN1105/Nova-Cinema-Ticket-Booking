package com.cinema.ticket_booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ErrorResponse {
    private int status; // Mã lỗi HTTP (400, 401, 404, 500...)
    private String message; // Mô tả lỗi ngắn gọn
    private String error; // Tên lỗi (Ví dụ: Bad Request)
    private String path; // Đường dẫn API bị lỗi cho việc debug sau này
    private Long timestamp; // Thời gian xảy ra lỗi
}
