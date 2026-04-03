package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class CanReviewResponse {
    @SerializedName("canReview")
    private boolean canReview;
    
    @SerializedName("alreadyReviewed")
    private boolean alreadyReviewed;
    
    @SerializedName("bookingId")
    private String bookingId;
    
    @SerializedName("existingReview")
    private ReviewResponse existingReview;

    public boolean isCanReview() {
        return canReview;
    }

    public boolean isAlreadyReviewed() {
        return alreadyReviewed;
    }

    public String getBookingId() {
        return bookingId;
    }

    public ReviewResponse getExistingReview() {
        return existingReview;
    }
}
