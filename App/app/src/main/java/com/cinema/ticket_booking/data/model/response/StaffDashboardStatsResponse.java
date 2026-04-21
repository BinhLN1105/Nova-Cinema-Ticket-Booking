package com.cinema.ticket_booking.data.model.response;

import com.google.gson.annotations.SerializedName;

public class StaffDashboardStatsResponse {

    @SerializedName("totalShowtimesToday")
    public long totalShowtimesToday;

    @SerializedName("ticketsCheckedToday")
    public long ticketsCheckedToday;

    @SerializedName("ticketsCheckedThisMonth")
    public long ticketsCheckedThisMonth;
}
