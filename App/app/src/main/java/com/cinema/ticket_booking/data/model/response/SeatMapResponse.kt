package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class SeatMapResponse(
    @SerializedName("showtimeId")
    val showtimeId: String?,

    @SerializedName("totalRows")
    val totalRows: Int,

    @SerializedName("totalCols")
    val totalCols: Int,

    @SerializedName("maxGridRow")
    val maxGridRow: Int,

    @SerializedName("maxGridCol")
    val maxGridCol: Int,

    @SerializedName("seatHoldMins")
    val seatHoldMins: Int,

    @SerializedName("seats")
    val seats: List<SeatItem>?
) {
    data class SeatItem(
        @SerializedName("showtimeSeatId")
        val showtimeSeatId: String?,

        @SerializedName("seatId")
        val seatId: String?,

        @SerializedName("rowLabel")
        val rowLabel: String?,

        @SerializedName("colNumber")
        val colNumber: Int,

        @SerializedName("gridRow")
        val gridRow: Int,

        @SerializedName("gridCol")
        val gridCol: Int,

        @SerializedName("seatLabel")
        val seatLabel: String?,

        @SerializedName("seatType")
        val seatType: String?,

        @SerializedName("status")
        val status: String?,

        @SerializedName("price")
        val price: Double
    )
}
