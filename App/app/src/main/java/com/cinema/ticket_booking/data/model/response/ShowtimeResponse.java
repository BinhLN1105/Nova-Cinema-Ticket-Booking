package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class ShowtimeResponse {
    @SerializedName("id")
    private String id;
    @SerializedName("startTime")
    private String startTime;
    @SerializedName("endTime")
    private String endTime;
    @SerializedName("basePrice")
    private double basePrice;
    @SerializedName("status")
    private String status;
    @SerializedName("movieTitle")
    private String movieTitle;
    @SerializedName("screenName")
    private String screenName;
    @SerializedName("screenType")
    private String screenType;
    @SerializedName("cinemaName")
    private String cinemaName;
    @SerializedName("cinemaAddress")
    private String cinemaAddress;
    @SerializedName("availableSeats")
    private long availableSeats;
    @SerializedName("movieId")
    private String movieId;
    @SerializedName("moviePosterUrl")
    private String moviePosterUrl;

    public String getMovieId() {
        return movieId;
    }

    public String getMoviePosterUrl() {
        return moviePosterUrl;
    }

    public String getId() {
        return id;
    }

    public String getStartTime() {
        return startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public double getBasePrice() {
        return basePrice;
    }

    public String getStatus() {
        return status;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public String getScreenName() {
        return screenName;
    }

    public String getScreenType() {
        return screenType;
    }

    public String getCinemaName() {
        return cinemaName;
    }

    public long getAvailableSeats() {
        return availableSeats;
    }
}
