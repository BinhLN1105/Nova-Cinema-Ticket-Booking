package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class ReviewResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("userFullName")
    private String userFullName;
    @SerializedName("userAvatarUrl")
    private String userAvatarUrl;
    @SerializedName("rating")
    private int rating;
    @SerializedName("comment")
    private String comment;
    @SerializedName("createdAt")
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getUserFullName() {
        return userFullName;
    }

    public String getUserAvatarUrl() {
        return userAvatarUrl;
    }

    public int getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
