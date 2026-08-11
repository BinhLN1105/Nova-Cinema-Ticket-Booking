package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class BookingSummary @JvmOverloads constructor(
    @SerializedName("id")
    var id: String? = null,

    @SerializedName("bookingCode")
    var bookingCode: String? = null,

    @SerializedName("movieTitle")
    var movieTitle: String? = null,

    @SerializedName("moviePosterUrl")
    var moviePosterUrl: String? = null,

    @SerializedName("startTime")
    var startTime: String? = null,

    @SerializedName("cinemaName")
    var cinemaName: String? = null,

    @SerializedName("screenName")
    var screenName: String? = null,

    @SerializedName("screenType")
    var screenType: String? = null,

    @SerializedName("seats")
    var seats: String? = null,

    @SerializedName("totalAmount")
    var totalAmount: Double = 0.0,

    @SerializedName("status")
    var status: String? = null,

    @SerializedName("createdAt")
    var createdAt: String? = null,

    @SerializedName("expiresAt")
    var expiresAt: String? = null,

    @SerializedName("movieId")
    var movieId: String? = null
)
