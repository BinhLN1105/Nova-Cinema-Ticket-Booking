package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class NotificationResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("title")
    private String title;
    @SerializedName("body")
    private String body;
    @SerializedName("type")
    private String type;
    @SerializedName("isRead")
    private boolean isRead;
    @SerializedName("sentAt")
    private String sentAt;

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public boolean isRead() {
        return isRead;
    }

    public String getSentAt() {
        return sentAt;
    }
}
