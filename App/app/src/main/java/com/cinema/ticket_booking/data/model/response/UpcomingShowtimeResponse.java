package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class UpcomingShowtimeResponse {

    @SerializedName("showtimeId")
    public String showtimeId;

    @SerializedName("movieTitle")
    public String movieTitle;

    @SerializedName("moviePosterUrl")
    public String moviePosterUrl;

    @SerializedName("screenName")
    public String screenName;

    @SerializedName("startTime")
    public String startTime; // ISO String, parse ở UI layer

    @SerializedName("minutesUntilStart")
    public long minutesUntilStart;

    @SerializedName("urgency")
    public String urgency; // "SOON" | "UPCOMING"
}
