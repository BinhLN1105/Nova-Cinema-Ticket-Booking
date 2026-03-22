package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class GiftCardResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("code")
    private String code;
    @SerializedName("amount")
    private double amount;
    @SerializedName("status")
    private String status;
    @SerializedName("createdAt")
    private String createdAt;
    @SerializedName("redeemedAt")
    private String redeemedAt;

    public String getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public double getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public String getRedeemedAt() {
        return redeemedAt;
    }
}
