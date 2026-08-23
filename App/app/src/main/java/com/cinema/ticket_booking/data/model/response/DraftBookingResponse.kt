package com.cinema.ticket_booking.data.model.response

import com.google.gson.annotations.SerializedName

data class DraftBookingResponse(
    @SerializedName("userId")
    val userId: String?,

    @SerializedName("showtimeId")
    val showtimeId: String?,

    @SerializedName("showtimeSeatIds")
    val showtimeSeatIds: List<String>?,

    @SerializedName("combos")
    val combos: List<DraftComboItem>?
)

data class DraftComboItem(
    @SerializedName("comboId")
    val comboId: String?,

    @SerializedName("quantity")
    val quantity: Int = 0
)
