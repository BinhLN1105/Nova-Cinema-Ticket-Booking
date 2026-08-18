package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class CheckInHistoryItemResponse(
    @SerializedName("bookingCode")
    @JvmField val bookingCode: String?,

    @SerializedName("customerName")
    @JvmField val customerName: String?,

    @SerializedName("customerPhone")
    @JvmField val customerPhone: String?,

    @SerializedName("movieTitle")
    @JvmField val movieTitle: String?,

    @SerializedName("moviePosterUrl")
    @JvmField val moviePosterUrl: String?,

    @SerializedName("screenName")
    @JvmField val screenName: String?,

    @SerializedName("cinemaName")
    @JvmField val cinemaName: String?,

    @SerializedName("seatsChecked")
    @JvmField val seatsChecked: String?,

    @SerializedName("success")
    @JvmField val success: Boolean,

    @SerializedName("failReason")
    @JvmField val failReason: String?,

    @SerializedName("scannedAt")
    @JvmField val scannedAt: String?
)
