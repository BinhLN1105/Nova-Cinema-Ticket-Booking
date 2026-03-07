package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReviewResponse {

    private String id;
    private String movieId;

    // Thông tin người đánh giá
    private String userId;
    private String userFullName;
    private String userAvatarUrl;

    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
