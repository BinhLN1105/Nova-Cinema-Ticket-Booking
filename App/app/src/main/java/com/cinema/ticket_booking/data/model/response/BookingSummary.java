package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class BookingSummary {
    @SerializedName("id")
    private String id;
    @SerializedName("bookingCode")
    private String bookingCode;
    @SerializedName("movieTitle")
    private String movieTitle;
    @SerializedName("moviePosterUrl")
    private String moviePosterUrl;
    @SerializedName("startTime")
    private String startTime;
    @SerializedName("cinemaName")
    private String cinemaName;
    @SerializedName("screenName")
    private String screenName;
    @SerializedName("seats")
    private String seats;
    @SerializedName("totalAmount")
    private double totalAmount;
    @SerializedName("status")
    private String status;
    @SerializedName("createdAt")
    private String createdAt;

    public String getId() {
        return id;
    }

    public String getBookingCode() {
        return bookingCode;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public String getMoviePosterUrl() {
        return moviePosterUrl;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public double getTotalAmount() {
        return totalAmount;
    }

    public String getStatus() {
        return status;
    }

    public String getScreenName() {
        return screenName;
    }

    public String getSeats() {
        return seats;
    }
}
