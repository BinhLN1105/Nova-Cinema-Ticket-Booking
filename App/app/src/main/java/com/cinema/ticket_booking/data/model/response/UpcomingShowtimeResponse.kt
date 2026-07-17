package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class UpcomingShowtimeResponse(
    @SerializedName("showtimeId")
    @JvmField val showtimeId: String?,

    @SerializedName("movieTitle")
    @JvmField val movieTitle: String?,

    @SerializedName("moviePosterUrl")
    @JvmField val moviePosterUrl: String?,

    @SerializedName("screenName")
    @JvmField val screenName: String?,

    @SerializedName("startTime")
    @JvmField val startTime: String?,

    @SerializedName("minutesUntilStart")
    @JvmField val minutesUntilStart: Long,

    @SerializedName("urgency")
    @JvmField val urgency: String?
)
