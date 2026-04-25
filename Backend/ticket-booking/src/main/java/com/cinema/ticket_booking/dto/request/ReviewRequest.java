package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewRequest {

    @NotBlank(message = "movieId không được để trống")
    private String movieId;

    // Booking xác minh đã xem phim
    @NotBlank(message = "bookingId không được để trống")
    private String bookingId;

    @NotNull(message = "Rating không được để trống")
    @Min(value = 1, message = "Rating tối thiểu là 1")
    @Max(value = 5, message = "Rating tối đa là 5")
    private Integer rating;

    @Size(max = 1000, message = "Nhận xét không quá 1000 ký tự")
    private String comment;
}
