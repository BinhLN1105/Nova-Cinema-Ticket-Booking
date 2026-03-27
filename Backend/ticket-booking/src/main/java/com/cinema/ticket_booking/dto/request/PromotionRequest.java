package com.cinema.ticket_booking.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionRequest {

    @NotBlank(message = "Tiêu đề không được để trống")
    String title;

    String description;

    @NotBlank(message = "URL hình ảnh không được để trống")
    String imageUrl;

    String targetUrl;

    LocalDate startDate;

    LocalDate endDate;

    @jakarta.validation.constraints.Min(0)
    Integer priority = 0;

    Boolean isActive = true;
}
