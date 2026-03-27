package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class PromotionResponse {
    @SerializedName("id")
    private String id;

    @SerializedName("title")
    private String title;

    @SerializedName("description")
    private String description;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("targetUrl")
    private String targetUrl;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getTargetUrl() {
        return targetUrl;
    }
}
