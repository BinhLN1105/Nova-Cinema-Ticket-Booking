package com.cinema.ticket_booking.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PromotionResponse {

    String id;

    String title;

    String description;

    String imageUrl;

    String targetUrl;

    LocalDateTime startDate;

    LocalDateTime endDate;

    Integer priority;

    Boolean isActive;

    LocalDateTime createdAt;
}
