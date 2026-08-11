package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class StaffDashboardStatsResponse(
    @SerializedName("totalShowtimesToday")
    @JvmField val totalShowtimesToday: Long,

    @SerializedName("ticketsCheckedToday")
    @JvmField val ticketsCheckedToday: Long,

    @SerializedName("ticketsCheckedThisMonth")
    @JvmField val ticketsCheckedThisMonth: Long
)
