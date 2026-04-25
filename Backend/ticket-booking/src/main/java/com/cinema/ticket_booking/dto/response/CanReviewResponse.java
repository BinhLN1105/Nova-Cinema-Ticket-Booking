package com.cinema.ticket_booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CanReviewResponse {
    private boolean canReview;
    private boolean alreadyReviewed;
    private UUID bookingId; // ID của vé đủ điều kiện (nếu có)
    private ReviewResponse existingReview; // Thông tin đánh giá cũ (nếu đã đánh giá)
}
