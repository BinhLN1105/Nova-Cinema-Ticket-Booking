package com.cinema.ticket_booking.data.model.request

import com.google.gson.annotations.SerializedName

data class BookingRequest(
    @SerializedName("showtimeId")
    val showtimeId: String?,

    @SerializedName("showtimeSeatIds")
    val showtimeSeatIds: List<String>?,

    @SerializedName("combos")
    val combos: List<ComboItem>?,

    @SerializedName("voucherCode")
    val voucherCode: String?
) {
    data class ComboItem(
        @SerializedName("comboId")
        val comboId: String?,

        @SerializedName("quantity")
        val quantity: Int
    )
}
