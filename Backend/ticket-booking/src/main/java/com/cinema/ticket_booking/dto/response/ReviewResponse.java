package com.cinema.ticket_booking.dto.response;

import lombok.Builder;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import java.io.Serializable;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse implements Serializable {
    private static final long serialVersionUID = 1L;

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
