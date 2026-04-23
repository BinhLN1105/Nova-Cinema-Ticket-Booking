package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class CheckInHistoryItemResponse {

    @SerializedName("bookingCode")
    public String bookingCode;

    @SerializedName("customerName")
    public String customerName;

    @SerializedName("customerPhone")
    public String customerPhone;

    @SerializedName("movieTitle")
    public String movieTitle;

    @SerializedName("moviePosterUrl")
    public String moviePosterUrl;

    @SerializedName("screenName")
    public String screenName;

    @SerializedName("cinemaName")
    public String cinemaName;

    @SerializedName("seatsChecked")
    public String seatsChecked;

    @SerializedName("success")
    public boolean success;

    @SerializedName("failReason")
    public String failReason;

    @SerializedName("scannedAt")
    public String scannedAt; // ISO String
}
