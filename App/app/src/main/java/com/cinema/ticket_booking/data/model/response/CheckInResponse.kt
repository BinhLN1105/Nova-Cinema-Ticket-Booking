package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class CheckInResponse(
    @SerializedName("bookingCode")
    val bookingCode: String?,

    @SerializedName("movieTitle")
    val movieTitle: String?,

    @SerializedName("startTime")
    val startTime: String?,

    @SerializedName("cinemaName")
    val cinemaName: String?,

    @SerializedName("screenName")
    val screenName: String?,

    @SerializedName("seats")
    val seats: List<SeatItem>?,

    @SerializedName("allCheckedIn")
    val allCheckedIn: Boolean?,

    @SerializedName("checkedInAt")
    val checkedInAt: String?,

    @SerializedName("customerName")
    val customerName: String?,

    @SerializedName("customerEmail")
    val customerEmail: String?,

    @SerializedName("customerPhone")
    val customerPhone: String?
) {
    data class SeatItem(
        @SerializedName("rowLabel")
        val rowLabel: String?,

        @SerializedName("colNumber")
        val colNumber: Int?,

        @SerializedName("seatType")
        val seatType: String?,

        @SerializedName("isUsed")
        @get:JvmName("getIsUsed")
        val isUsed: Boolean?
    )
}
